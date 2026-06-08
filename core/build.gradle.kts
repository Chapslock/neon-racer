plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.6"
val os =
    org.gradle.internal.os.OperatingSystem
        .current()
val lwjglNatives =
    when {
        os.isWindows -> "natives-windows"
        os.isMacOsX -> "natives-macos"
        else -> "natives-linux"
    }

val lombokVersion = "1.18.42"
val fastUtilVersion = "5.0.9"
val jBulletVersion = "20101010-1"
val jomlVersion = "1.10.8"
val jgltfVersion = "2.0.4"
dependencies {
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    implementation("org.projectlombok:lombok:$lombokVersion")
    api("cz.advel.jbullet:jbullet:$jBulletVersion")
    api("de.javagl:jgltf-model:$jgltfVersion")

    api("fastutil:fastutil:$fastUtilVersion")
    api("org.joml:joml:$jomlVersion")
    api(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    api("org.lwjgl", "lwjgl")
    api("org.lwjgl", "lwjgl-glfw")
    api("org.lwjgl", "lwjgl-nanovg")
    api("org.lwjgl", "lwjgl-nuklear")
    api("org.lwjgl", "lwjgl-openal")
    api("org.lwjgl", "lwjgl-opengl")
    api("org.lwjgl", "lwjgl-par")
    api("org.lwjgl", "lwjgl-stb")
    api("org.lwjgl", "lwjgl-msdfgen")
    api("org.lwjgl", "lwjgl-freetype")
    api("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    api("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    api("org.lwjgl", "lwjgl-nanovg", classifier = lwjglNatives)
    api("org.lwjgl", "lwjgl-nuklear", classifier = lwjglNatives)
    api("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    api("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    api("org.lwjgl", "lwjgl-par", classifier = lwjglNatives)
    api("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
    api("org.lwjgl", "lwjgl-msdfgen", classifier = lwjglNatives)
    api("org.lwjgl", "lwjgl-freetype", classifier = lwjglNatives)

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
