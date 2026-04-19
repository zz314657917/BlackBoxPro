import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

plugins {
    id("fabric-loom")
    id("org.jetbrains.kotlin.jvm")
}

evaluationDependsOn(":1.21.11:runtime")

val commonSourceSet = project(":common").the<SourceSetContainer>()["main"]
val runtimeSharedDir = project(":1.21.11:runtime").file("src/main/kotlin/com/blackboxpro/runtime")

the<SourceSetContainer>()["main"].java.srcDir(runtimeSharedDir)

base {
    archivesName.set("BlackBoxPro-fabric-${property("minecraft_version")}")
}

loom {
    accessWidenerPath.set(file("src/main/resources/blackboxpro.accesswidener"))
}

repositories {
    maven("https://repo.tabooproject.org/repository/releases/")
    mavenCentral()
}

val shadeReflex: Configuration by configurations.creating

dependencies {
    implementation(project(":common"))
    implementation("org.tabooproject.reflex:reflex:1.2.3")
    shadeReflex("org.tabooproject.reflex:reflex:1.2.3")
    shadeReflex("org.tabooproject.reflex:analyser:1.2.3")

    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}

tasks.named<Jar>("jar") {
    from(commonSourceSet.output)
    from({ shadeReflex.files.map { zipTree(it) } }) {
        exclude("META-INF/**")
        exclude("org/objectweb/asm/**")
        exclude("org/apache/commons/**")
        exclude("kotlin/**")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Jar>("sourcesJar") {
    from(commonSourceSet.allSource)
}
