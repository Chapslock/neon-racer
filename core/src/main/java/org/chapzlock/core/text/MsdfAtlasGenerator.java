package org.chapzlock.core.text;

import lombok.experimental.UtilityClass;
import org.chapzlock.core.logging.Log;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.msdfgen.MSDFGenBitmap;
import org.lwjgl.util.msdfgen.MSDFGenBounds;
import org.lwjgl.util.msdfgen.MSDFGenTransform;

import org.lwjgl.PointerBuffer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.stb.STBRectPack.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.msdfgen.MSDFGen.*;
import static org.lwjgl.util.msdfgen.MSDFGenExt.*;

/**
 * Generates MSDF font atlases from TrueType font data.
 *
 * <p>This utility combines three LWJGL libraries to produce a {@link RawFontAtlas}:
 * <ul>
 *   <li><b>STB TrueType</b> — font metrics (advance, bearing, ascent, descent)</li>
 *   <li><b>MSDF-gen</b> — multi-channel signed distance field bitmap generation</li>
 *   <li><b>STB Rect Pack</b> — bin-packing glyph bitmaps into a single atlas texture</li>
 * </ul>
 *
 * <p>The MSDF technique stores shape distance in RGB channels, enabling crisp text rendering
 * at any scale without resampling. Distance range controls the smoothness vs. sharpness tradeoff.
 *
 * @see RawFontAtlas
 */
@UtilityClass
public class MsdfAtlasGenerator {
    private static final Log log = Log.create(MsdfAtlasGenerator.class);

    private static final int GLYPH_SIZE = 32; // pixel size for each glyph's MSDF bitmap
    private static final float DISTANCE_RANGE = 4.0f; // SDF distance range (pixels)
    private static final int PADDING = 2; // padding between glyphs in atlas (pixels)
    private static final int FIRST_CHAR = 32; // space
    private static final int LAST_CHAR = 255; // ÿ
    private static final int INITIAL_ATLAS_SIZE = 512; // start with 512x512, double if needed

    /**
     * Generate an MSDF atlas from TrueType font bytes.
     *
     * @param ttfData the font file bytes (must remain valid for the duration of this call)
     * @param pixelHeight the target pixel height for the font (used for scaling metrics)
     * @return a complete {@link RawFontAtlas} with pixel data, glyphs, and kerning
     * @throws RuntimeException if font loading or atlas generation fails
     */
    public RawFontAtlas generate(ByteBuffer ttfData, float pixelHeight) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Initialize STB TrueType for metrics
            STBTTFontinfo fontInfo = STBTTFontinfo.calloc(stack);
            if (!stbtt_InitFont(fontInfo, ttfData)) {
                throw new RuntimeException("Failed to initialize STB TrueType font");
            }

            // Initialize MSDF-gen FreeType
            long ftHandle = initMsdfFreeType(stack);
            long ftFont = loadMsdfFont(ftHandle, ttfData, stack);

