package org.chapzlock.core.component;

import org.chapzlock.core.math.Vector3f;

import lombok.Getter;

@Getter
public class CameraComponent implements Component {
    private Vector3f position = new Vector3f(0,0,0);
    /**
     * Camera rotation in degrees (x = pitch, y = yaw, z = roll)
     */
    private Vector3f rotation = new Vector3f(0,0,0);
}
