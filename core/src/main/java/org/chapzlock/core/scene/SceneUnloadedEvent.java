package org.chapzlock.core.scene;

import org.chapzlock.core.event.Event;

/**
 * Published on the {@link org.chapzlock.core.event.EventBus} after a scene finishes unloading.
 *
 * <p>At the time this event fires, all entities from the scene have been destroyed,
 * scene systems removed from the scheduler, and GPU asset references released.
 *
 * @param sceneName the name of the scene that was unloaded
 * @see SceneManager
 */
public record SceneUnloadedEvent(String sceneName) implements Event {}