            try {
                // Generate MSDF bitmaps for all glyphs
                List<GlyphBitmap> glyphBitmaps = generateGlyphBitmaps(ftFont, pixelHeight, stack);

                // Pack glyphs into atlas
                PackResult packResult = packGlyphs(glyphBitmaps);

                // Extract metrics from STB TrueType
                float scale = stbtt_ScaleForPixelHeight(fontInfo, pixelHeight);
                FontMetrics fontMetrics = extractFontMetrics(fontInfo, scale, stack);

                // Build glyph map with UVs and metrics
                Map<Character, Glyph> glyphs = buildGlyphMap(
                    glyphBitmaps,
                    packResult,
                    fontInfo,
                    scale,
                    fontMetrics
                );

                // Extract kerning pairs
                Map<Long, Float> kerning = extractKerning(ftFont, scale, stack);

                return RawFontAtlas.builder()
                    .pixelData(packResult.atlasData)
                    .atlasWidth(packResult.atlasSize)
                    .atlasHeight(packResult.atlasSize)
                    .glyphs(glyphs)
                    .kerning(kerning)
                    .lineHeight(fontMetrics.lineHeight)
                    .ascent(fontMetrics.ascent)
                    .descent(fontMetrics.descent)
                    .distanceRange(DISTANCE_RANGE)
                    .build();
            } finally {
                // Cleanup MSDF resources
                msdf_ft_font_destroy(ftFont);
                msdf_ft_deinit(ftHandle);
            }
        }
    }

    /**
     * Initialize MSDF-gen's FreeType handle.
     */
    private long initMsdfFreeType(MemoryStack stack) {
        PointerBuffer handleBuf = stack.mallocPointer(1);
        handleBuf.put(0, 0L);

        int result = msdf_ft_init(handleBuf);

        if (result != 0) {
            throw new RuntimeException("Failed to initialize MSDF FreeType: " + result);
        }

        return handleBuf.get(0);
    }

    /**
     * Load font into MSDF-gen's FreeType.
     */
    private long loadMsdfFont(long ftHandle, ByteBuffer ttfData, MemoryStack stack) {
        PointerBuffer fontBuf = stack.mallocPointer(1);
        fontBuf.put(0, 0L);

        int result = msdf_ft_load_font_data(ftHandle, ttfData, fontBuf);

        if (result != 0) {
            msdf_ft_deinit(ftHandle);
            throw new RuntimeException("Failed to load MSDF font data: " + result);
        }

        return fontBuf.get(0);
    }

    /**
     * Generate MSDF bitmaps for all printable ASCII characters.
     */
    private List<GlyphBitmap> generateGlyphBitmaps(long ftFont, float pixelHeight, MemoryStack stack) {
        List<GlyphBitmap> bitmaps = new ArrayList<>();

        for (int cp = FIRST_CHAR; cp <= LAST_CHAR; cp++) {
            GlyphBitmap bitmap = generateGlyphBitmap(ftFont, cp, stack);
            if (bitmap != null) {
                bitmaps.add(bitmap);
            }
        }

        log.info("Generated %d glyph bitmaps", bitmaps.size());
        return bitmaps;
    }

    /**
     * Generate a single MSDF bitmap for a codepoint.
     *
     * @return the bitmap, or null if the glyph has no visible shape (e.g., space)
     */
    private GlyphBitmap generateGlyphBitmap(long ftFont, int codepoint, MemoryStack stack) {
        // Load glyph shape (em-normalized coordinates)
        PointerBuffer shapeBuf = stack.mallocPointer(1);
        shapeBuf.put(0, 0L);

        int result = msdf_ft_font_load_glyph(
            ftFont,
            codepoint,
            MSDF_FONT_SCALING_EM_NORMALIZED,
            shapeBuf
        );

        if (result != 0) {
            log.warn("Failed to load glyph shape for codepoint %d: %d", codepoint, result);
            return null;
        }

        long shape = shapeBuf.get(0);
        if (shape == NULL) {
            return null; // no visible shape (e.g., space)
        }

        try {
            // Normalize shape
            if (msdf_shape_normalize(shape) != 0) {
                log.warn("Failed to normalize shape for codepoint %d", codepoint);
                return null;
            }

            // Apply edge coloring
            if (msdf_shape_edge_colors_simple(shape, 3.0) != 0) {
                log.warn("Failed to color edges for codepoint %d", codepoint);
                return null;
            }

            // Get shape bounds
            MSDFGenBounds bounds = MSDFGenBounds.calloc(stack);
            if (msdf_shape_get_bounds(shape, bounds) != 0) {
                log.warn("Failed to get bounds for codepoint %d", codepoint);
                return null;
            }

            double shapeWidth = bounds.r() - bounds.l();
            double shapeHeight = bounds.t() - bounds.b();

            // Skip glyphs with no dimensions (e.g., space)
            if (shapeWidth <= 0.0 || shapeHeight <= 0.0) {
                return null;
            }

            // Allocate MSDF bitmap
            MSDFGenBitmap bitmap = MSDFGenBitmap.calloc(stack);
            if (msdf_bitmap_alloc(MSDF_BITMAP_TYPE_MSDF, GLYPH_SIZE, GLYPH_SIZE, bitmap) != 0) {
                log.warn("Failed to allocate bitmap for codepoint %d", codepoint);
                return null;
            }

            // Compute transform: fit shape into GLYPH_SIZE with padding
            double pxRange = DISTANCE_RANGE;
            double padding = pxRange / GLYPH_SIZE * Math.max(shapeWidth, shapeHeight);

            double scale = Math.min(
                (GLYPH_SIZE - 2 * pxRange) / shapeWidth,
                (GLYPH_SIZE - 2 * pxRange) / shapeHeight
            );

            double translateX = -bounds.l() + padding;
            double translateY = -bounds.b() + padding;

            MSDFGenTransform transform = MSDFGenTransform.calloc(stack);
            transform.scale(v -> v.x(scale).y(scale));
            transform.translation(v -> v.x(translateX).y(translateY));
            transform.distance_mapping(r -> r.lower(pxRange).upper(-pxRange));

            // Generate MSDF
            if (msdf_generate_msdf(bitmap, shape, transform) != 0) {
                msdf_bitmap_free(bitmap);
                log.warn("Failed to generate MSDF for codepoint %d", codepoint);
                return null;
            }

            // Extract pixel data (3-channel float)
            PointerBuffer pixelsPtrBuf = stack.mallocPointer(1);
            pixelsPtrBuf.put(0, 0L);

            if (msdf_bitmap_get_pixels(bitmap, pixelsPtrBuf) != 0) {
                msdf_bitmap_free(bitmap);
                log.warn("Failed to get pixels for codepoint %d", codepoint);
                return null;
            }

            long pixelsAddr = pixelsPtrBuf.get(0);
            if (pixelsAddr == NULL) {
                msdf_bitmap_free(bitmap);
                return null;
            }

            // Convert float pixels to RGB bytes
            FloatBuffer floatPixels = memFloatBuffer(pixelsAddr, GLYPH_SIZE * GLYPH_SIZE * 3);
            ByteBuffer rgbData = memAlloc(GLYPH_SIZE * GLYPH_SIZE * 3);

            for (int i = 0; i < GLYPH_SIZE * GLYPH_SIZE * 3; i++) {
                float value = floatPixels.get(i);
                int byteValue = Math.max(0, Math.min(255, (int) (value * 255.0f)));
                rgbData.put((byte) byteValue);
            }
            rgbData.flip();

            msdf_bitmap_free(bitmap);

            return new GlyphBitmap((char) codepoint, rgbData, GLYPH_SIZE, GLYPH_SIZE);
        } finally {
            msdf_shape_free(shape);
        }
    }

    /**
     * Pack glyph bitmaps into a single atlas texture.
     */
    private PackResult packGlyphs(List<GlyphBitmap> glyphBitmaps) {
        int atlasSize = INITIAL_ATLAS_SIZE;
        PackResult result = null;

        // Try packing with increasing atlas sizes until successful
        while (result == null && atlasSize <= 4096) {
            result = tryPackGlyphs(glyphBitmaps, atlasSize);
            if (result == null) {
                atlasSize *= 2;
            }
        }

        if (result == null) {
            throw new RuntimeException("Failed to pack glyphs into atlas (tried up to 4096x4096)");
        }

        log.info("Packed %d glyphs into %dx%d atlas", glyphBitmaps.size(), atlasSize, atlasSize);
        return result;
    }

    /**
     * Attempt to pack glyphs into an atlas of the given size.
     *
     * @return the packed atlas, or null if packing failed
     */
    private PackResult tryPackGlyphs(List<GlyphBitmap> glyphBitmaps, int atlasSize) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Initialize rect packer
            STBRPContext context = STBRPContext.calloc(stack);
            STBRPNode.Buffer nodes = STBRPNode.calloc(atlasSize, stack);
            stbrp_init_target(context, atlasSize, atlasSize, nodes);

            // Create rect buffer
            STBRPRect.Buffer rects = STBRPRect.calloc(glyphBitmaps.size(), stack);
            for (int i = 0; i < glyphBitmaps.size(); i++) {
                GlyphBitmap gb = glyphBitmaps.get(i);
                rects.get(i)
                    .w((short) (gb.width + PADDING))
                    .h((short) (gb.height + PADDING))
                    .id(i);
            }

            // Try packing
            stbrp_pack_rects(context, rects);

            // Check if all rects were packed
            for (int i = 0; i < glyphBitmaps.size(); i++) {
                if (!rects.get(i).was_packed()) {
                    return null; // packing failed
                }
            }

            // Copy glyph bitmaps into atlas (RGB format)
            ByteBuffer atlasData = memAlloc(atlasSize * atlasSize * 3);
            memSet(atlasData, 0);

            Map<Character, PackedGlyph> packedGlyphs = new HashMap<>();

            for (int i = 0; i < glyphBitmaps.size(); i++) {
                GlyphBitmap gb = glyphBitmaps.get(i);
                STBRPRect rect = rects.get(i);

                int x = rect.x();
                int y = rect.y();

                // Copy glyph pixels into atlas (flip vertically — msdfgen stores bottom-to-top)
                ByteBuffer glyphPixels = gb.pixelData;

                for (int row = 0; row < gb.height; row++) {
                    int srcRow = gb.height - 1 - row;
                    for (int col = 0; col < gb.width; col++) {
                        int srcIndex = (srcRow * gb.width + col) * 3;
                        int atlasIndex = ((y + row) * atlasSize + (x + col)) * 3;

                        atlasData.put(atlasIndex, glyphPixels.get(srcIndex));
                        atlasData.put(atlasIndex + 1, glyphPixels.get(srcIndex + 1));
                        atlasData.put(atlasIndex + 2, glyphPixels.get(srcIndex + 2));
                    }
                }

                // Store packed position
                packedGlyphs.put(gb.codepoint, new PackedGlyph(x, y, gb.width, gb.height));
            }

            atlasData.rewind();
            return new PackResult(atlasData, atlasSize, packedGlyphs);
        }
    }

    /**
     * Extract font-wide metrics from STB TrueType.
     */
    private FontMetrics extractFontMetrics(STBTTFontinfo fontInfo, float scale, MemoryStack stack) {
        IntBuffer ascentBuf = stack.mallocInt(1);
        IntBuffer descentBuf = stack.mallocInt(1);
        IntBuffer lineGapBuf = stack.mallocInt(1);

        stbtt_GetFontVMetrics(fontInfo, ascentBuf, descentBuf, lineGapBuf);

        float ascent = ascentBuf.get(0) * scale;
        float descent = descentBuf.get(0) * scale;
        float lineGap = lineGapBuf.get(0) * scale;
        float lineHeight = ascent - descent + lineGap;

        return new FontMetrics(lineHeight, ascent, descent);
    }

    /**
     * Build the final glyph map with metrics and UV coordinates.
     */
    private Map<Character, Glyph> buildGlyphMap(
        List<GlyphBitmap> glyphBitmaps,
        PackResult packResult,
        STBTTFontinfo fontInfo,
        float scale,
        FontMetrics fontMetrics
    ) {
        Map<Character, Glyph> glyphs = new HashMap<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceBuf = stack.mallocInt(1);
            IntBuffer lsbBuf = stack.mallocInt(1);

            for (GlyphBitmap gb : glyphBitmaps) {
                PackedGlyph packed = packResult.packedGlyphs.get(gb.codepoint);
                if (packed == null) {
                    continue; // shouldn't happen
                }

                // Get horizontal metrics
                stbtt_GetCodepointHMetrics(fontInfo, gb.codepoint, advanceBuf, lsbBuf);
                float advance = advanceBuf.get(0) * scale;
                float lsb = lsbBuf.get(0) * scale;

                // Get bounding box for bearing calculation
                IntBuffer x0 = stack.mallocInt(1);
                IntBuffer y0 = stack.mallocInt(1);
                IntBuffer x1 = stack.mallocInt(1);
                IntBuffer y1 = stack.mallocInt(1);
                stbtt_GetCodepointBitmapBox(fontInfo, gb.codepoint, scale, scale, x0, y0, x1, y1);

                float xBearing = lsb;
                float yBearing = -y0.get(0); // distance from baseline to top
                float width = (x1.get(0) - x0.get(0));
                float height = (y1.get(0) - y0.get(0));

                // Compute UV coordinates
                float u0 = (float) packed.x / packResult.atlasSize;
                float v0 = (float) packed.y / packResult.atlasSize;
                float u1 = (float) (packed.x + packed.width) / packResult.atlasSize;
                float v1 = (float) (packed.y + packed.height) / packResult.atlasSize;

                // Store metrics in pixel units at reference size (pixelHeight=32)
                // TextLayoutUtil applies fontSize/ascent scaling at layout time
                glyphs.put(gb.codepoint, new Glyph(
                    advance,
                    xBearing,
                    yBearing,
                    width,
                    height,
                    u0,
                    v0,
                    u1,
                    v1
                ));
            }
        }

        // Add space character with metrics but no geometry
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceBuf = stack.mallocInt(1);
            IntBuffer lsbBuf = stack.mallocInt(1);
            stbtt_GetCodepointHMetrics(fontInfo, ' ', advanceBuf, lsbBuf);

            float advance = advanceBuf.get(0) * scale;

            glyphs.put(' ', new Glyph(
                advance,
                0, 0, 0, 0, // no bearing/size for space
                0, 0, 0, 0  // no UVs
            ));
        }

        return glyphs;
    }

    /**
     * Extract kerning pairs from MSDF-gen FreeType.
     */
    private Map<Long, Float> extractKerning(long ftFont, float scale, MemoryStack stack) {
        Map<Long, Float> kerning = new HashMap<>();

        DoubleBuffer kerningBuf = stack.mallocDouble(1);

        // Sample common kerning pairs (full matrix would be 256*256 = 65k pairs)
        char[] commonChars = {
            'A', 'V', 'W', 'Y', 'T', 'P', 'F',
            'a', 'v', 'w', 'y', 't', 'p', 'f',
            '.', ',', '-', '\'', '"'
        };

        for (char left : commonChars) {
            for (char right : commonChars) {
                int result = msdf_ft_font_get_kerning(ftFont, left, right, kerningBuf);
                if (result == 0) {
                    double kerningValue = kerningBuf.get(0);
                    if (Math.abs(kerningValue) > 0.0001) {
                        // Kerning is in em-normalized units, scale to pixels then normalize
                        float kerningPixels = (float) kerningValue * scale;
                        kerning.put(RawFontAtlas.kerningKey(left, right), kerningPixels);
                    }
                }
            }
        }

        log.info("Extracted %d kerning pairs", kerning.size());
        return kerning;
    }

    // ===== Data Structures =====

    /**
     * A generated MSDF bitmap for a single glyph.
     */
    private record GlyphBitmap(
        char codepoint,
        ByteBuffer pixelData, // RGB, must be freed
        int width,
        int height
    ) {}

    /**
     * A glyph's position in the packed atlas.
     */
    private record PackedGlyph(
        int x,
        int y,
        int width,
        int height
    ) {}

    /**
     * The result of packing glyphs into an atlas.
     */
    private record PackResult(
        ByteBuffer atlasData, // RGB, must be freed
        int atlasSize,
        Map<Character, PackedGlyph> packedGlyphs
    ) {}

    /**
     * Font-wide vertical metrics.
     */
    private record FontMetrics(
        float lineHeight,
        float ascent,
        float descent
    ) {}
}
