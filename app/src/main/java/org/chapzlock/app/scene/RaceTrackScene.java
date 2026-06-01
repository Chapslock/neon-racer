package org.chapzlock.app.scene;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.systems.PlayerInputSystem;
import org.chapzlock.app.systems.PlayerModelSystem;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.component.AmbientLight;
import org.chapzlock.core.component.Attenuation;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.DirectionalLight;
import org.chapzlock.core.component.Fog;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.SpriteDescriptor;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.input.Key;
import org.chapzlock.core.render.CameraFreeRoamSystem;
import org.chapzlock.core.scene.Scene;
import org.joml.Vector3f;

public class RaceTrackScene {

    public static Scene create() {
        return Scene.builder("race-track")
            .fromGltf("glb/forest-scene.glb")
            .inputMap(InputMap.builder()
                .action("quit", Key.ESCAPE)
                .axis("moveForward", Key.S, Key.W)
                .axis("moveRight", Key.A, Key.D)
                .build())
            .directionalLight(DirectionalLight.builder()
                .direction(new Vector3f(-0.2f, -1f, -0.3f))
                .color(new Color(1.0f, 0.95f, 0.8f))
                .build())
            .pointLight(PointLight.builder()
                .position(new Vector3f(0, -50, -5))
                .color(new Color(1.0f, 0.3f, 0.1f))
                .attenuation(Attenuation.DEFAULT)
                .build())
            .pointLight(PointLight.builder()
                .position(new Vector3f(30, -50, -30))
                .color(new Color(0.2f, 0.4f, 1.0f))
                .attenuation(Attenuation.DEFAULT)
                .build())
            .pointLight(PointLight.builder()
                .position(new Vector3f(-20, -50, -40))
                .color(new Color(0.0f, 1.0f, 0.5f))
                .attenuation(Attenuation.DEFAULT)
                .build())
            .fog(Fog.builder().density(0.012f).gradient(2.0f).build())
            .ambient(AmbientLight.builder().strength(0.5f)
                .build())
            .entity("player", e -> e
                .transform(Transform.at(0, 0, -3, 0, 90, 0))
                .tag(new PlayerTag())
                .component(new PlayerInputComponent()))
            .entity("speedometer", e -> e
                .transform(Transform.at(0, 0, 0, 0, 0, 0, 200))
                .sprite(SpriteDescriptor.of("textures/speedometer.png")))
            .camera(0, 0, 5)
            .sky()
            .systems(
                new PlayerInputSystem(),
                new PlayerModelSystem(),
                //new PlayerMovementSystem(),
                //new PlayerCameraFollowSystem()
                new CameraFreeRoamSystem()
            )
            .build();
    }
}
