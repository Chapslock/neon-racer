package org.chapzlock;

import org.chapzlock.app.scene.ForestScene;
import org.chapzlock.core.application.Application;
import org.chapzlock.core.application.ApplicationSpecification;
import org.chapzlock.core.logging.LogEngine;
import org.chapzlock.core.logging.LogLevel;
import org.chapzlock.core.physics.PhysicsWorldConfig;
import org.chapzlock.core.window.WindowSpecification;

public class Main {
    public static void main(String[] args) {
        LogEngine.setGlobalLevel(LogLevel.DEBUG);
        Application app = Application.create(
            ApplicationSpecification.builder()
                .windowSpec(WindowSpecification.builder()
                    .title("Neon Racer")
                    .width(1280)
                    .height(720)
                    .build())
                .physicsConfig(PhysicsWorldConfig.withDebug())
                .initialScene(ForestScene.create())
                .build());
        app.run();
    }
}
