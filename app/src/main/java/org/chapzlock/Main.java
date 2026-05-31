package org.chapzlock;

import java.util.List;

import org.chapzlock.app.scene.RaceTrackScene;
import org.chapzlock.core.animation.AnimationSystem;
import org.chapzlock.core.application.Application;
import org.chapzlock.core.application.ApplicationSpecification;
import org.chapzlock.core.logging.Log;
import org.chapzlock.core.physics.PhysicsSystem;
import org.chapzlock.core.physics.PhysicsSystemSpecs;
import org.chapzlock.core.render.RenderSystem;
import org.chapzlock.core.render.TransformSystem;
import org.chapzlock.core.tween.TweenSystem;
import org.chapzlock.core.window.WindowSpecification;

public class Main {
    public static void main(String[] args) {
        Log.setDebugEnabled(true);
        Application app = Application.create(
            ApplicationSpecification.builder()
                .windowSpec(WindowSpecification.builder()
                    .title("Neon Racer")
                    .width(1280)
                    .height(720)
                    .build())
                .engineSystems(List.of(
                    new AnimationSystem(),
                    new TweenSystem(),
                    new TransformSystem(),
                    new PhysicsSystem(PhysicsSystemSpecs.builder()
                        .isDebugEnabled(true)
                        .build()),
                    new RenderSystem()))
                .initialScene(RaceTrackScene.create())
                .build());
        app.run();
    }
}
