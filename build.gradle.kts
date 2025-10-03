import org.gradle.internal.jvm.Jvm

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
val os =
    org.gradle.internal.os.OperatingSystem
        .current()
val lwjglNatives =
    when {
        os.isWindows -> "natives-windows"
        os.isMacOsX -> "natives-macos"
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

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

val appName = "NeonRacer"
val mainClass = "org.chapzlock.Main"
val jarName = "neon-racer-$version-all.jar"
val inputDir: String =
    layout.buildDirectory
        .dir("libs")
        .get()
        .asFile.absolutePath
val outputDir: String =
    layout.buildDirectory
        .dir("jpackage")
        .get()
        .asFile.absolutePath

val installerType =
    when {
        os.isWindows -> "exe"
        os.isMacOsX -> "dmg"
        os.isLinux -> "deb"
        else -> "app-image"
    }

val appImageName = "$appName-${project.version}-${os.name}"
val appImageDir = file("$outputDir/$appImageName")

val jPackageExecutable: String =
    Jvm
        .current()
        .javaHome
        .resolve("bin")
        .resolve("jpackage")
        .absolutePath

// --- App Image Task ---
tasks.register<Exec>("jPackageAppImage") {
    dependsOn(tasks.shadowJar)

    doFirst {
        if (appImageDir.exists()) {
            appImageDir.deleteRecursively()
        }
        mkdir(outputDir)
    }

    commandLine(
        jPackageExecutable,
        "--name",
        appImageName,
        "--input",
        inputDir,
        "--main-jar",
        jarName,
        "--main-class",
        mainClass,
        "--type",
        "app-image",
        "--dest",
        outputDir,
    )

    application.applicationDefaultJvmArgs.forEach { arg ->
        commandLine(commandLine + listOf("--java-options", arg))
    }
}

// --- Installer Task ---
tasks.register<Exec>("jPackageInstaller") {
    dependsOn(tasks.shadowJar, "jPackageAppImage")

    commandLine(
        jPackageExecutable,
        "--name",
        appImageName,
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

    application.applicationDefaultJvmArgs.forEach { arg ->
        commandLine(commandLine + listOf("--java-options", arg))
    }

    if (os.isWindows) {
        commandLine(commandLine + listOf("--win-shortcut", "--win-menu"))
    }
}

// --- Zip App Image ---
tasks.register<Zip>("jPackageArchiveAppImage") {
    dependsOn("jPackageAppImage")
    from(appImageDir)
    archiveFileName.set("$appName-${project.version}-${os.name}-app-image.zip")
    destinationDirectory.set(file(outputDir))
}

// --- Convenience Task ---
tasks.register("jPackageAll") {
    dependsOn("jPackageAppImage", "jPackageInstaller", "jPackageArchiveAppImage")
}
