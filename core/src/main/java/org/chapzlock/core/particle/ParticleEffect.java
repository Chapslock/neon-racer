package org.chapzlock.core.particle;

import lombok.Getter;
import org.chapzlock.core.component.Color;
import org.joml.Vector3f;

/**
 * Immutable configuration object that defines the visual appearance and behavior of a particle effect.
 * <p>
 * A ParticleEffect describes how particles should be emitted, how they look, and how they move over their
 * lifetime. Multiple emitters can share the same ParticleEffect configuration to create consistent visual
 * effects throughout the game.
 * <p>
 * Built via a fluent builder pattern:
 * <pre>{@code
 * ParticleEffect fire = ParticleEffect.builder()
 *     .rate(100f)                                         // 100 particles per second
 *     .lifetime(0.5f, 1.0f)                              // particles live 0.5-1.0 seconds
 *     .speed(2f, 4f)                                     // initial speed 2-4 units/sec
 *     .size(0.2f, 0.5f)                                  // particle size 0.2-0.5 units
 *     .color(new Color(1f, 0.8f, 0.2f, 1f))             // bright orange-yellow
 *     .colorOverLifetime(
 *         new Color(1f, 0.8f, 0.2f, 1f),                // start bright orange
 *         new Color(0.3f, 0.1f, 0.1f, 0f)               // fade to dark red
 *     )
 *     .sizeOverLifetime(1f, 0f)                          // shrink to nothing
 *     .emitShape(EmitShape.cone(30f))                    // emit in 30° cone
 *     .gravity(0f, 2f, 0f)                               // rise upward
 *     .drag(0.5f)                                        // slow down over time
 *     .texture("textures/particle_soft.png")
 *     .maxParticles(200)
 *     .build();
 * }</pre>
 *
 * @see EmitShape
 * @see Color
 */
@Getter
public final class ParticleEffect {

    /**
     * Number of particles emitted per second.
     */
    private final float rate;

    /**
     * Minimum lifetime in seconds for each particle.
     */
    private final float lifetimeMin;

    /**
     * Maximum lifetime in seconds for each particle.
     */
    private final float lifetimeMax;

    /**
     * Minimum initial speed in world units per second.
     */
    private final float speedMin;

    /**
     * Maximum initial speed in world units per second.
     */
    private final float speedMax;

    /**
     * Minimum particle size in world units (width/height of sprite).
     */
    private final float sizeMin;

    /**
     * Maximum particle size in world units (width/height of sprite).
     */
    private final float sizeMax;

    /**
     * Maximum number of particles that can exist simultaneously for this effect.
     * When this limit is reached, new particles will not be emitted until some die.
     */
    private final int maxParticles;

    /**
     * Base color for particles. Used as the initial color if colorOverLifetime is not set.
     */
    private final Color initialColor;

    /**
     * Shape that defines the emission volume and initial particle directions.
     */
    private final EmitShape emitShape;

    /**
     * Path to particle texture file, or null to use default (solid white).
     */
    private final String texturePath;

    /**
     * Starting color for color-over-lifetime interpolation. Null if not using color animation.
     */
    private final Color colorStart;

    /**
     * Ending color for color-over-lifetime interpolation. Null if not using color animation.
     */
    private final Color colorEnd;

    /**
     * Whether this effect uses color-over-lifetime animation.
     */
    private final boolean hasColorOverLifetime;

    /**
     * Size multiplier at the start of particle lifetime (0-1 normalized age).
     */
    private final float sizeStartScale;

    /**
     * Size multiplier at the end of particle lifetime (0-1 normalized age).
     */
    private final float sizeEndScale;

    /**
     * Whether this effect uses size-over-lifetime animation.
     */
    private final boolean hasSizeOverLifetime;

    /**
     * Constant acceleration applied to particles in world space (e.g., gravity).
     * Use (0, -9.8, 0) for Earth-like downward gravity.
     */
    private final Vector3f gravity;

    /**
     * Drag coefficient (0-1). Higher values slow particles down faster.
     * 0 = no drag, 1 = particles stop immediately.
     */
    private final float drag;

