package org.chapzlock.core.application;

import org.chapzlock.core.window.WindowSpecification;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ApplicationSpecification {
    @Builder.Default
    private String name = "Application";
    private WindowSpecification windowSpec;
}
