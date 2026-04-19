import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.1"
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
}

val commonJar = rootProject.extra["commonJar"] as File

base.archivesName.set("BlackBoxPro-runtime-1.12.2")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

minecraft {
    mcVersion.set("1.12.2")
    mcpMappingChannel.set("stable")
    mcpMappingVersion.set("39")

    extraRunJvmArguments.addAll("-ea:com.blackboxpro")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files(commonJar))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.25")
    implementation("com.google.code.gson:gson:2.8.9")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

