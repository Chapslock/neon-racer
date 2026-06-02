package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector4f;

/**
 * Material component for 2D sprites rendered with orthographic projection.
 *
 * <p><strong>What is a sprite?</strong> A flat 2D image drawn on screen — used for HUD
 * elements (health bars, minimaps), UI buttons, particle effects, or any 2D overlay.
 * Unlike 3D PBR materials which simulate physical light interaction, sprites are simply
 * textured quads with optional color tinting and alpha blending.
 *
 * <p><strong>How it works:</strong> The {@link org.chapzlock.core.render.SpriteRenderer}
 * owns the sprite shader program internally. This component is pure data — it tells the
 * renderer WHAT to draw (which texture, what tint), not HOW to draw it.
 *
 * <p><strong>Usage:</strong> Typically created via
 * {@link org.chapzlock.core.scene.DescriptorResolver} from a {@link SpriteDescriptor}
 * during scene instantiation. Game code does not create these manually.
 *
 * @param texture the texture to render on the sprite quad
 * @param tint RGBA color multiplied with the texture sample (white = no tint)
 * @see SpriteLayer
 * @see SpriteDescriptor
 */
public record SpriteMaterial(Texture texture, Vector4f tint) implements Component {

    /**
     * Creates a sprite material with no tint (texture colors rendered as-is).
     *
     * @param texture the texture to render
     */
    public SpriteMaterial(Texture texture) {
        this(texture, new Vector4f(1f, 1f, 1f, 1f));
    }
}
