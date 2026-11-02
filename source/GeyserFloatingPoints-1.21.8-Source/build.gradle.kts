plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "oxy.geyser.fp"
version = "2.0.0-1.21.8-compat.2"

val geyserCoreVersion = providers.gradleProperty("geyserCoreVersion")
    .orElse("2.12.1-SNAPSHOT")
val geyserRuntimeJar = providers.gradleProperty("geyserRuntimeJar").orNull

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

dependencies {
    // A local runtime JAR makes the build match a concrete deployed Geyser build.
    if (geyserRuntimeJar != null) {
        compileOnly(files(geyserRuntimeJar))
        // The Velocity distribution relocates Geyser's core but deliberately
        // leaves these API dependencies external. Keep them explicit so the
        // compiler checks our code against the supplied runtime JAR.
        compileOnly("io.netty:netty-buffer:4.1.118.Final")
        compileOnly("org.checkerframework:checker-qual:3.48.3")
        compileOnly("net.kyori:adventure-api:4.18.0")
    } else {
        compileOnly("org.geysermc.geyser:core:${geyserCoreVersion.get()}") {
            exclude(group = "com.google.code.gson", module = "gson")
        }
    }

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("org.cloudburstmc.fastutil.commons:long-common:8.5.15")
    implementation("org.cloudburstmc.fastutil.maps:long-object-maps:8.5.15")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.test { useJUnitPlatform() }

tasks.shadowJar {
    archiveFileName.set("GeyserFloatingPoints-1.21.8-compat.jar")
    relocate("org.yaml.snakeyaml", "oxy.geyser.fp.shaded.snakeyaml")
    relocate("com.fasterxml", "oxy.geyser.fp.shaded.fasterxml")
    relocate("it.unimi.dsi.fastutil", "oxy.geyser.fp.shaded.fastutil")
}

tasks.build { dependsOn(tasks.shadowJar) }

// The built JAR is a Geyser extension. It is NOT a Fabric mod or backend plugin.
// Minecraft 1.21.8 here means a backend running 1.21.8 behind a Geyser/Velocity proxy.
