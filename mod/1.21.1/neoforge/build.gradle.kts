import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import java.util.Properties

plugins {
    id("net.neoforged.moddev")
    id("org.jetbrains.kotlin.jvm")
}

val localProps = Properties().apply {
    projectDir.parentFile.resolve("gradle.properties").reader().use(::load)
}
fun localProp(key: String) = localProps.getProperty(key)
    ?: error("Missing property '$key' in mod/1.21.1/gradle.properties")

evaluationDependsOn(":common")
evaluationDependsOn(":1.21.1:runtime")

val commonSourceSet = project(":common").extensions.getByType(SourceSetContainer::class.java).getByName("main")
val runtimeSourceSet = project(":1.21.1:runtime").extensions.getByType(SourceSetContainer::class.java).getByName("main")
val localSourceSet = extensions.getByType(SourceSetContainer::class.java).getByName("main")

base {
    archivesName.set("BlackBoxPro-neoforge-${localProp("minecraft_version")}")
}

repositories {
    mavenCentral()
    maven("https://repo.tabooproject.org/repository/releases/")
    maven { setUrl("https://thedarkcolour.github.io/KotlinForForge/") }
}

neoForge {
    version = localProp("neoforge_version")

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
    implementation(project(":1.21.1:runtime"))
    implementation("thedarkcolour:kotlinforforge-neoforge:${localProp("kotlin_for_forge_version")}")
    implementation("org.tabooproject.reflex:reflex:1.2.3")
    shadeReflex("org.tabooproject.reflex:reflex:1.2.3")
    shadeReflex("org.tabooproject.reflex:analyser:1.2.3")
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
    // 1.21.1 的 runtime 工程已经内嵌了 common 源码/输出，这里避免再次打入导致重复条目
    from(runtimeSourceSet.output)
    from({ shadeReflex.files.map { zipTree(it) } }) {
        exclude("META-INF/**")
        exclude("org/objectweb/asm/**")
        exclude("org/apache/commons/**")
        exclude("kotlin/**")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Jar>("sourcesJar") {
    // 同上：避免 sourcesJar 中出现 duplicate entry
    from(runtimeSourceSet.allSource)
}
