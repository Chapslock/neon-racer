package org.chapzlock.core.graphics.material;

import static org.chapzlock.core.graphics.shader.EntityShaderProps.UNIFORM_LIGHT_COLOR;
import static org.chapzlock.core.graphics.shader.EntityShaderProps.UNIFORM_LIGHT_POSITION;
import static org.chapzlock.core.graphics.shader.EntityShaderProps.UNIFORM_PROJECTION_MATRIX;
import static org.chapzlock.core.graphics.shader.EntityShaderProps.UNIFORM_REFLECTIVITY;
import static org.chapzlock.core.graphics.shader.EntityShaderProps.UNIFORM_SHINE_DAMPER;
import static org.chapzlock.core.graphics.shader.EntityShaderProps.UNIFORM_TEXTURE_SAMPLER;
import static org.chapzlock.core.graphics.shader.EntityShaderProps.UNIFORM_TRANSFORMATION_MATRIX;
import static org.chapzlock.core.graphics.shader.EntityShaderProps.UNIFORM_VIEW_MATRIX;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.graphics.shader.EntityShaderProps;
import org.chapzlock.core.system.CameraSystem;
import org.chapzlock.core.system.ShaderSystem;
import org.chapzlock.core.system.TextureSystem;

public class EntityMaterialRenderer implements MaterialRenderer {
    private final ShaderSystem shaderSystem = ShaderSystem.instance();
    private final TextureSystem textureSystem = TextureSystem.instance();
    private final CameraSystem cameraSystem = new CameraSystem();

    @Override
    public void apply(Material material, Camera camera, PointLight light, Sky sky) {
        Shader shader = material.getShader();
        shaderSystem.use(shader);

        Texture texture = material.getFirstTexture();
        if (texture != null) {
            textureSystem.bind(texture, 0);
            shaderSystem.setUniform(shader, UNIFORM_TEXTURE_SAMPLER, 0);
        }

        if (sky != null) {
            shaderSystem.setUniform(shader, EntityShaderProps.UNIFORM_SKY_COLOR, sky.getColor().toVector3f());
            shaderSystem.setUniform(shader, EntityShaderProps.UNIFORM_FOG_DENSITY, sky.getFogDensity());
            shaderSystem.setUniform(shader, EntityShaderProps.UNIFORM_FOG_GRADIENT, sky.getFogGradient());
        }

        shaderSystem.setUniform(shader, UNIFORM_SHINE_DAMPER, material.getReflection().getShineDamper());
        shaderSystem.setUniform(shader, UNIFORM_REFLECTIVITY, material.getReflection().getReflectivity());
        shaderSystem.setUniform(shader, UNIFORM_VIEW_MATRIX, cameraSystem.calculateViewMatrix(camera));
        shaderSystem.setUniform(shader, UNIFORM_PROJECTION_MATRIX, cameraSystem.calculateProjectionMatrix(camera));
        if (light != null) {
            shaderSystem.setUniform(shader, UNIFORM_LIGHT_POSITION, light.position());
            shaderSystem.setUniform(shader, UNIFORM_LIGHT_COLOR, light.color().toVector3f());
        }
    }

    @Override
    public void prepareEntity(EntityView entity, Material material) {
        shaderSystem.setUniform(material.getShader(), UNIFORM_TRANSFORMATION_MATRIX, entity.get(Transform.class)
            .calculateTransformationMatrix());
    }

    @Override
    public void unapply(Material material) {
        Texture texture = material.getFirstTexture();
        if (texture != null) {
            textureSystem.unbind(texture);
        }
        shaderSystem.clearProgram();
    }
}
