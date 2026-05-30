package org.chapzlock.app.scene;

import java.util.List;

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
                .build())
            .pointLight(PointLight.builder()
                .position(new Vector3f(-5, 300, -10))
                .build())
            .fog(Fog.builder().build())
            .ambient(AmbientLight.builder().build())
            .entity("player", e -> e
                .transform(Transform.at(0, 10, -5, 0, 0, 180))
                .model(ModelDescriptor.builder()
                    .mesh("obj/funcar.obj")
                    .texture("textures/funcar.png")
                    .reflectivity(0.5f)
                    .shineDamper(1f)
                    .build())
                .physics(PhysicsDescriptor.dynamic(1f))
                .tag(new PlayerTag())
                .component(new PlayerInputComponent()))
            .entity("terrain", e -> e
                .transform(Transform.at(-400, 0, -400, 10, 0, 0))
                .terrain(TerrainDescriptor.builder()
                    .heightmap("textures/terrain/heightmap.png")
                    .maxHeight(40f)
                    .textures(List.of(
                        "textures/terrain/grass.png",
                        "textures/terrain/dirt.png",
                        "textures/terrain/concrete.png"))
                    .blendMap("textures/terrain/blendMap.png")
                    .build())
                .physics(PhysicsDescriptor.asStatic()))
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
