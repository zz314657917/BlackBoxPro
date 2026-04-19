import java.util.Properties

plugins {
    base
}

val rootProps = Properties().apply {
    file("${rootDir}/../../gradle.properties").reader(Charsets.UTF_8).use(::load)
}

val sharedVersion = rootProps.getProperty("version", "0.0.0")
val commonVersion = sharedVersion
val commonJar = file("${rootDir}/../../common/build/libs/blackboxpro-common-${commonVersion}.jar")
extra["commonJar"] = commonJar

allprojects {
    group = "com.blackboxpro"
    version = sharedVersion
}

subprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

tasks.named("build") {
    dependsOn(":runtime:build", ":forge:build")
    finalizedBy("collectJars")
}

tasks.named("clean") {
    dependsOn(":runtime:clean", ":forge:clean")
}

tasks.register("buildAll") {
    group = "build"
    description = "构建 1.12.2 runtime 与 forge 并收集 jar"
    dependsOn("build")
}

tasks.register("cleanAll") {
    group = "build"
    description = "清理 1.12.2 所有构建产物"
    dependsOn("clean")
}

tasks.register<DefaultTask>("jar") {
    group = "build"
    description = "构建 1.12.2 Forge jar"
    dependsOn(":forge:jar")
}

tasks.register<Sync>("collectJars") {
    group = "build"
    description = "收集 1.12.2 forge 客户端 mod 的 jar 到 mod/1.12.2/build/libs"
    into(layout.buildDirectory.dir("libs"))
    from(project(":forge").layout.buildDirectory.dir("libs"))
}
