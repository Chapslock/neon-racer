package org.chapzlock.core;

import org.chapzlock.core.graphics.Material;
import org.chapzlock.core.graphics.Mesh;
import org.chapzlock.core.math.MathUtil;
import org.chapzlock.core.math.Matrix4f;
import org.chapzlock.core.math.Vector3f;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity is an instance of a renderable object with a texture and mesh
 */
@Getter
@Setter
public class Entity {

    private Mesh mesh;
    private Material material;
    private Vector3f position;
    private Vector3f rotation;
    private float scale;

    public Entity(Mesh mesh, Material material, Vector3f position, Vector3f rotation, float scale) {
        this.mesh = mesh;
        this.material = material;
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    public void move(Vector3f value) {
        this.position.x+=value.x;
        this.position.y+=value.y;
        this.position.z+=value.z;
    }

    public void rotate(Vector3f value) {
        this.rotation.x+=value.x;
        this.rotation.y+=value.y;
        this.rotation.z+=value.z;
    }

    public void onRender(Camera camera) {
        material.bind();

        material.getShader().loadTransformationMatrix(MathUtil.createTransformationMatrix(
            position,
            rotation,
            scale
        ));
        material.getShader().loadProjectionMatrix(Matrix4f.perspective(
            70f,
            600f / 400f,
            0.1f,
            1000f
        ));

        material.getShader().loadViewMatrix(MathUtil.createViewMatrix(
            camera.getPosition(),
            camera.getRotation()
        ));

        mesh.render();
        material.unbind();
    }
}
