package org.chapzlock.app.systems;

import java.util.List;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.input.InputMap;
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
    private EngineContext context;

    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

    @Override
    public List<Class<? extends GameSystem>> runAfter() {
        return List.of(CharacterGroundCheckSystem.class);
    }

    @Override
    public void onInit(EngineContext ctx) {
        this.context = ctx;
        this.registry = ctx.getRegistry();
        this.physics = ctx.getPhysics();
    }

    @Override
    public void onUpdate(float deltaTime) {
        InputMap input = context.getActiveInputMap();
        if (input == null) return;

        if (!input.wasJustPressed("jump")) return;

        registry.forEach(PlayerTag.class, CharacterState.class,
            (entityId, tag, state) -> {
                if (!state.isGrounded()) return;

                physics.applyImpulse(entityId, new Vector3f(0, JUMP_IMPULSE, 0));
                state.setGrounded(false);
            });
    }
}
