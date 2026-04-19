pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "GTNH Maven"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// 1.12.2 只依赖顶层预构建的 common jar，避免与 common 源码构建形成跨版本 Gradle 组合构建。
rootProject.name = "BlackBoxPro-1122"

include("runtime", "forge")
