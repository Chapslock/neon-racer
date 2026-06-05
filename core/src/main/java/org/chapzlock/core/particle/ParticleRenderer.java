package org.chapzlock.core.particle;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.render.RenderContext;
import org.chapzlock.core.render.ShaderRegistry;
import org.chapzlock.core.render.TextureRegistry;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/**
 * GPU-instanced renderer for particle systems using billboarded quads.
 *
 * <p><strong>What is instanced rendering?</strong> Instead of drawing each particle with a
 * separate draw call, instanced rendering draws all particles in one call. Each particle gets
 * its own "instance" of the same quad geometry, with per-instance attributes (position, size,
 * color) stored in a separate GPU buffer.
 *
 * <p><strong>What is billboarding?</strong> Particles always face the camera by computing their
 * orientation from the camera's right and up vectors. This creates the illusion of 3D particles
 * (sparks, smoke) while rendering simple 2D quads.
 *
 * <p>This renderer manages:
 * <ul>
 *   <li>A quad VAO (4 vertices forming a square from -0.5 to +0.5)</li>
 *   <li>A dynamic instance VBO updated each frame with particle data</li>
 *   <li>Particle shader (billboard vertex transform + textured fragment)</li>
 *   <li>Default soft-circle texture (64x64 radial gradient)</li>
 * </ul>
 *
 * @see ParticlePass
 * @see ParticleEmitter
 */
public class ParticleRenderer implements Disposable {

    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;
    private static final int FLOATS_PER_VERTEX = 5; // vec3 pos + vec2 uv
    private static final int BYTES_PER_INSTANCE = 32; // vec3 + float + vec4 = 3*4 + 4 + 4*4 = 32

    private final ShaderRegistry shaderRegistry;
    private final TextureRegistry textureRegistry;

    private Shader shader;
    private int vao;
    private int quadVbo;
    private int quadEbo;
    private int instanceVbo;
    private int instanceCapacity;
    private Texture defaultTexture;

    /**
     * Creates a particle renderer using the given registries for shader/texture management.
     *
     * @param shaderRegistry shader registry for compiling and setting uniforms
     * @param textureRegistry texture registry for binding textures to units
     */
    public ParticleRenderer(ShaderRegistry shaderRegistry, TextureRegistry textureRegistry) {
        this.shaderRegistry = shaderRegistry;
        this.textureRegistry = textureRegistry;
    }

    /**
     * Initializes GPU resources: quad geometry, instance buffer, shader, and default texture.
     * Call once after OpenGL context is ready.
     *
     * @param initialCapacity initial number of particles the instance buffer can hold
     */
    public void init(int initialCapacity) {
        this.instanceCapacity = initialCapacity;

        // Create quad geometry: positions (x, y, 0) + UVs (u, v)
        float[] quadVertices = {
            // x     y    z     u    v
            -0.5f, -0.5f, 0f,  0f,  0f,  // bottom-left
             0.5f, -0.5f, 0f,  1f,  0f,  // bottom-right
             0.5f,  0.5f, 0f,  1f,  1f,  // top-right
            -0.5f,  0.5f, 0f,  0f,  1f   // top-left
        };
        int[] quadIndices = { 0, 1, 2, 2, 3, 0 };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Upload quad vertices
        quadVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);

