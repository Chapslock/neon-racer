package org.chapzlock.core.event;

import org.chapzlock.core.application.Component;

public record ComponentRemovedEvent(int entityId, Class<? extends Component> componentType) implements Event {
}
