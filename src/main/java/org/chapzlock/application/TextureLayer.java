package org.chapzlock.application;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.chapzlock.core.Layer;
import org.chapzlock.core.graphics.Shader;
import org.chapzlock.core.graphics.ShaderProgram;
import org.chapzlock.core.graphics.Texture;
import org.chapzlock.core.graphics.VertexArrayObject;
import org.chapzlock.core.graphics.VertexBufferObject;
import org.chapzlock.core.math.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

public class TextureLayer implements Layer {

    private VertexArrayObject vao;
    private VertexBufferObject vbo;
    private VertexBufferObject ebo;
    private Texture texture;
    private Shader vertexShader;
    private Shader fragmentShader;
    private ShaderProgram program;

    public TextureLayer() {
        /* Get width and height of framebuffer */
        int width, height;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long window = GLFW.glfwGetCurrentContext();
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(window, widthBuffer, heightBuffer);
            width = widthBuffer.get();
            height = heightBuffer.get();
        }

        /* Create texture */
        texture = Texture.loadTexture("textures/example.png");
        texture.bind();

        /* Generate Vertex Array Object */
        vao = new VertexArrayObject();
        vao.bind();

        /* Get coordinates for centering the texture on screen */
        float x1 = (width - texture.getWidth()) / 2f;
        float y1 = (height - texture.getHeight()) / 2f;
        float x2 = x1 + texture.getWidth();
        float y2 = y1 + texture.getHeight();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            /* Vertex data */
            FloatBuffer vertices = stack.mallocFloat(4 * 7);
            vertices.put(x1).put(y1).put(1f).put(1f).put(1f).put(0f).put(0f);
            vertices.put(x2).put(y1).put(1f).put(1f).put(1f).put(1f).put(0f);
            vertices.put(x2).put(y2).put(1f).put(1f).put(1f).put(1f).put(1f);
            vertices.put(x1).put(y2).put(1f).put(1f).put(1f).put(0f).put(1f);
            vertices.flip();

            /* Generate Vertex Buffer Object */
            vbo = new VertexBufferObject();
            vbo.bind(GL_ARRAY_BUFFER);
            vbo.uploadData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

            /* Element data */
            IntBuffer elements = stack.mallocInt(2 * 3);
            elements.put(0).put(1).put(2);
            elements.put(2).put(3).put(0);
            elements.flip();

            /* Generate Element Buffer Object */
            ebo = new VertexBufferObject();
            ebo.bind(GL_ELEMENT_ARRAY_BUFFER);
            ebo.uploadData(GL_ELEMENT_ARRAY_BUFFER, elements, GL_STATIC_DRAW);
        }

        /* Load shaders */
        vertexShader = Shader.loadShader(GL20.GL_VERTEX_SHADER, "shaders/vertex.glsl");
        fragmentShader = Shader.loadShader(GL20.GL_FRAGMENT_SHADER, "shaders/fragment.glsl");

        /* Create shader program */
        program = new ShaderProgram();
        program.attachShader(vertexShader);
        program.attachShader(fragmentShader);
        program.bindFragmentDataLocation(0, "fragColor");
        program.link();
        program.use();

        specifyVertexAttributes();

        /* Set texture uniform */
        int uniTex = program.getUniformLocation("texImage");
        program.setUniform(uniTex, 0);

        /* Set model matrix to identity matrix */
        Matrix4f model = new Matrix4f();
        int uniModel = program.getUniformLocation("model");
        program.setUniform(uniModel, model);

        /* Set view matrix to identity matrix */
        Matrix4f view = new Matrix4f();
        int uniView = program.getUniformLocation("view");
        program.setUniform(uniView, view);

        /* Set projection matrix to an orthographic projection */
        Matrix4f projection = Matrix4f.orthographic(0f, width, 0f, height, -1f, 1f);
        int uniProjection = program.getUniformLocation("projection");
        program.setUniform(uniProjection, projection);
    }

    /**
     * Specifies the vertex attributes.
     */
    private void specifyVertexAttributes() {
        /* Specify Vertex Pointer */
        int posAttrib = program.getAttributeLocation("position");
        program.enableVertexAttribute(posAttrib);
        program.pointVertexAttribute(posAttrib, 2, 7 * Float.BYTES, 0);

        /* Specify Color Pointer */
        int colAttrib = program.getAttributeLocation("color");
        program.enableVertexAttribute(colAttrib);
        program.pointVertexAttribute(colAttrib, 3, 7 * Float.BYTES, 2 * Float.BYTES);

        /* Specify Texture Pointer */
        int texAttrib = program.getAttributeLocation("texcoord");
        program.enableVertexAttribute(texAttrib);
        program.pointVertexAttribute(texAttrib, 2, 7 * Float.BYTES, 5 * Float.BYTES);
    }



    @Override
    public void onUpdate(float deltaTime) {

    }

    @Override
    public void onRender(float deltaTime) {
        vao.bind();
        texture.bind();
        program.use();

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    @Override
    public void onDestroy() {
        vao.delete();
        vbo.delete();
        ebo.delete();
        texture.delete();
        vertexShader.delete();
        fragmentShader.delete();
        program.delete();
    }
}
