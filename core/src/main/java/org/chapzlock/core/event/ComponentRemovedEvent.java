package org.chapzlock.core.event;

import org.chapzlock.core.ecs.Component;

public record ComponentRemovedEvent(int entityId, Class<? extends Component> componentType) implements Event {
}
