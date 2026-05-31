package org.chapzlock.core.component;

/**
 * Controls how a material handles transparency during rendering.
 *
 * <p>This enum follows the glTF 2.0 specification for alpha modes. Each mode represents
 * a different trade-off between visual quality and rendering performance:
 *
 * <ul>
 *   <li><strong>OPAQUE</strong> — fully solid, no transparency. The alpha channel of the
 *       base color is completely ignored. This is the most performant mode because the GPU
 *       can use early depth testing to skip hidden fragments entirely.</li>
 *   <li><strong>MASK</strong> — binary transparency: each pixel is either fully opaque or
 *       fully transparent based on whether its alpha value meets the
 *       {@link PbrMaterial#getAlphaCutoff() alphaCutoff} threshold. Used for leaves, fences,
 *       chain-link, hair cards — anything with hard-edged transparency. Still allows early
 *       depth testing for opaque pixels, so it performs better than BLEND.</li>
 *   <li><strong>BLEND</strong> — smooth alpha blending for semi-transparent surfaces like
 *       glass, particles, smoke, and water. Requires back-to-front depth sorting of
 *       transparent geometry, which is more expensive. Disables early depth testing for
 *       blended fragments.</li>
 * </ul>
 *
 * <p><strong>Performance tip:</strong> Prefer OPAQUE whenever possible. Use MASK for
 * hard-edged cutouts. Only use BLEND when you truly need smooth semi-transparency.
 *
 * @see PbrMaterial
 */
public enum AlphaMode {
    /** Fully opaque. The alpha channel is ignored and no blending occurs. */
    OPAQUE,

    /**
     * Binary transparency using alpha testing.
     *
     * <p>Pixels with alpha below {@link PbrMaterial#getAlphaCutoff()} are discarded
     * (not rendered). All other pixels are rendered as fully opaque. This creates
     * hard-edged transparency without the cost of depth sorting.
     */
    MASK,

    /**
     * Smooth alpha blending for semi-transparent surfaces.
     *
     * <p>The fragment's alpha value controls how much of the background shows through.
     * Requires sorting transparent geometry back-to-front for correct results, making
     * this the most expensive alpha mode.
     */
    BLEND
}
