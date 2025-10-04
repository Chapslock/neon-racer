package org.chapzlock.application;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.chapzlock.core.Layer;
import org.chapzlock.core.graphics.Shader;
import org.chapzlock.core.graphics.ShaderProgram;
import org.chapzlock.core.graphics.VertexArrayObject;
import org.chapzlock.core.graphics.VertexBufferObject;
import org.chapzlock.core.math.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

public class AppLayer implements Layer {

    private final CharSequence vertexSource
        = "#version 150 core\n"
        + "\n"
        + "in vec3 position;\n"
        + "in vec3 color;\n"
        + "\n"
        + "out vec3 vertexColor;\n"
        + "\n"
        + "uniform mat4 model;\n"
        + "uniform mat4 view;\n"
        + "uniform mat4 projection;\n"
        + "\n"
        + "void main() {\n"
        + "    vertexColor = color;\n"
        + "    mat4 mvp = projection * view * model;\n"
        + "    gl_Position = mvp * vec4(position, 1.0);\n"
        + "}";
    private final CharSequence fragmentSource
        = "#version 150 core\n"
        + "\n"
        + "in vec3 vertexColor;\n"
        + "\n"
        + "out vec4 fragColor;\n"
        + "\n"
        + "void main() {\n"
        + "    fragColor = vec4(vertexColor, 1.0);\n"
        + "}";

    private VertexArrayObject vao;
    private VertexBufferObject vbo;
    private Shader vertexShader;
    private Shader fragmentShader;
    private ShaderProgram program;

    private int uniModel;
    private float previousAngle = 0f;
    private float angle = 0f;
    private final float anglePerSecond = 50f;

    public AppLayer() {
        /* Generate Vertex Array Object */
        vao = new VertexArrayObject();
        vao.bind();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            /* Vertex data */
            FloatBuffer vertices = stack.mallocFloat(3 * 6);
            vertices.put(-0.6f).put(-0.4f).put(0f).put(1f).put(0f).put(0f);
            vertices.put(0.6f).put(-0.4f).put(0f).put(0f).put(1f).put(0f);
            vertices.put(0f).put(0.6f).put(0f).put(0f).put(0f).put(1f);
            vertices.flip();

            /* Generate Vertex Buffer Object */
            vbo = new VertexBufferObject();
            vbo.bind(GL_ARRAY_BUFFER);
            vbo.uploadData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        }

        /* Load shaders */
        vertexShader = Shader.createShader(GL_VERTEX_SHADER, vertexSource);
        fragmentShader = Shader.createShader(GL_FRAGMENT_SHADER, fragmentSource);

        /* Create shader program */
        program = new ShaderProgram();
        program.attachShader(vertexShader);
        program.attachShader(fragmentShader);
        program.bindFragmentDataLocation(0, "fragColor");
        program.link();
        program.use();

        specifyVertexAttributes();

        /* Get uniform location for the model matrix */
        uniModel = program.getUniformLocation("model");

        /* Set view matrix to identity matrix */
        Matrix4f view = new Matrix4f();
        int uniView = program.getUniformLocation("view");
        program.setUniform(uniView, view);

        /* Get width and height for calculating the ratio */
        float ratio;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long window = GLFW.glfwGetCurrentContext();
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(window, width, height);
            ratio = width.get() / (float) height.get();
        }
        /* Set projection matrix to an orthographic projection */
        Matrix4f projection = Matrix4f.orthographic(-ratio, ratio, -1f, 1f, -1f, 1f);
        int uniProjection = program.getUniformLocation("projection");
        program.setUniform(uniProjection, projection);
    }

    private void specifyVertexAttributes() {
        /* Specify Vertex Pointer */
        int posAttrib = program.getAttributeLocation("position");
        program.enableVertexAttribute(posAttrib);
        program.pointVertexAttribute(posAttrib, 3, 6 * Float.BYTES, 0);

        /* Specify Color Pointer */
        int colAttrib = program.getAttributeLocation("color");
        program.enableVertexAttribute(colAttrib);
        program.pointVertexAttribute(colAttrib, 3, 6 * Float.BYTES, 3 * Float.BYTES);
    }

    @Override
    public void onUpdate(float deltaTime) {
        previousAngle = angle;
        angle += deltaTime * anglePerSecond;
    }

    @Override
    public void onRender(float deltaTime) {
        glClear(GL_COLOR_BUFFER_BIT);

        vao.bind();
        program.use();

        float lerpAngle = (1f - deltaTime) * previousAngle + deltaTime * angle;
        Matrix4f model = Matrix4f.rotate(lerpAngle, 0f, 0f, 1f);
        program.setUniform(uniModel, model);

        glDrawArrays(GL_TRIANGLES, 0, 3);
    }

    public void onTransition() {
        vao.delete();
        vbo.delete();
        vertexShader.delete();
        fragmentShader.delete();
        program.delete();
    }
}
