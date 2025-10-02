import org.gradle.internal.jvm.Jvm
import java.util.Locale

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass = "org.chapzlock.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

group = "org.chapzlock"
version = "1.0"

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.6"
val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
val lwjglNatives =
    when {
        osName.contains("win") -> "natives-windows"
        osName.contains("mac") -> "natives-macos"
        else -> "natives-linux"
    }

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-assimp")
    implementation("org.lwjgl", "lwjgl-bgfx")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-nanovg")
    implementation("org.lwjgl", "lwjgl-nuklear")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-par")
    implementation("org.lwjgl", "lwjgl-stb")
    implementation("org.lwjgl", "lwjgl-vulkan")
    implementation("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-bgfx", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-nanovg", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-nuklear", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-par", classifier = lwjglNatives)
    implementation("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Exec>("jpackageApp") {
    dependsOn(tasks.shadowJar)

    val jpackageExecutable =
        Jvm
            .current()
            .javaHome
            .resolve("bin")
            .resolve("jpackage")
            .absolutePath
    val appName = "NeonRacer"
    val mainClass = "org.chapzlock.Main"
    val jarName = "neon-racer-$version-all.jar"
    val inputDir =
        layout.buildDirectory
            .dir("libs")
            .get()
            .asFile.absolutePath
    val outputDir =
        layout.buildDirectory
            .dir("jpackage")
            .get()
            .asFile.absolutePath
    val appDir = file("$outputDir/$appName")

    doFirst {
        if (appDir.exists()) {
            appDir.deleteRecursively()
        }
        mkdir(outputDir)
    }

    var installerType = "app-image"
    /*
    val os =
        org.gradle.internal.os.OperatingSystem
            .current()
    if (os.isWindows) {
        installerType = "exe"
    }
    if (os.isMacOsX) {
        installerType = "dmg"
    }
    if (os.isLinux) {
        installerType = "deb"
    }
     */

    commandLine(
        jpackageExecutable,
        "--name",
        appName,
        "--input",
        inputDir,
        "--main-jar",
        jarName,
        "--main-class",
        mainClass,
        "--type",
        installerType,
        "--dest",
        outputDir,
    )
    val jvmArgs = application.applicationDefaultJvmArgs
    jvmArgs.forEach { arg ->
        commandLine(commandLine + listOf("--java-options", arg))
    }
    /*
    if (os.isWindows) {
        commandLine(commandLine + listOf("--win-shortcut", "--win-menu"))
    }
     */
}