    private ParticleEffect(Builder builder) {
        this.rate = builder.rate;
        this.lifetimeMin = builder.lifetimeMin;
        this.lifetimeMax = builder.lifetimeMax;
        this.speedMin = builder.speedMin;
        this.speedMax = builder.speedMax;
        this.sizeMin = builder.sizeMin;
        this.sizeMax = builder.sizeMax;
        this.maxParticles = builder.maxParticles;
        this.initialColor = builder.initialColor;
        this.emitShape = builder.emitShape;
        this.texturePath = builder.texturePath;
        this.colorStart = builder.colorStart;
        this.colorEnd = builder.colorEnd;
        this.hasColorOverLifetime = builder.hasColorOverLifetime;
        this.sizeStartScale = builder.sizeStartScale;
        this.sizeEndScale = builder.sizeEndScale;
        this.hasSizeOverLifetime = builder.hasSizeOverLifetime;
        this.gravity = builder.gravity;
        this.drag = builder.drag;
    }

    /**
     * Returns whether this effect uses color-over-lifetime animation.
     *
     * @return true if color animation is enabled
     */
    public boolean hasColorOverLifetime() {
        return hasColorOverLifetime;
    }

    /**
     * Returns whether this effect uses size-over-lifetime animation.
     *
     * @return true if size animation is enabled
     */
    public boolean hasSizeOverLifetime() {
        return hasSizeOverLifetime;
    }

    /**
     * Creates a new ParticleEffect builder with default values.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for creating immutable ParticleEffect configurations.
     */
    public static final class Builder {
        private float rate = 10f;
        private float lifetimeMin = 1f;
        private float lifetimeMax = 1f;
        private float speedMin = 1f;
        private float speedMax = 1f;
        private float sizeMin = 0.1f;
        private float sizeMax = 0.1f;
        private int maxParticles = 100;
        private Color initialColor = Color.WHITE;
        private EmitShape emitShape = EmitShape.point();
        private String texturePath = null;
        private Color colorStart = null;
        private Color colorEnd = null;
        private boolean hasColorOverLifetime = false;
        private float sizeStartScale = 1f;
        private float sizeEndScale = 1f;
        private boolean hasSizeOverLifetime = false;
        private Vector3f gravity = new Vector3f(0, 0, 0);
        private float drag = 0f;

        private Builder() {}

        /**
         * Sets the emission rate in particles per second.
         * <p>
         * Higher values create denser effects. For reference:
         * <ul>
         *   <li>5-20: sparse effects (dust, stars)</li>
         *   <li>50-100: moderate effects (fire, smoke)</li>
         *   <li>200+: dense effects (explosions, water spray)</li>
         * </ul>
         * Default: 10
         *
         * @param rate particles emitted per second
         * @return this builder
         */
        public Builder rate(float rate) {
            this.rate = rate;
            return this;
        }

        /**
         * Sets a fixed lifetime for all particles.
         * <p>
         * Use this when you want consistent particle behavior. For more variation, use
         * {@link #lifetime(float, float)} instead.
         * <p>
         * Default: 1.0 second
         *
         * @param lifetime how long each particle lives (in seconds)
         * @return this builder
         */
        public Builder lifetime(float lifetime) {
            this.lifetimeMin = lifetime;
            this.lifetimeMax = lifetime;
            return this;
        }

        /**
         * Sets a random lifetime range for particles.
         * <p>
         * Each particle will live between min and max seconds. Variation creates more
         * natural-looking effects.
         * <p>
         * Examples:
         * <ul>
         *   <li>(0.2, 0.5): quick burst effects</li>
         *   <li>(1.0, 2.0): lingering smoke</li>
         *   <li>(3.0, 5.0): slow ambient particles</li>
         * </ul>
         * Default: 1.0 to 1.0 seconds
         *
         * @param min minimum lifetime in seconds
         * @param max maximum lifetime in seconds
         * @return this builder
         */
        public Builder lifetime(float min, float max) {
            this.lifetimeMin = min;
            this.lifetimeMax = max;
            return this;
        }

