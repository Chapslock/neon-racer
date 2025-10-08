package org.chapzlock.core.component;

import org.chapzlock.core.math.Vector3f;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransformComponent implements Component{
    private Vector3f position = new Vector3f(0, 0,0);
    private Vector3f rotation = new Vector3f(0, 0, 0);
    private float scale = 1;

    public TransformComponent() {
    }

    public TransformComponent(Vector3f position) {
        this.position = position;
    }

    public TransformComponent(Vector3f position, Vector3f rotation) {
        this.position = position;
        this.rotation = rotation;
    }

    public TransformComponent(Vector3f position, Vector3f rotation, float scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }
}
