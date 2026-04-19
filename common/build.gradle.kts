import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

val rootProps = Properties().apply {
    file("../gradle.properties").reader().use(::load)
}

group = "com.blackboxpro"
version = rootProps.getProperty("version", "0.0.0")

base {
    archivesName.set("blackboxpro-common")
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.code.gson:gson:2.10.1")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll("-Xjvm-default=all", "-Xmetadata-version=1.9.0")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}
