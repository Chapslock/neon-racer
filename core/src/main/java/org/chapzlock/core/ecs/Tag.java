package org.chapzlock.core.ecs;

/**
 * Marker interface for empty components used in queries.
 *
 * <p>Tags have no data fields — they exist only to mark entities for filtering.
 * Example: {@code class Enemy implements Tag {}} allows querying {@code view(Enemy.class)}.
 */
public interface Tag extends Component {
}
