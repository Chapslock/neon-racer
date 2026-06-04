package org.chapzlock.core.physics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import org.chapzlock.core.application.Disposable;
import org.chapzlock.core.asset.FileUtils;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.render.ShaderUtil;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.IDebugDraw;

/**
 * Renders wireframe debug overlays for the physics simulation.
 *
 * <h3>What this shows</h3>
 * <ul>
 *   <li><strong>Wireframes</strong> — outlines of every collision shape in the world.
 *       Green = active dynamic body, red = sleeping, cyan = static.</li>
 *   <li><strong>Contact points</strong> — small lines at points where bodies touch.
 *       Length indicates penetration depth.</li>
 *   <li><strong>AABBs</strong> — axis-aligned bounding boxes used by the broadphase
 *       to quickly rule out non-colliding pairs before expensive narrowphase checks.</li>
 * </ul>
 *
 * <h3>Performance</h3>
 * <p>Debug drawing is expensive — it uploads fresh vertex data to the GPU every frame.
 * Never enable in production. Use {@link PhysicsWorldConfig#debugEnabled()} to toggle.</p>
 */
class PhysicsDebugDrawer extends IDebugDraw implements Disposable {

    private static final Log log = Log.create(PhysicsDebugDrawer.class);

    private record Line(org.joml.Vector3f from, org.joml.Vector3f to, org.joml.Vector3f color) {}

    private final List<Line> lines = new ArrayList<>();
    private int debugMode = DebugDrawModes.DRAW_WIREFRAME | DebugDrawModes.DRAW_CONTACT_POINTS | DebugDrawModes.DRAW_AABB;

    private int vao;
    private int vbo;
    private int shaderProgram;
    private boolean gpuInitialized;

    @Override
    public void drawLine(Vector3f from, Vector3f to, Vector3f color) {
        lines.add(new Line(
            new org.joml.Vector3f(from.x, from.y, from.z),
            new org.joml.Vector3f(to.x, to.y, to.z),
            new org.joml.Vector3f(color.x, color.y, color.z)
        ));
    }

    @Override
    public void drawContactPoint(Vector3f pointOnB, Vector3f normalOnB, float distance, int lifeTime, Vector3f color) {
        Vector3f to = new Vector3f(
            pointOnB.x + normalOnB.x * distance,
            pointOnB.y + normalOnB.y * distance,
            pointOnB.z + normalOnB.z * distance
        );
        drawLine(pointOnB, to, color);
    }

    @Override
    public void reportErrorWarning(String warningString) {
        log.error("[Physics] %s", warningString);
    }

    @Override
    public void draw3dText(Vector3f location, String textString) {}

    @Override
    public void setDebugMode(int debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public int getDebugMode() {
        return debugMode;
    }

    void render(Matrix4f projection, Matrix4f view) {
        if (lines.isEmpty()) return;

        if (!gpuInitialized) {
            initGpu();
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

    private void initGpu() {
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

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        gpuInitialized = true;
    }

    @Override
    public void dispose() {
        if (gpuInitialized) {
            glDeleteBuffers(vbo);
            glDeleteVertexArrays(vao);
            glDeleteProgram(shaderProgram);
            gpuInitialized = false;
        }
    }
}
