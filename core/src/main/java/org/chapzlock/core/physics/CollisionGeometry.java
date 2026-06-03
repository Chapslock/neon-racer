package org.chapzlock.core.physics;

import java.util.List;

import org.joml.Vector3f;

/**
 * Describes the shape of a collision body without creating any physics engine objects.
 *
 * <h3>Why this exists</h3>
 * <p>Physics engines need a mathematical shape to detect collisions — the "collision geometry."
 * This is separate from the visual mesh because collision shapes must be much simpler for
 * performance. A 10,000 polygon character model might use a simple capsule (cylinder with
 * rounded ends) for collision.</p>
 *
 * <h3>Shape types explained</h3>
 * <ul>
 *   <li><strong>Box</strong> — axis-aligned rectangular solid. Cheapest to compute. Good for
 *       crates, walls, platforms.</li>
 *   <li><strong>Sphere</strong> — perfect sphere. Extremely fast collision checks. Good for
 *       balls, simple projectiles, proximity triggers.</li>
 *   <li><strong>Capsule</strong> — cylinder with hemispherical caps. The standard shape for
 *       characters/players in most games because it slides smoothly over terrain and doesn't
 *       catch on edges.</li>
 *   <li><strong>ConvexHull</strong> — shrink-wrap around a mesh's vertices. More precise than
 *       primitives but more expensive. Cannot represent concave (hollow/indented) shapes.
 *       The {@code maxVertices} parameter controls precision vs performance.</li>
 *   <li><strong>TriangleMesh</strong> — exact triangle-by-triangle collision. Most precise but
 *       most expensive. <strong>Static bodies only</strong> (terrain, architecture). Moving a
 *       triangle mesh body causes serious performance issues in the physics engine.</li>
 *   <li><strong>Compound</strong> — multiple shapes combined into one body. Used for complex
 *       objects (e.g., an L-shaped piece, a character with separate head/body collision).</li>
 * </ul>
 *
 * <h3>Performance guidance</h3>
 * <p>From cheapest to most expensive: Sphere &lt; Capsule &lt; Box &lt; ConvexHull &lt; TriangleMesh.
 * Always use the simplest shape that gives acceptable collision behavior. Players won't notice
 * that a barrel uses a cylinder instead of matching every polygon.</p>
 *
 * @see PhysicsBody
 * @see org.chapzlock.core.component.PhysicsDescriptor
 */
public sealed interface CollisionGeometry {

    record Box(Vector3f halfExtents) implements CollisionGeometry {}
    record Sphere(float radius) implements CollisionGeometry {}
    record Capsule(float radius, float height) implements CollisionGeometry {}
    record ConvexHull(int maxVertices) implements CollisionGeometry {}
    record TriangleMesh() implements CollisionGeometry {}
    record Compound(List<Positioned> children) implements CollisionGeometry {}
    record Offset(CollisionGeometry inner, Vector3f offset) implements CollisionGeometry {}
    record Positioned(CollisionGeometry shape, Vector3f offset) {}

    default CollisionGeometry offset(float x, float y, float z) {
        CollisionGeometry base = this instanceof Offset o ? o.inner() : this;
        return new Offset(base, new Vector3f(x, y, z));
    }

    static CollisionGeometry box(float hx, float hy, float hz) {
        return new Box(new Vector3f(hx, hy, hz));
    }

    static CollisionGeometry box(Vector3f halfExtents) {
        return new Box(new Vector3f(halfExtents));
    }

    static CollisionGeometry sphere(float radius) {
        if (radius <= 0) throw new IllegalArgumentException("Sphere radius must be positive, got: " + radius);
        return new Sphere(radius);
    }

    static CollisionGeometry capsule(float radius, float height) {
        if (radius <= 0) throw new IllegalArgumentException("Capsule radius must be positive, got: " + radius);
        if (height <= 0) throw new IllegalArgumentException("Capsule height must be positive, got: " + height);
        return new Capsule(radius, height);
    }

    static CollisionGeometry convexHull() {
        return new ConvexHull(256);
    }

    static CollisionGeometry convexHull(int maxVertices) {
        if (maxVertices <= 0) throw new IllegalArgumentException("maxVertices must be positive, got: " + maxVertices);
        return new ConvexHull(maxVertices);
    }

    static CollisionGeometry triangleMesh() {
        return new TriangleMesh();
    }

    static CollisionGeometry compound(Positioned... children) {
        return new Compound(List.of(children));
    }
}
