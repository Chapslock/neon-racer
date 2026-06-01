package org.chapzlock.core.component;

/**
 * Controls how a point light's intensity decreases with distance from the source.
 *
 * <p>Uses the standard attenuation formula:
 * {@code intensity = 1.0 / (constant + linear * d + quadratic * d^2)}
 * where {@code d} is the distance from the light to the surface point.</p>
 *
 * <p>The three coefficients control the falloff curve:
 * <ul>
 *   <li><b>constant</b> — base divisor, prevents division by zero. Always 1.0.</li>
 *   <li><b>linear</b> — makes light dim proportionally to distance. Higher = faster falloff.
 *       Dominates at medium distances.</li>
 *   <li><b>quadratic</b> — makes light dim proportionally to distance squared. Higher = faster falloff.
 *       Dominates at large distances, producing a natural "soft edge" to the light's reach.</li>
 * </ul>
 *
 * <p>Presets provide common configurations:
 * <ul>
 *   <li>{@link #DEFAULT} — ~200 unit effective range (outdoor scenes, fill lights)</li>
 *   <li>{@link #SHORT_RANGE} — ~7 unit effective range (torches, candles, dashboard lights)</li>
 *   <li>{@link #LONG_RANGE} — ~600 unit effective range (sun approximation, stadium lights)</li>
 * </ul>
 */
public record Attenuation(float constant, float linear, float quadratic) {

    /** ~200 unit effective range. Suitable for general outdoor/fill lighting. */
    public static final Attenuation DEFAULT = new Attenuation(1.0f, 0.014f, 0.0007f);

    /** ~7 unit effective range. Suitable for torches, candles, small local lights. */
    public static final Attenuation SHORT_RANGE = new Attenuation(1.0f, 0.7f, 1.8f);

    /** ~600 unit effective range. Suitable for large area lights, stadium floods. */
    public static final Attenuation LONG_RANGE = new Attenuation(1.0f, 0.007f, 0.0002f);
}
