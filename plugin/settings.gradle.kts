pluginManagement {
    repositories {
        maven(url = uri("$rootDir/.gradle-local-repo"))
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        mavenCentral()
        maven("https://repo.tabooproject.org/repository/releases/")
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.izzel.taboolib") {
                useModule("io.izzel.taboolib:taboolib-gradle-plugin:${requested.version}")
            }
        }
    }
}

includeBuild("../common")

rootProject.name = "BlackBoxPro-Plugin"