        // location 0: aPosition (vec3)
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * 4, 0);

        // location 1: aTexCoord (vec2)
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * 4, 3 * 4);

        // Upload quad indices
        quadEbo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, quadIndices, GL_STATIC_DRAW);

        // Create instance buffer (dynamic, resizable)
        instanceVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
        glBufferData(GL_ARRAY_BUFFER, (long) instanceCapacity * BYTES_PER_INSTANCE, GL_DYNAMIC_DRAW);

        // location 3: iPosition (vec3, per-instance)
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, BYTES_PER_INSTANCE, 0);
        glVertexAttribDivisor(3, 1);

        // location 4: iSize (float, per-instance)
        glEnableVertexAttribArray(4);
        glVertexAttribPointer(4, 1, GL_FLOAT, false, BYTES_PER_INSTANCE, 3 * 4);
        glVertexAttribDivisor(4, 1);

        // location 5: iColor (vec4, per-instance)
        glEnableVertexAttribArray(5);
        glVertexAttribPointer(5, 4, GL_FLOAT, false, BYTES_PER_INSTANCE, 4 * 4);
        glVertexAttribDivisor(5, 1);

        glBindVertexArray(0);

        // Compile shader
        shader = new Shader("shaders/ParticleVertex.glsl", "shaders/ParticleFragment.glsl");
        shaderRegistry.compileAndLink(shader);

        // Generate default texture (64x64 soft circle)
        defaultTexture = createDefaultTexture();
    }

    /**
     * Renders a batch of particles with the given texture.
     *
     * @param ctx render context containing camera matrices
     * @param particles particle array to render
     * @param count number of alive particles to render
     * @param texture texture to apply (use defaultTexture for soft circle)
     */
    public void render(RenderContext ctx, Particle[] particles, int count, Texture texture) {
        if (count <= 0) return;

        // Resize instance buffer if needed
        if (count > instanceCapacity) {
            instanceCapacity = Math.max(count, instanceCapacity * 2);
            glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
            glBufferData(GL_ARRAY_BUFFER, (long) instanceCapacity * BYTES_PER_INSTANCE, GL_DYNAMIC_DRAW);
        }

        // Fill instance buffer with particle data
        ByteBuffer buffer = MemoryUtil.memAlloc(count * BYTES_PER_INSTANCE);
        FloatBuffer floatBuffer = buffer.asFloatBuffer();

        for (int i = 0; i < count; i++) {
            Particle p = particles[i];
            Vector3f pos = p.getPosition();
            Vector4f col = p.getColor();

            floatBuffer.put(pos.x).put(pos.y).put(pos.z);  // iPosition
            floatBuffer.put(p.getSize());                   // iSize
            floatBuffer.put(col.x).put(col.y).put(col.z).put(col.w); // iColor
        }
        floatBuffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        MemoryUtil.memFree(buffer);

        // Bind shader and set uniforms
        shaderRegistry.use(shader);
        shaderRegistry.setUniform(shader, "uViewMatrix", ctx.getViewMatrix());
        shaderRegistry.setUniform(shader, "uProjectionMatrix", ctx.getProjectionMatrix());
        textureRegistry.bind(texture, 0);
        shaderRegistry.setUniform(shader, "uTexture", 0);

        // Draw instanced
        glBindVertexArray(vao);
        glDrawElementsInstanced(GL_TRIANGLES, INDICES_PER_QUAD, GL_UNSIGNED_INT, 0, count);
        glBindVertexArray(0);

        textureRegistry.unbind(texture);
        shaderRegistry.clearProgram();
    }

    /**
     * Returns the default soft-circle texture for particles without a custom texture.
     */
    public Texture getDefaultTexture() {
        return defaultTexture;
    }

    /**
     * Creates a procedural 64x64 white radial gradient texture.
     */
    private Texture createDefaultTexture() {
        int size = 64;
        ByteBuffer buffer = MemoryUtil.memAlloc(size * size * 4);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float nx = (x / (float) size) * 2f - 1f;
                float ny = (y / (float) size) * 2f - 1f;
                float dist = (float) Math.sqrt(nx * nx + ny * ny);
                float alpha = Math.max(0f, 1f - dist);
                alpha = alpha * alpha; // soften edges

                buffer.put((byte) 255);  // R
                buffer.put((byte) 255);  // G
                buffer.put((byte) 255);  // B
                buffer.put((byte) (alpha * 255));  // A
            }
        }
        buffer.flip();

        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);

        MemoryUtil.memFree(buffer);

        return Texture.builder()
            .textureHandle(texId)
            .width(size)
            .height(size)
            .filePath("__default_particle__")
            .build();
    }

    @Override
    public void dispose() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            vao = 0;
        }
        if (quadVbo != 0) {
            glDeleteBuffers(quadVbo);
            quadVbo = 0;
        }
        if (quadEbo != 0) {
            glDeleteBuffers(quadEbo);
            quadEbo = 0;
        }
        if (instanceVbo != 0) {
            glDeleteBuffers(instanceVbo);
            instanceVbo = 0;
        }
        if (shader != null && shader.isCompiled()) {
            shaderRegistry.delete(shader);
            shader = null;
        }
        if (defaultTexture != null) {
            glDeleteTextures(defaultTexture.getTextureHandle());
            defaultTexture = null;
        }
    }
}
