import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.neoforged.moddev")
    id("org.jetbrains.kotlin.jvm")
}

evaluationDependsOn(":common")
evaluationDependsOn(":1.21.11:runtime")

val commonSourceSet = project(":common").extensions.getByType(SourceSetContainer::class.java).getByName("main")
val runtimeSourceSet = project(":1.21.11:runtime").extensions.getByType(SourceSetContainer::class.java).getByName("main")
val localSourceSet = extensions.getByType(SourceSetContainer::class.java).getByName("main")

base {
    archivesName.set("BlackBoxPro-neoforge-${property("minecraft_version")}")
}

repositories {
    maven("https://repo.tabooproject.org/repository/releases/")
    mavenCentral()
    maven { setUrl("https://thedarkcolour.github.io/KotlinForForge/") }
}

neoForge {
    version = property("neoforge_version").toString()

    runs {
        configureEach {
            val blackboxpro = mods.create("blackboxpro")
            blackboxpro.sourceSet(localSourceSet)
            blackboxpro.sourceSet(runtimeSourceSet)
            blackboxpro.sourceSet(commonSourceSet)
        }
    }
}

val shadeReflex: Configuration by configurations.creating

dependencies {
    implementation(project(":common"))
    implementation(project(":1.21.11:runtime"))
    implementation("org.tabooproject.reflex:reflex:1.2.3")
    shadeReflex("org.tabooproject.reflex:reflex:1.2.3")
    shadeReflex("org.tabooproject.reflex:analyser:1.2.3")
    implementation("thedarkcolour:kotlinforforge-neoforge:${property("kotlin_for_forge_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }

    from(runtimeSourceSet.resources)
    from(commonSourceSet.resources)
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
    from(runtimeSourceSet.output)
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
    from(runtimeSourceSet.allSource)
    from(commonSourceSet.allSource)
}
