package org.chapzlock.core.physics;

import javax.vecmath.Vector3f;

import lombok.experimental.UtilityClass;

/**
 * Utility class for mapping between objects from vecmath library and joml library
 */
@UtilityClass
public class PhysicsMapper {

    public static Vector3f vector3f(org.joml.Vector3f vector3f) {
        return new Vector3f(vector3f.x, vector3f.y, vector3f.z);
    }
}
