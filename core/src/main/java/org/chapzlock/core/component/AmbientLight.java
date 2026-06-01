package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

import lombok.Builder;
import lombok.Getter;

/**
 * Controls the minimum illumination level for all surfaces in the scene,
 * simulating indirect light that bounces between surfaces in the real world.
 *
 * <p>Without ambient light, surfaces not directly facing a light source would be
 * completely black. The ambient strength acts as a floor: a value of 0.2 means
 * every surface receives at least 20% of its base color regardless of light direction.</p>
 *
 * <p>Ambient is applied uniformly — it has no direction or position. It simply
 * prevents unrealistically dark shadows in a forward-rendered scene that has no
 * global illumination.</p>
 *
 * <p>Opt-in as an entity component. If no AmbientLight entity exists, the default
 * strength ({@link #DEFAULT_STRENGTH}) is used by the renderer.</p>
 */
@Getter
@Builder
public class AmbientLight implements Component {

    /** Default ambient strength: 20% base illumination on all surfaces. */
    public static final float DEFAULT_STRENGTH = 0.2f;

    @Builder.Default
    private final float strength = DEFAULT_STRENGTH;
}
