import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.gradle.jvm.tasks.Jar
import io.izzel.taboolib.gradle.Basic
import io.izzel.taboolib.gradle.Bukkit
import io.izzel.taboolib.gradle.BukkitUtil
import io.izzel.taboolib.gradle.CommandHelper
import io.izzel.taboolib.gradle.MinecraftChat
import java.util.Properties

plugins {
    java
    id("io.izzel.taboolib") version "2.0.30"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    `maven-publish`
}

// Read version from root project's gradle.properties
val rootProps = Properties().apply {
    load(file("${rootDir}/../gradle.properties").reader())
}
version = rootProps.getProperty("version", "0.0.0")
val commonVersion = rootProps.getProperty("version", "0.0.0")
val embeddedCommon by configurations.creating
val commonJar = file("${rootDir}/../common/build/libs/blackboxpro-common-$commonVersion.jar")

taboolib {
    env {
        install(Basic)
        install(Bukkit)
        install(BukkitUtil)
        install(CommandHelper)
        install(MinecraftChat)
    }
    description {
        name = "BlackBoxPro"
    }
    version { taboolib = "6.2.4-99fb800" }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files(commonJar))
    embeddedCommon(files(commonJar))
    compileOnly(kotlin("stdlib"))
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("ink.ptms.core:v12105:12105:mapped")
    compileOnly("ink.ptms.core:v12105:12105:universal")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    dependsOn(gradle.includedBuild("common").task(":jar"))
    compilerOptions {
        jvmTarget.set(JVM_1_8)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.withType<JavaCompile> {
    dependsOn(gradle.includedBuild("common").task(":jar"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.named<Jar>("jar") {
    dependsOn(gradle.includedBuild("common").task(":jar"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        embeddedCommon.map { zipTree(it) }
    })
}

// ======================== 发布配置 ========================

val pluginJar = tasks.named("jar")

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "aeolianReleases"
            url = uri("http://repo.aeoliancloud.com/repository/releases")
            isAllowInsecureProtocol = true
            credentials {
                username = (project.findProperty("aeolianUsername") as String?) ?: ""
                password = (project.findProperty("aeolianPassword") as String?) ?: ""
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()
            artifact(pluginJar)
            artifact(tasks.named("kotlinSourcesJar")) { classifier = "sources" }
        }
    }
}
