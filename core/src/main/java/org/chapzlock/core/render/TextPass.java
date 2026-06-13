package org.chapzlock.core.render;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.text.Font;
import org.chapzlock.core.text.FontRegistry;
import org.chapzlock.core.text.TextBounds;
import org.chapzlock.core.text.TextComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Render pass for text rendering using MSDF font atlases.
 *
 * <p>Executes at order 850 (after sprites, before debug UI). Batches text entities
 * by font, renders each batch via {@link TextRenderer}, and writes back computed
 * {@link TextBounds} to entities.
 *
 * <p>Query: {@code TextComponent + Transform}. Batching: by font path.
 */
public class TextPass implements RenderPass {

    private TextRenderer textRenderer;
    private FontRegistry fontRegistry;
    private ComponentRegistry registry;

    private final Map<String, List<Integer>> renderQueue = new HashMap<>();

    @Override
    public int order() {
        return 850;
    }

    @Override
    public void init(Engine engine) {
        this.registry = engine.getRegistry();
        this.fontRegistry = engine.getFontRegistry();
        this.textRenderer = new TextRenderer(
                engine.getShaderRegistry(),
                engine.getWindow().getSpecification());
        this.textRenderer.init();
    }

    @Override
    public void collectVisibles(RenderContext ctx) {
        renderQueue.clear();
        ctx.getRegistry().forEach(TextComponent.class, Transform.class,
                (entityId, text, transform) -> {
                    renderQueue.computeIfAbsent(text.getFont(), f -> new ArrayList<>()).add(entityId);
                });
    }

    @Override
    public void execute(RenderContext ctx) {
        for (var entry : renderQueue.entrySet()) {
            String fontPath = entry.getKey();
            List<Integer> entityIds = entry.getValue();

            Font font = fontRegistry.load(fontPath);
            textRenderer.beginBatch(font);

            for (int entityId : entityIds) {
                TextComponent text = registry.getComponent(entityId, TextComponent.class);
                Transform transform = registry.getComponent(entityId, Transform.class);
                textRenderer.renderEntity(entityId, text, transform, font);

                // Write back TextBounds
                TextBounds bounds = textRenderer.getCachedBounds(entityId);
                if (bounds != null) {
                    registry.addComponent(entityId, bounds);
                }
            }

            textRenderer.endBatch();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (textRenderer != null) {
            textRenderer.resize(width, height);
        }
    }

    @Override
    public void dispose() {
        if (textRenderer != null) {
            textRenderer.dispose();
        }
    }
}
