package org.chapzlock.core.scene;

import org.chapzlock.core.render.MaterialRenderer;
import org.chapzlock.core.render.MaterialRendererRegistry;
import org.chapzlock.core.render.ShaderSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaterialRendererRegistryDefaultsTest {

    @Test
    void registerDefaultAndRetrieveByMaterialType() {
        ShaderSystem shaderSystem = mock(ShaderSystem.class);
        MaterialRendererRegistry registry = new MaterialRendererRegistry(shaderSystem);
        MaterialRenderer renderer = mock(MaterialRenderer.class);

        registry.registerDefault(MaterialType.ENTITY, renderer);

        MaterialRenderer retrieved = registry.getDefaultRenderer(MaterialType.ENTITY);
        assertSame(renderer, retrieved);
    }

    @Test
    void getDefaultRendererReturnsNullForUnregisteredType() {
        ShaderSystem shaderSystem = mock(ShaderSystem.class);
        MaterialRendererRegistry registry = new MaterialRendererRegistry(shaderSystem);

        assertNull(registry.getDefaultRenderer(MaterialType.TERRAIN));
    }
}
