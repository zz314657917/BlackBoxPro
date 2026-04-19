plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("net.neoforged.moddev")
}

base {
    archivesName.set("BlackBoxPro-runtime-${property("minecraft_version")}")
}

repositories {
    maven("https://repo.tabooproject.org/repository/releases/")
    mavenCentral()
}

neoForge {
    neoFormVersion = "1.21.11-20251209.172050"
}

dependencies {
    api(project(":common"))
    implementation("org.tabooproject.reflex:reflex:1.2.3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}
