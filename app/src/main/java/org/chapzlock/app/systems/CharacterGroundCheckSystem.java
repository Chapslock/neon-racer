package org.chapzlock.app.systems;

import java.util.List;
import java.util.Optional;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.chapzlock.core.physics.Physics;
import org.chapzlock.core.physics.RaycastHit;
import org.joml.Vector3f;

/**
 * Raycasts downward from each player entity to detect ground contact.
 *
 * <p>Each frame, this system casts a short ray straight down from the player's center
 * of mass. If the ray hits something within {@link #RAY_LENGTH} units, the player is
 * considered grounded. The result is written to {@link CharacterState#setGrounded(boolean)}.
 *
 * <p>This system runs before movement and jump systems so they can read the grounded
 * state when deciding whether to apply forces (e.g., jump only when grounded, apply
 * ground friction vs air control).
 *
 * <h3>Ray geometry</h3>
 * <p>The ray starts at the entity's transform position (capsule center) and extends
 * downward by {@code CAPSULE_HALF_HEIGHT + GROUND_CHECK_MARGIN}. The margin accounts
 * for floating-point imprecision and minor penetration at rest.</p>
 */
public class CharacterGroundCheckSystem implements GameSystem {

    private static final float CAPSULE_HALF_HEIGHT = 0.6f;
    private static final float GROUND_CHECK_MARGIN = 0.05f;
    private static final float RAY_LENGTH = CAPSULE_HALF_HEIGHT + GROUND_CHECK_MARGIN;
    private static final Vector3f DOWN = new Vector3f(0, -1, 0);

    private ComponentRegistry registry;
    private Physics physics;

    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

    @Override
    public List<Class<? extends GameSystem>> runBefore() {
        return List.of(CharacterMovementSystem.class, CharacterJumpSystem.class);
    }

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
        this.physics = ctx.getPhysics();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(PlayerTag.class, Transform.class, CharacterState.class,
            (entityId, tag, transform, state) -> {
                Vector3f pos = new Vector3f();
                transform.getPosition(pos);

                Optional<RaycastHit> hit = physics.raycast(pos, DOWN, RAY_LENGTH,
                    0xFFFFFFFF, entityId);

                state.setGrounded(hit.isPresent());
            });
    }
}
