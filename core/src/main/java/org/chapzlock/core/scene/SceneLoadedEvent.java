package org.chapzlock.core.scene;

import org.chapzlock.core.event.Event;

public record SceneLoadedEvent(String sceneName) implements Event {}
