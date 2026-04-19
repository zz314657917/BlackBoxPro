import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("net.neoforged.moddev") version "2.0.140"
}

val rootProps = Properties().apply {
    rootProject.file("gradle.properties").reader().use(::load)
}
val commonVersion = rootProps.getProperty("version", "0.0.0")

evaluationDependsOn(":common")

val commonSourceSet = project(":common").extensions.getByType(SourceSetContainer::class.java).getByName("main")
val commonSourcesDir = project(":common").file("src/main/kotlin")
val commonOutputs = files(commonSourceSet.output).builtBy(project(":common").tasks.named("classes"))

base {
    archivesName.set("BlackBoxPro-runtime-1.21.1")
}

repositories {
    mavenCentral()
    maven("https://repo.tabooproject.org/repository/releases/")
}

neoForge {
    neoFormVersion = "1.21.1-20240808.144430"
}

the<SourceSetContainer>()["main"].java.srcDir(commonSourcesDir)

dependencies {
    api(project(":common"))
    implementation(project(":common"))
    compileOnly(commonOutputs)
    implementation("org.tabooproject.reflex:reflex:1.2.3")
}

tasks.withType<KotlinCompile> {
    dependsOn(project(":common").tasks.named("classes"))
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll("-Xjvm-default=all")
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
