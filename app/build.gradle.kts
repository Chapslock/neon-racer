import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.jvm.Jvm
import java.util.*

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass = "org.chapzlock.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("neon-racer")
    archiveClassifier.set("")
    archiveVersion.set("$version")
}

val os =
    org.gradle.internal.os.OperatingSystem
        .current()
val appName = "neon-racer"
val mainClass = "org.chapzlock.Main"
val jarName = "neon-racer-$version.jar"
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
        else -> detectLinuxInstallerType()
    }
val osName =
    when {
        os.isWindows -> "windows"
        os.isMacOsX -> "mac"
        else -> "linux"
    }
val appImageName = "$appName-${project.version}-$osName"
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
        "--verbose",
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
        "--verbose",
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
    val appImageDir =
        if (os.isMacOsX) {
            file("$outputDir/$appImageName.app")
        } else {
            file("$outputDir/$appImageName")
        }

    from(appImageDir)
    archiveFileName.set("$appName-${project.version}-$osName-app-image.zip")
    destinationDirectory.set(file(outputDir))
}

// --- Convenience Task ---
tasks.register("jPackageAll") {
    dependsOn("jPackageAppImage", "jPackageInstaller", "jPackageArchiveAppImage")
}

fun detectLinuxInstallerType(): String {
    val osRelease = file("/etc/os-release")
    if (osRelease.exists()) {
        val props = Properties()
        osRelease.forEachLine { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                props[parts[0]] = parts[1].trim('"')
            }
        }
        val id = (props["ID"] as? String)?.lowercase() ?: ""
        return when {
            id.contains("fedora") || id.contains("rhel") || id.contains("centos") -> "rpm"
            id.contains("debian") || id.contains("ubuntu") -> "deb"
            else -> "deb"
        }
    }
    return "deb"
}