        /**
         * Sets the initial speed range for particles.
         * <p>
         * Speed is in world units per second along the emit direction. Higher speeds
         * create more explosive effects, lower speeds create gentle drifting.
         * <p>
         * Examples:
         * <ul>
         *   <li>(0.5, 1.0): slow drift (steam, fog)</li>
         *   <li>(2.0, 4.0): moderate motion (fire, sparks)</li>
         *   <li>(10.0, 15.0): fast projection (bullet impacts, explosions)</li>
         * </ul>
         * Default: 1.0 to 1.0 units/sec
         *
         * @param min minimum speed in world units per second
         * @param max maximum speed in world units per second
         * @return this builder
         */
        public Builder speed(float min, float max) {
            this.speedMin = min;
            this.speedMax = max;
            return this;
        }

        /**
         * Sets the size range for particles in world units.
         * <p>
         * Size represents the width and height of the particle sprite. Variation in size
         * adds visual depth and realism.
         * <p>
         * Examples:
         * <ul>
         *   <li>(0.05, 0.1): tiny particles (dust, sparkles)</li>
         *   <li>(0.2, 0.5): medium particles (fire, smoke)</li>
         *   <li>(1.0, 2.0): large particles (debris, leaves)</li>
         * </ul>
         * Default: 0.1 to 0.1 units
         *
         * @param min minimum particle size in world units
         * @param max maximum particle size in world units
         * @return this builder
         */
        public Builder size(float min, float max) {
            this.sizeMin = min;
            this.sizeMax = max;
            return this;
        }

        /**
         * Sets the base color for particles.
         * <p>
         * This color is multiplied with the particle texture. Use white (default) to show
         * the texture as-is, or tint it with other colors. The alpha channel controls
         * overall opacity.
         * <p>
         * If using {@link #colorOverLifetime}, this also serves as the starting color.
         * <p>
         * Default: Color.WHITE
         *
         * @param color the particle color
         * @return this builder
         */
        public Builder color(Color color) {
            this.initialColor = color;
            return this;
        }

        /**
         * Sets the emission shape that determines particle spawn positions and directions.
         * <p>
         * Examples:
         * <ul>
         *   <li>EmitShape.point(): all particles from a single point (explosions, impacts)</li>
         *   <li>EmitShape.cone(15): narrow cone (rocket exhaust, focused beam)</li>
         *   <li>EmitShape.cone(90): wide cone (fountain, spray)</li>
         * </ul>
         * Default: EmitShape.point()
         *
         * @param shape the emission shape
         * @return this builder
         */
        public Builder emitShape(EmitShape shape) {
            this.emitShape = shape;
            return this;
        }

        /**
         * Sets the maximum number of particles that can exist simultaneously.
         * <p>
         * When this limit is reached, new particles won't spawn until some die. Higher
         * limits allow denser effects but cost more CPU/GPU. Balance this with emission
         * rate and lifetime.
         * <p>
         * Examples:
         * <ul>
         *   <li>50-100: simple effects (small fires, light sparks)</li>
         *   <li>200-500: moderate effects (explosions, heavy smoke)</li>
         *   <li>1000+: dense effects (waterfalls, blizzards)</li>
         * </ul>
         * Default: 100
         *
         * @param max maximum simultaneous particles
         * @return this builder
         */
        public Builder maxParticles(int max) {
            this.maxParticles = max;
            return this;
        }

        /**
         * Enables color animation over particle lifetime.
         * <p>
         * Particles smoothly interpolate from startColor to endColor as they age. Use this
         * to create effects like fire fading from yellow to red to black, or sparks
         * dimming over time.
         * <p>
         * The start color also becomes the initialColor if not set separately.
         * <p>
         * Example: fire that fades out
         * <pre>{@code
         * .colorOverLifetime(
         *     new Color(1.0f, 0.8f, 0.2f, 1.0f),  // bright orange-yellow
         *     new Color(0.3f, 0.1f, 0.1f, 0.0f)   // dark red, transparent
         * )
         * }</pre>
         * Default: disabled
         *
         * @param start color at birth (age 0.0)
         * @param end color at death (age 1.0)
         * @return this builder
         */
        public Builder colorOverLifetime(Color start, Color end) {
            this.colorStart = start;
            this.colorEnd = end;
            this.hasColorOverLifetime = true;
            return this;
        }

