package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Vector4f;

import lombok.Getter;

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
 * <p><strong>Deferred loading:</strong> Supports both immediate (texture already loaded)
 * and deferred (path-based) construction. When constructed with a path, the texture is
 * null until resolved via {@link #withTexture(Texture)}.
 *
 * @see SpriteLayer
 * @see SpriteDescriptor
 */
@Getter
public class SpriteMaterial implements Component {

    private final String texturePath;
    private final Texture texture;
    private final Vector4f tint;

    /**
     * Creates a sprite material with deferred texture loading and default tint.
     *
     * @param texturePath the asset path to the texture (resolved later)
     */
    public SpriteMaterial(String texturePath) {
        this(texturePath, null, new Vector4f(1f, 1f, 1f, 1f));
    }

    /**
     * Creates a sprite material with deferred texture loading and custom tint.
     *
     * @param texturePath the asset path to the texture (resolved later)
     * @param tint RGBA color multiplied with the texture sample
     */
    public SpriteMaterial(String texturePath, Vector4f tint) {
        this(texturePath, null, tint);
    }

    /**
     * Creates a sprite material with an already-loaded texture and default tint.
     *
     * @param texture the texture to render on the sprite quad
     */
    public SpriteMaterial(Texture texture) {
        this(null, texture, new Vector4f(1f, 1f, 1f, 1f));
    }

    /**
     * Creates a sprite material with an already-loaded texture and custom tint.
     *
     * @param texture the texture to render on the sprite quad
     * @param tint RGBA color multiplied with the texture sample (white = no tint)
     */
    public SpriteMaterial(Texture texture, Vector4f tint) {
        this(null, texture, tint);
    }

    private SpriteMaterial(String texturePath, Texture texture, Vector4f tint) {
        this.texturePath = texturePath;
        this.texture = texture;
        this.tint = tint;
    }

    /**
     * Returns true if this material was constructed with a path and needs texture resolution.
     *
     * @return true when texture is null and texturePath is set
     */
    public boolean needsResolution() {
        return texture == null && texturePath != null;
    }

    /**
     * Returns a new SpriteMaterial with the texture resolved.
     * The path and tint are preserved from this instance.
     *
     * @param resolved the loaded texture
     * @return a new resolved SpriteMaterial
     */
    public SpriteMaterial withTexture(Texture resolved) {
        return new SpriteMaterial(texturePath, resolved, tint);
    }

    // Record-style accessors for backward compatibility
    public Texture texture() { return texture; }
    public Vector4f tint() { return tint; }
    public String texturePath() { return texturePath; }
}
