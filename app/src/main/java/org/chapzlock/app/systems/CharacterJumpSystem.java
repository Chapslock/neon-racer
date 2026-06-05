package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityBuilder;
import org.chapzlock.core.ecs.EntityFactory;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.particle.EmitShape;
import org.chapzlock.core.particle.ParticleEffect;
import org.chapzlock.core.particle.ParticleEmitter;
import org.chapzlock.core.physics.Physics;
import org.joml.Vector3f;

/**
 * Applies an upward impulse when the player presses the jump action while grounded.
 *
 * <p>This system checks the active {@link InputMap} for a "jump" action press each frame.
 * If the action was just pressed (not held) and the player's {@link CharacterState} reports
 * grounded, an instantaneous upward impulse of {@link #JUMP_IMPULSE} units is applied via
 * the physics engine. The grounded flag is immediately set to false to prevent double-jumps
 * before the ground check system re-evaluates on the next frame.</p>
 *
 * <p>Runs after {@link CharacterGroundCheckSystem} so it reads a fresh grounded state each frame.</p>
 */
public class CharacterJumpSystem implements GameSystem {

    private static final float JUMP_IMPULSE = 5f;

    private ComponentRegistry registry;
    private Physics physics;
    private Engine engine;
    private EntityFactory entityFactory;

    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

    @Override
    public List<Class<? extends GameSystem>> runAfter() {
        return List.of(CharacterGroundCheckSystem.class);
    }

    @Override
    public void onInit(Engine engine) {
        this.engine = engine;
        this.registry = engine.getRegistry();
        this.physics = engine.getPhysics();
        this.entityFactory = engine.getEntityFactory();
    }

    @Override
    public void onUpdate(float deltaTime) {
        InputMap input = engine.getSceneManager().getActiveInputMap();
        if (input == null) return;

        if (!input.wasJustPressed("jump")) return;

        registry.forEach(PlayerTag.class, CharacterState.class,
            (entityId, tag, state) -> {
                if (!state.isGrounded()) return;

                physics.applyImpulse(entityId, new Vector3f(0, JUMP_IMPULSE, 0));
                state.setGrounded(false);
                int jumpEffect = entityFactory.instantiate(
                    EntityBuilder.create()
                        .transform(Transform.at(0, 0, 0))
                        .component(new ParticleEmitter(ParticleEffect.builder()
                            .burst(20)
                            .lifetime(0.3f, 0.6f)
                            .speed(1f, 3f)
                            .size(0.05f, 0.15f)
                            .color(new Color(0.2f, 0.8f, 1f, 1f))
                            .emitShape(EmitShape.cone(60f))
                            .maxParticles(25)
                            .colorOverLifetime(
                                new Color(0.2f, 0.8f, 1f, 1f),
                                new Color(0.1f, 0.3f, 1f, 0f))
                            .sizeOverLifetime(1f, 0f)
                            .gravity(0f, -2f, 0f)
                            .drag(1f)
                            .build())));
                entityFactory.attachChild(entityId, jumpEffect);
            });
    }
}
