package org.chapzlock.core.graphics.material;

import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_LIGHT_COLOR;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_LIGHT_POSITION;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_PROJECTION_MATRIX;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_REFLECTIVITY;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_SHINE_DAMPER;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_TEXTURE_0;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_TEXTURE_1;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_TEXTURE_2;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_TEXTURE_BLEND_MAP;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_TRANSFORMATION_MATRIX;
import static org.chapzlock.core.graphics.shader.TerrainShaderProps.UNIFORM_VIEW_MATRIX;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.EntityView;
import org.chapzlock.core.graphics.shader.TerrainShaderProps;
import org.chapzlock.core.system.CameraSystem;
import org.chapzlock.core.system.ShaderSystem;
import org.chapzlock.core.system.TextureSystem;

public class TerrainMaterialRenderer implements MaterialRenderer {
    private final ShaderSystem shaderSystem = ShaderSystem.instance();
    private final TextureSystem textureSystem = TextureSystem.instance();
    private final CameraSystem cameraSystem = new CameraSystem();

    @Override
    public void apply(Material material, Camera camera, PointLight light, Sky sky) {
        Shader shader = material.getShader();
        shaderSystem.use(shader);

        bindTextures(material, shader);
        bindBlendMap(material, shader);

        if (sky != null) {
            shaderSystem.setUniform(shader, TerrainShaderProps.UNIFORM_SKY_COLOR, sky.getColor().toVector3f());
            shaderSystem.setUniform(shader, TerrainShaderProps.UNIFORM_FOG_DENSITY, sky.getFogDensity());
            shaderSystem.setUniform(shader, TerrainShaderProps.UNIFORM_FOG_GRADIENT, sky.getFogGradient());
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

    private void bindTextures(Material material, Shader shader) {
        for (int i = 0; i < material.getTextures().size(); i++) {
            textureSystem.bind(material.getTextures().get(i), i);
        }
        shaderSystem.setUniform(shader, UNIFORM_TEXTURE_0, 0);
        shaderSystem.setUniform(shader, UNIFORM_TEXTURE_1, 1);
        shaderSystem.setUniform(shader, UNIFORM_TEXTURE_2, 2);
    }

    private void bindBlendMap(Material material, Shader shader) {
        Texture blendMap = material.getBlendMap();
        if (blendMap != null) {
            textureSystem.bind(blendMap, material.getTextures().size());
            shaderSystem.setUniform(shader, UNIFORM_TEXTURE_BLEND_MAP, material.getTextures().size());
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
