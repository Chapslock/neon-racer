package org.chapzlock.core.physics;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.files.FileUtils;
import org.chapzlock.core.graphics.ShaderUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

/**
 * Specialized renderer for drawing physics simulation details.
 * Only used by the Physics system for debugging purposes.
 * NB! Should not be used in production builds.
 */
public class PhysicsDebugRenderer {

    private static final List<Line> lines = new ArrayList<>();

    private static int vao, vbo;
    private static int shaderProgram;
    private static boolean initialized = false;

    public static void addLineToDrawQueue(Vector3f from, Vector3f to, Vector3f color) {
        lines.add(new Line(new Vector3f(from), new Vector3f(to), new Vector3f(color)));
    }

    public static void drawLines(Matrix4f projection, Matrix4f view) {
        if (lines.isEmpty()) {
            return;
        }
        if (!initialized) {
            init();
        }

        glDisable(GL_DEPTH_TEST);
        glUseProgram(shaderProgram);
        int projLoc = glGetUniformLocation(shaderProgram, "uProjection");
        int viewLoc = glGetUniformLocation(shaderProgram, "uView");

        FloatBuffer projBuf = MemoryUtil.memAllocFloat(16);
        FloatBuffer viewBuf = MemoryUtil.memAllocFloat(16);
        projection.get(projBuf);
        view.get(viewBuf);

        glUniformMatrix4fv(projLoc, false, projBuf);
        glUniformMatrix4fv(viewLoc, false, viewBuf);

        MemoryUtil.memFree(projBuf);
        MemoryUtil.memFree(viewBuf);

        // Upload vertex data
        FloatBuffer buffer = MemoryUtil.memAllocFloat(lines.size() * 12);
        for (Line l : lines) {
            buffer.put(l.from.x).put(l.from.y).put(l.from.z)
                .put(l.color.x).put(l.color.y).put(l.color.z);
            buffer.put(l.to.x).put(l.to.y).put(l.to.z)
                .put(l.color.x).put(l.color.y).put(l.color.z);
        }
        buffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        MemoryUtil.memFree(buffer);

        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, lines.size() * 2);

        glBindVertexArray(0);
        glEnable(GL_DEPTH_TEST);
        lines.clear();
    }

    public static void init() {
        if (initialized) {
            return;
        }

        // Simple passthrough vertex + fragment shaders
        String vertexSrc = FileUtils.loadAsString("shaders/SimpleVertex.glsl");
        String fragmentSrc = FileUtils.loadAsString("shaders/SimpleFragment.glsl");
        shaderProgram = ShaderUtil.linkShaderProgram(
            ShaderUtil.compileShader(vertexSrc, GL_VERTEX_SHADER),
            ShaderUtil.compileShader(fragmentSrc, GL_FRAGMENT_SHADER)
        );

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 4096 * Float.BYTES, GL_DYNAMIC_DRAW);

        // position (3 floats)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        // color (3 floats)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        initialized = true;
    }

    public record Line(Vector3f from, Vector3f to, Vector3f color) {
    }
}
