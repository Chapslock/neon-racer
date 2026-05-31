package org.chapzlock.app.scene;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.systems.PlayerCameraFollowSystem;
import org.chapzlock.app.systems.PlayerInputSystem;
import org.chapzlock.app.systems.PlayerMovementSystem;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.component.*;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.input.Key;
import org.chapzlock.core.scene.Scene;
import org.joml.Vector3f;

public class RaceTrackScene {

    public static Scene create() {
        return Scene.builder("race-track")
            .inputMap(InputMap.builder()
                .action("quit", Key.ESCAPE)
                .axis("throttle", Key.S, Key.W)
                .axis("steer", Key.A, Key.D)
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
            .ambient(AmbientLight.builder().strength(0.1f).build())
            .entity("player", e -> e
                .transform(Transform.at(0, -70, 0, 0, 0, 0))
                .model(ModelDescriptor.builder()
                    .mesh("obj/funcar.obj")
                    .texture("textures/funcar.png")
                    .reflectivity(0.5f)
                    .shineDamper(1f)
                    .build())
                .physics(PhysicsDescriptor.dynamic(1f))
                .tag(new PlayerTag())
                .component(new PlayerInputComponent()))
            .entity("speedometer", e -> e
                .transform(Transform.at(0, 0, 0, 0, 0, 0, 200))
                .sprite(SpriteDescriptor.of("textures/speedometer.png")))
            .camera(0, 5, 10)
            .sky()
            .systems(
                new PlayerInputSystem(),
                new PlayerMovementSystem(),
                new PlayerCameraFollowSystem())
            .build();
    }
}