        /**
         * Enables size animation over particle lifetime.
         * <p>
         * Particles scale from startScale to endScale as they age. The base size is
         * randomly chosen from the size range, then multiplied by the current scale.
         * <p>
         * Examples:
         * <ul>
         *   <li>(1.0, 0.0): shrink to nothing (dissipating smoke)</li>
         *   <li>(0.0, 1.0): grow from nothing (expanding explosion)</li>
         *   <li>(0.5, 1.5): grow over time (inflating bubbles)</li>
         * </ul>
         * Default: disabled (constant size)
         *
         * @param start size multiplier at birth (age 0.0)
         * @param end size multiplier at death (age 1.0)
         * @return this builder
         */
        public Builder sizeOverLifetime(float start, float end) {
            this.sizeStartScale = start;
            this.sizeEndScale = end;
            this.hasSizeOverLifetime = true;
            return this;
        }

        /**
         * Sets constant acceleration applied to particles (typically gravity).
         * <p>
         * Applied every frame as a force in world space. Use negative Y for downward
         * gravity, or any direction for wind, suction, or other effects.
         * <p>
         * Examples:
         * <ul>
         *   <li>(0, -9.8, 0): realistic Earth gravity</li>
         *   <li>(0, -2.0, 0): gentle falling (light debris, leaves)</li>
         *   <li>(0, 5.0, 0): upward force (heat rising, magic lift)</li>
         *   <li>(2, 0, 0): horizontal wind</li>
         * </ul>
         * Default: (0, 0, 0) - no gravity
         *
         * @param gravity acceleration vector in world units/sec²
         * @return this builder
         */
        public Builder gravity(Vector3f gravity) {
            this.gravity = gravity;
            return this;
        }

        /**
         * Sets constant acceleration applied to particles from component values.
         * <p>
         * Convenience overload of {@link #gravity(Vector3f)}.
         * <p>
         * Default: (0, 0, 0) - no gravity
         *
         * @param x X component of acceleration
         * @param y Y component of acceleration
         * @param z Z component of acceleration
         * @return this builder
         */
        public Builder gravity(float x, float y, float z) {
            this.gravity = new Vector3f(x, y, z);
            return this;
        }

        /**
         * Sets the drag coefficient that slows particles over time.
         * <p>
         * Drag reduces velocity each frame, creating air resistance. Higher values make
         * particles slow down faster.
         * <p>
         * Values:
         * <ul>
         *   <li>0.0: no drag, particles maintain speed (vacuum, magic projectiles)</li>
         *   <li>0.1-0.3: light drag (normal air, light particles)</li>
         *   <li>0.5-0.8: heavy drag (thick smoke, underwater)</li>
         *   <li>1.0: immediate stop (unusable, particles freeze instantly)</li>
         * </ul>
         * Default: 0.0 (no drag)
         *
         * @param drag drag coefficient (0-1)
         * @return this builder
         */
        public Builder drag(float drag) {
            this.drag = drag;
            return this;
        }

        /**
         * Sets the texture to render for each particle.
         * <p>
         * The texture path is relative to the assets directory. Use textures with soft
         * edges and pre-multiplied alpha for best blending results. If not set, particles
         * render as solid white quads.
         * <p>
         * Recommended formats: PNG with alpha channel.
         * <p>
         * Examples:
         * <ul>
         *   <li>"textures/particle_soft.png": soft round particle</li>
         *   <li>"textures/spark.png": elongated spark/streak</li>
         *   <li>"textures/smoke_wisp.png": irregular smoke puff</li>
         * </ul>
         * Default: null (solid white quad)
         *
         * @param path path to texture file
         * @return this builder
         */
        public Builder texture(String path) {
            this.texturePath = path;
            return this;
        }

        /**
         * Builds the immutable ParticleEffect configuration.
         *
         * @return a new ParticleEffect instance
         */
        public ParticleEffect build() {
            return new ParticleEffect(this);
        }
    }
}
