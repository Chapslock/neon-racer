package org.chapzlock.core.scene;

import org.chapzlock.core.event.Event;

/**
 * Published by {@link SceneManager} when a scene finishes loading.
 *
 * @param sceneName the name of the loaded scene
 */
public record SceneLoadedEvent(String sceneName) implements Event {}
