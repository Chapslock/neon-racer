package org.chapzlock.core.scene;

import org.chapzlock.core.event.Event;

/**
 * Published on the {@link org.chapzlock.core.event.EventBus} after a scene finishes loading.
 *
 * <p>At the time this event fires, all entities have been instantiated, GPU assets uploaded,
 * and scene systems initialized. Listeners can safely query the ComponentRegistry for
 * entities from the newly loaded scene.
 *
 * @param sceneName the name of the scene that was loaded
 * @see SceneManager
 */
public record SceneLoadedEvent(String sceneName) implements Event {}
