package org.chapzlock.core.render;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.*;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderContextTest {

    private ComponentRegistry registry;
    private RenderContext ctx;

    @BeforeEach
    void setUp() {
        EngineContext engineCtx = EngineContext.testContext();
        registry = engineCtx.getRegistry();
        ctx = new RenderContext(engineCtx);
    }

    private int createEntityWith(org.chapzlock.core.ecs.Component... components) {
        int id = Entity.create(registry);
        for (var c : components) {
            registry.addComponent(id, c);
        }
        return id;
    }

    @Test
    void beginFrameReturnsFalseWithoutCamera() {
        assertFalse(ctx.beginFrame());
    }

    @Test
    void beginFrameCollectsDirectionalLight() {
        createEntityWith(new Camera(), Transform.at(0, 0, 0));
        createEntityWith(DirectionalLight.builder().direction(new Vector3f(0, -1, 0)).build());

        ctx.beginFrame();

        assertNotNull(ctx.getDirectionalLight());
        assertEquals(new Vector3f(0, -1, 0), ctx.getDirectionalLight().getDirection());
    }

    @Test
    void beginFrameDirectionalLightNullWhenAbsent() {
        createEntityWith(new Camera(), Transform.at(0, 0, 0));

        ctx.beginFrame();

        assertNull(ctx.getDirectionalLight());
    }

    @Test
    void beginFrameCollectsMultiplePointLights() {
        createEntityWith(new Camera(), Transform.at(0, 0, 0));
        createEntityWith(PointLight.builder().build(), Transform.at(1, 0, 0));
        createEntityWith(PointLight.builder().build(), Transform.at(2, 0, 0));
        createEntityWith(PointLight.builder().build(), Transform.at(3, 0, 0));

        ctx.beginFrame();

        assertEquals(3, ctx.getPointLights().size());
    }

    @Test
    void beginFrameReadsPointLightPositionFromTransform() {
        createEntityWith(new Camera(), Transform.at(0, 0, 0));
        createEntityWith(PointLight.builder().build(), Transform.at(5, 10, 15));

        ctx.beginFrame();

        assertEquals(1, ctx.getPointLights().size());
        assertEquals(new Vector3f(5, 10, 15), ctx.getPointLights().get(0).position());
    }

    @Test
    void beginFrameCapsPointLightsAtMaxCount() {
        createEntityWith(new Camera(), Transform.at(0, 0, 0));
        for (int i = 0; i < 10; i++) {
            createEntityWith(PointLight.builder().build(), Transform.at(i, 0, 0));
        }

        ctx.beginFrame();

        assertEquals(PointLight.MAX_COUNT, ctx.getPointLights().size());
    }

    @Test
    void beginFrameFogPresentReturnsFogComponent() {
        createEntityWith(new Camera(), Transform.at(0, 0, 0));
        createEntityWith(Fog.builder().density(0.01f).gradient(2.0f).build());

        ctx.beginFrame();

        assertNotNull(ctx.getFog());
        assertTrue(ctx.isFogEnabled());
        assertEquals(0.01f, ctx.getFog().getDensity());
        assertEquals(2.0f, ctx.getFog().getGradient());
    }

    @Test
    void beginFrameFogAbsentReturnsNull() {
        createEntityWith(new Camera(), Transform.at(0, 0, 0));

        ctx.beginFrame();

        assertNull(ctx.getFog());
        assertFalse(ctx.isFogEnabled());
    }

    @Test
    void beginFrameAmbientPresentReturnsStrength() {
        createEntityWith(new Camera(), Transform.at(0, 0, 0));
        createEntityWith(AmbientLight.builder().strength(0.4f).build());

        ctx.beginFrame();

        assertNotNull(ctx.getAmbientLight());
        assertEquals(0.4f, ctx.getEffectiveAmbientStrength());
    }

    @Test
    void beginFrameAmbientAbsentFallsBackToDefault() {
        createEntityWith(new Camera(), Transform.at(0, 0, 0));

        ctx.beginFrame();

        assertNull(ctx.getAmbientLight());
        assertEquals(AmbientLight.DEFAULT_STRENGTH, ctx.getEffectiveAmbientStrength());
    }

    @Test
    void beginFrameExtractsCameraPosition() {
        createEntityWith(new Camera(), Transform.at(5, 10, 15));

        ctx.beginFrame();

        assertEquals(new Vector3f(5, 10, 15), ctx.getCameraPosition());
    }
}
