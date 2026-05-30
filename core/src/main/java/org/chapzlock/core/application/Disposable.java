package org.chapzlock.core.application;

/**
 * Interface for objects that require cleanup.
 *
 * <p>GPU systems (MeshSystem, TextureSystem, ShaderSystem) implement this to free OpenGL resources.
 * {@link EngineContext#dispose()} cascades to all disposable subsystems on shutdown.
 */
public interface Disposable {
    /**
     * Frees resources held by this object. Called at shutdown.
     */
    void dispose();
}
