package org.chapzlock.app.scene;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.component.ThirdPersonCamera;
import org.chapzlock.app.systems.CharacterAnimationSystem;
import org.chapzlock.app.systems.CharacterGroundCheckSystem;
import org.chapzlock.app.systems.CharacterJumpSystem;
import org.chapzlock.app.systems.CharacterMovementSystem;
import org.chapzlock.app.systems.CharacterRotationSystem;
import org.chapzlock.app.systems.PlayerInputSystem;
import org.chapzlock.app.systems.ThirdPersonCameraSystem;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.component.AmbientLight;
import org.chapzlock.core.component.Attenuation;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.DirectionalLight;
import org.chapzlock.core.component.Fog;
import org.chapzlock.core.component.GltfModel;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.component.SpriteLayer;
import org.chapzlock.core.component.SpriteMaterial;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.input.Key;
import org.chapzlock.core.input.MouseButton;
import org.chapzlock.core.particle.EmitShape;
import org.chapzlock.core.particle.ParticleEffect;
import org.chapzlock.core.particle.ParticleEmitter;
import org.chapzlock.core.physics.CollisionGeometry;
import org.chapzlock.core.scene.Scene;
import org.joml.Vector3f;

public class ForestScene {

    public static Scene create() {
        return Scene.builder("forest")
            .entity("terrain", e -> e
                .transform(Transform.at(0, 0, 0))
                .component(new GltfModel("glb/terrain.glb"))
                .component(PhysicsBody.asStatic()
                    .shape(CollisionGeometry.triangleMesh())))
            .entity("sky", e -> e
                .component(DirectionalLight.builder()
                    .direction(new Vector3f(-0.2f, -1f, -0.3f))
                    .color(new Color(1.0f, 0.95f, 0.8f))
                    .build()))
            .entity("torch-1", e -> e
                .transform(Transform.at(0, 0, 0))
                .component(PointLight.builder()
                    .color(new Color(1.0f, 0.3f, 0.1f))
                    .attenuation(Attenuation.DEFAULT)
                    .build()))
            .entity(e -> e.component(Fog.builder().density(0.012f).gradient(2.0f).build()))
            .entity(e -> e.component(AmbientLight.builder().strength(0.5f).build()))
            .entity("player", e -> e
                .transform(Transform.at(0, 3, 0))
                .component(new GltfModel("glb/player-character.glb"))
                .component(PhysicsBody.dynamic(1)
                    .shape(CollisionGeometry.capsule(0.3f, 0.6f)
                        .offset(0, 0.3f, 0))
                    .lockRotation())
                .tag(new PlayerTag())
                .component(new PlayerInputComponent())
                .component(new CharacterState())
                .child("cape-particles", c -> c
                    .transform(Transform.at(0, .7f, 0))
                    .component(new ParticleEmitter(ParticleEffect.builder()
                        .rate(15)
                        .lifetime(0.8f, 1.5f)
                        .speed(0.3f, 0.8f)
                        .size(0.05f, 0.12f)
                        .color(new Color(0.8f, 0.1f, 0.4f, 0.8f))
                        .emitShape(EmitShape.cone(35f))
                        .maxParticles(50)
                        .colorOverLifetime(
                            new Color(0.8f, 0.1f, 0.4f, 0.8f),
                            new Color(0.3f, 0.0f, 0.2f, 0.0f))
                        .sizeOverLifetime(1f, 0.3f)
                        .gravity(0f, 0.5f, 0f)
                        .drag(0.3f)
                        .build()))))
            .entity("healthbar", e -> e
                .transform(Transform.at(0, 0, 0, 0, 0, 0, 250))
                .component(new SpriteMaterial("textures/health-bar.png"))
                .component(new SpriteLayer()))
            .entity(e -> e
                .transform(Transform.at(0, 3, 5))
                .component(new Camera())
                .component(new ThirdPersonCamera()))
            .entity(e -> e.component(new Sky()))
            .systems(
                new PlayerInputSystem(),
                new CharacterGroundCheckSystem(),
                new CharacterMovementSystem(),
                new CharacterJumpSystem(),
                new CharacterRotationSystem(),
                new CharacterAnimationSystem(),
                new ThirdPersonCameraSystem())
            .inputMap(InputMap.builder()
                .action("quit", Key.ESCAPE)
                .action("jump", Key.SPACE)
                .action("cameraOrbit", MouseButton.RIGHT)
                .action("cameraLook", MouseButton.LEFT)
                .axis("moveForward", Key.S, Key.W)
                .axis("turnRight", Key.A, Key.D)
                .mouseAxis("lookX", "lookY")
                .build())
            .build();
    }
}
