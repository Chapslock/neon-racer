package org.chapzlock.core.scene;

import org.chapzlock.core.event.Event;

/**
 * Published by {@link SceneManager} when a scene finishes unloading.
 *
 * @param sceneName the name of the unloaded scene
 */
public record SceneUnloadedEvent(String sceneName) implements Event {}
