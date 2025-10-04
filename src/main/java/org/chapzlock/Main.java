package org.chapzlock;

import org.chapzlock.application.AppLayer;
import org.chapzlock.application.TextureLayer;
import org.chapzlock.core.application.Application;
import org.chapzlock.core.application.ApplicationSpecification;
import org.chapzlock.core.window.WindowSpecification;

public class Main {

    public static void main(String[] args) {
        var appSpec = ApplicationSpecification.builder()
            .windowSpec(WindowSpecification.builder()
                .build())
            .build();
        Application application = new Application(appSpec);
        application.pushLayer(new TextureLayer());
        application.pushLayer(new AppLayer());
        application.run();
    }

}
