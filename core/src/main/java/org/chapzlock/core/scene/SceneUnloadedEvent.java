package org.chapzlock.core.scene;

import org.chapzlock.core.event.Event;

public record SceneUnloadedEvent(String sceneName) implements Event {}
