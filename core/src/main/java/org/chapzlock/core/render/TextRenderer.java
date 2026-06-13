package org.chapzlock.core.render;

import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.text.*;
import org.chapzlock.core.window.WindowSpecification;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TextRenderer {

    private static final int FLOATS_PER_VERTEX = 8; // vec2 pos + vec2 uv + vec4 color
    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;

    private final ShaderRegistry shaderRegistry;
    private final Matrix4f projection = new Matrix4f();

    private Shader shader;
    private final Map<Integer, CachedTextMesh> meshCache = new HashMap<>();

    public TextRenderer(ShaderRegistry shaderRegistry, WindowSpecification windowSpec) {
        this.shaderRegistry = shaderRegistry;
        this.projection.ortho2D(0f, windowSpec.getWidth(), windowSpec.getHeight(), 0f);
    }

    public void init() {
        this.shader = new Shader("shaders/TextVertex.glsl", "shaders/TextFragment.glsl");
        shaderRegistry.compileAndLink(shader);
    }

    public void resize(int width, int height) {
        projection.identity().ortho2D(0f, width, height, 0f);
    }

    public void beginBatch(Font font) {
        shaderRegistry.use(shader);

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shaderRegistry.setUniform(shader, "uProjection", projection);
        shaderRegistry.setUniform(shader, "uAtlas", 0);
        shaderRegistry.setUniform(shader, "uDistanceRange", font.getDistanceRange());

        TextureUtil.bind(font.getAtlasTexture(), 0);
    }

    public void renderEntity(int entityId, TextComponent textComponent, Transform transform, Font font) {
        CachedTextMesh cached = meshCache.get(entityId);

        if (cached == null || cached.source != textComponent) {
            if (cached != null) {
                cached.dispose();
            }
            cached = buildMesh(textComponent, font);
            meshCache.put(entityId, cached);
        }

        // Set per-entity model matrix (translate by entity position)
        Matrix4f model = new Matrix4f().translate(
                transform.getX(),
                transform.getY(),
                transform.getZ());
        shaderRegistry.setUniform(shader, "uModel", model);

        // Set outline uniforms
        shaderRegistry.setUniform(shader, "uOutlineWidth", textComponent.getOutlineWidth());
        if (textComponent.getOutlineColor() != null) {
            shaderRegistry.setUniform(shader, "uOutlineColor", textComponent.getOutlineColor());
        } else {
            shaderRegistry.setUniform(shader, "uOutlineColor", new Vector4f(0, 0, 0, 0));
        }

        // Draw
        glBindVertexArray(cached.vao);
        glDrawElements(GL_TRIANGLES, cached.indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void endBatch() {
        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    public TextBounds getCachedBounds(int entityId) {
        CachedTextMesh cached = meshCache.get(entityId);
        return cached != null ? cached.bounds : null;
    }

    public void cleanupEntity(int entityId) {
        CachedTextMesh cached = meshCache.remove(entityId);
        if (cached != null) {
            cached.dispose();
        }
    }

    public void dispose() {
        for (CachedTextMesh cached : meshCache.values()) {
            cached.dispose();
        }
        meshCache.clear();
    }

    private CachedTextMesh buildMesh(TextComponent textComponent, Font font) {
        TextLayout layout = TextLayoutUtil.layout(
                textComponent.getText(),
                font,
                textComponent.getFontSize(),
                textComponent.getMaxWidth(),
                textComponent.getAlignment(),
                textComponent.getColor(),
                textComponent.getShadowColor(),
                textComponent.getShadowOffset());

        List<TextLayout.TextQuad> quads = layout.quads();
        int quadCount = quads.size();

        if (quadCount == 0) {
            return new CachedTextMesh(0, 0, 0, 0, textComponent, layout.bounds());
        }

        FloatBuffer vertices = MemoryUtil.memAllocFloat(quadCount * VERTICES_PER_QUAD * FLOATS_PER_VERTEX);
        IntBuffer indices = MemoryUtil.memAllocInt(quadCount * INDICES_PER_QUAD);

        for (int i = 0; i < quadCount; i++) {
            TextLayout.TextQuad q = quads.get(i);
            int baseVertex = i * VERTICES_PER_QUAD;

            // Top-left
            putVertex(vertices, q.x(), q.y(), q.u0(), q.v0(), q.r(), q.g(), q.b(), q.a());
            // Top-right
            putVertex(vertices, q.x() + q.width(), q.y(), q.u1(), q.v0(), q.r(), q.g(), q.b(), q.a());
            // Bottom-right
            putVertex(vertices, q.x() + q.width(), q.y() + q.height(), q.u1(), q.v1(), q.r(), q.g(), q.b(), q.a());
            // Bottom-left
            putVertex(vertices, q.x(), q.y() + q.height(), q.u0(), q.v1(), q.r(), q.g(), q.b(), q.a());

            // Two triangles: TL-TR-BR, TL-BR-BL
            indices.put(baseVertex).put(baseVertex + 1).put(baseVertex + 2);
            indices.put(baseVertex).put(baseVertex + 2).put(baseVertex + 3);
        }

        vertices.flip();
        indices.flip();

        // Upload to GPU
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        // position (vec2) at location 0
        glVertexAttribPointer(0, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        // uv (vec2) at location 1
        glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        // color (vec4) at location 2
        glVertexAttribPointer(2, 4, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 4L * Float.BYTES);
        glEnableVertexAttribArray(2);

        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);

        glBindVertexArray(0);

        MemoryUtil.memFree(vertices);
        MemoryUtil.memFree(indices);

        return new CachedTextMesh(vao, vbo, ebo, quadCount * INDICES_PER_QUAD, textComponent, layout.bounds());
    }

    private void putVertex(FloatBuffer buf, float x, float y, float u, float v,
                           float r, float g, float b, float a) {
        buf.put(x).put(y).put(u).put(v).put(r).put(g).put(b).put(a);
    }

    private static class CachedTextMesh {
        final int vao;
        final int vbo;
        final int ebo;
        final int indexCount;
        final TextComponent source;
        final TextBounds bounds;

        CachedTextMesh(int vao, int vbo, int ebo, int indexCount, TextComponent source, TextBounds bounds) {
            this.vao = vao;
            this.vbo = vbo;
            this.ebo = ebo;
            this.indexCount = indexCount;
            this.source = source;
            this.bounds = bounds;
        }

        void dispose() {
            if (vao != 0) {
                glDeleteVertexArrays(vao);
                glDeleteBuffers(vbo);
                glDeleteBuffers(ebo);
            }
        }
    }
}
