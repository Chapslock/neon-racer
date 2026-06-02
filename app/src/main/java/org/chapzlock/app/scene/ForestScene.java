package org.chapzlock.app.scene;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.systems.PlayerInputSystem;
import org.chapzlock.app.systems.PlayerAnimationSystem;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.component.AmbientLight;
import org.chapzlock.core.component.Attenuation;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.DirectionalLight;
import org.chapzlock.core.component.Fog;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.SpriteDescriptor;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.input.Key;
import org.chapzlock.core.render.CameraFreeRoamSystem;
import org.chapzlock.core.scene.EntityBlueprintBuilder;
import org.chapzlock.core.scene.Scene;
import org.joml.Vector3f;

public class ForestScene {

    public static Scene create() {
        return Scene.builder("forest")
            .entity("terrain", e -> e
                .gltfModel("glb/terrain.glb"))
            .entity("sun", e -> e
                .directionalLight(DirectionalLight.builder()
                    .direction(new Vector3f(-0.2f, -1f, -0.3f))
                    .color(new Color(1.0f, 0.95f, 0.8f))
                    .build()))
            .entity("torch-1", e -> e
                .transform(Transform.at(0, -50, -5))
                .pointLight(PointLight.builder()
                    .color(new Color(1.0f, 0.3f, 0.1f))
                    .attenuation(Attenuation.DEFAULT)
                    .build()))
            .entity("torch-2", e -> e
                .transform(Transform.at(30, -50, -30))
                .pointLight(PointLight.builder()
                    .color(new Color(0.2f, 0.4f, 1.0f))
                    .attenuation(Attenuation.DEFAULT)
                    .build()))
            .entity("torch-3", e -> e
                .transform(Transform.at(-20, -50, -40))
                .pointLight(PointLight.builder()
                    .color(new Color(0.0f, 1.0f, 0.5f))
                    .attenuation(Attenuation.DEFAULT)
                    .build()))
            .entity(e -> e.fog(Fog.builder().density(0.012f).gradient(2.0f).build()))
            .entity(e -> e.ambient(AmbientLight.builder().strength(0.5f).build()))
            .entity("player", e -> e
                .transform(Transform.at(0, 1, -3, 0, 90, 0))
                .gltfModel("glb/player-character.glb")
                .tag(new PlayerTag())
                .component(new PlayerInputComponent()))
            .entity("healthbar", e -> e
                .transform(Transform.at(0, 0, 0, 0, 0, 0, 250))
                .sprite(SpriteDescriptor.of("textures/health-bar.png")))

            .entity(e -> e.camera(new Camera(new Vector3f(0, 3, 5))))

            .entity(EntityBlueprintBuilder::sky)
            .systems(
                new PlayerInputSystem(),
                new PlayerAnimationSystem(),
                new CameraFreeRoamSystem())
            .inputMap(InputMap.builder()
                .action("quit", Key.ESCAPE)
                .axis("moveForward", Key.S, Key.W)
                .axis("moveRight", Key.A, Key.D)
                .build())
            .build();
    }
}
