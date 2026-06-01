package org.chapzlock.core.asset.gltf;

/**
 * Thrown when a glTF/GLB file cannot be loaded or parsed.
 *
 * <p>This exception wraps any lower-level I/O or parsing errors that occur during
 * glTF asset loading. It provides a consistent error type for callers to catch,
 * regardless of whether the failure was due to a missing file, invalid binary format,
 * unsupported glTF feature, or corrupted data.
 *
 * <p><strong>Usage:</strong> You typically don't need to catch this directly — the
 * scene loading system will log the error and skip the failed asset. But if you're
 * writing custom asset loading code, you can catch it to provide fallback behavior.
 *
 * @see GltfLoader
 */
public class GltfLoadException extends RuntimeException {

    /**
     * Creates a new GltfLoadException with a descriptive message and the underlying cause.
     *
     * @param message human-readable description of what went wrong (e.g., "Failed to load glTF asset: car.glb")
     * @param cause   the underlying exception that caused the failure (may be null)
     */
    public GltfLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
