package org.chapzlock.core.scene;

import org.chapzlock.core.render.MaterialRenderer;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaterialSystemDefaultsTest {

    @Test
    void registerDefaultAndRetrieveByMaterialType() {
        ShaderSystem shaderSystem = mock(ShaderSystem.class);
        MaterialSystem system = new MaterialSystem(shaderSystem);
        MaterialRenderer renderer = mock(MaterialRenderer.class);

        system.registerDefault(MaterialType.PBR, renderer);

        MaterialRenderer retrieved = system.getDefaultRenderer(MaterialType.PBR);
        assertSame(renderer, retrieved);
    }

    @Test
    void getDefaultRendererReturnsNullForUnregisteredType() {
        ShaderSystem shaderSystem = mock(ShaderSystem.class);
        MaterialSystem system = new MaterialSystem(shaderSystem);

        assertNull(system.getDefaultRenderer(MaterialType.PBR));
    }
}
