package org.chapzlock.core.event;

import org.chapzlock.core.application.Component;

public record ComponentAddedEvent(int entityId, Class<? extends Component> componentType) implements Event {
}
