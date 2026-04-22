import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync

val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows

@Suppress("UNCHECKED_CAST")
val execTaskClass = Class.forName("org.gradle.api.tasks.Exec") as Class<Exec>
@Suppress("UNCHECKED_CAST")
val syncTaskClass = Class.forName("org.gradle.api.tasks.Sync") as Class<Sync>

val rootGradlew = if (isWindows) file("gradlew.bat") else file("gradlew")
val pluginGradlew = if (isWindows) file("plugin/gradlew.bat") else file("plugin/gradlew")
val forge1122Gradlew = if (isWindows) file("mod/1.12.2/gradlew.bat") else file("mod/1.12.2/gradlew")
val forge1201Gradlew = if (isWindows) file("mod/1.20.1/gradlew.bat") else file("mod/1.20.1/gradlew")
val commonProjectDir = file("common")
val modProjectDir = file("mod")
val pluginProjectDir = file("plugin")
val forge1122ProjectDir = file("mod/1.12.2")
val forge1201ProjectDir = file("mod/1.20.1")
val forge1122GradleUserHome = file(".gradle-user-home/forge1122")
val forge1201GradleUserHome = file(".gradle-user-home/forge1201")
val localTemurin21Home = file("../.local-tools/temurin21/jdk-21.0.10+7")

fun Exec.configureLocalJava21IfPresent() {
    if (localTemurin21Home.isDirectory) {
        environment("JAVA_HOME", localTemurin21Home.absolutePath)
        val currentPath = System.getenv("PATH") ?: ""
        environment("PATH", localTemurin21Home.resolve("bin").absolutePath + java.io.File.pathSeparator + currentPath)
    }
}

tasks.register("common_build", execTaskClass, object : Action<Exec> {
    override fun execute(task: Exec) {
        task.group = "standalone"
        task.description = "构建 common jar"
        task.workingDir = rootDir
        task.commandLine(rootGradlew.absolutePath, "-p", commonProjectDir.absolutePath, "--no-daemon", "jar")
    }
})

tasks.register("plugin_build", execTaskClass, object : Action<Exec> {
    override fun execute(task: Exec) {
        task.group = "standalone"
        task.description = "build 独立项目 plugin"
        task.workingDir = pluginProjectDir
        task.commandLine(pluginGradlew.absolutePath, "--no-daemon", "build")
    }
})

tasks.register("plugin_clean", execTaskClass, object : Action<Exec> {
    override fun execute(task: Exec) {
        task.group = "standalone"
        task.description = "clean 独立项目 plugin"
        task.workingDir = pluginProjectDir
        task.commandLine(pluginGradlew.absolutePath, "--no-daemon", "clean")
        task.isIgnoreExitValue = true
    }
})

tasks.register("forge1122_build", execTaskClass, object : Action<Exec> {
    override fun execute(task: Exec) {
        task.group = "standalone"
        task.description = "build 独立项目 1.12.2"
        task.dependsOn("common_build")
        task.workingDir = forge1122ProjectDir
        task.commandLine(
            forge1122Gradlew.absolutePath,
            "-g",
            forge1122GradleUserHome.absolutePath,
            "--no-daemon",
            "clean",
            "build"
        )
    }
})


tasks.register("forge1122_clean", execTaskClass, object : Action<Exec> {
    override fun execute(task: Exec) {
        task.group = "standalone"
        task.description = "clean 独立项目 1.12.2"
        task.workingDir = forge1122ProjectDir
        task.commandLine(
            forge1122Gradlew.absolutePath,
            "-g",
            forge1122GradleUserHome.absolutePath,
            "--no-daemon",
            "clean"
        )
        task.isIgnoreExitValue = true
    }
})

tasks.register("forge1201_build", execTaskClass, object : Action<Exec> {
    override fun execute(task: Exec) {
        task.group = "standalone"
        task.description = "build 独立项目 1.20.1 Forge"
        task.workingDir = forge1201ProjectDir
        task.configureLocalJava21IfPresent()
        task.commandLine(
            forge1201Gradlew.absolutePath,
            "-g",
            forge1201GradleUserHome.absolutePath,
            "--no-daemon",
            "clean",
            "build"
        )
    }
})

tasks.register("forge1201_clean", execTaskClass, object : Action<Exec> {
    override fun execute(task: Exec) {
        task.group = "standalone"
        task.description = "clean 独立项目 1.20.1 Forge"
        task.workingDir = forge1201ProjectDir
        task.configureLocalJava21IfPresent()
        task.commandLine(
            forge1201Gradlew.absolutePath,
            "-g",
            forge1201GradleUserHome.absolutePath,
            "--no-daemon",
            "clean"
        )
        task.isIgnoreExitValue = true
    }
})

tasks.register("mod2111_build", execTaskClass, object : Action<Exec> {
    override fun execute(task: Exec) {
        task.group = "standalone"
        task.description = "构建 mod 1.21.11 客户端产物"
        task.workingDir = rootDir
        task.commandLine(
            rootGradlew.absolutePath,
            "-p",
            modProjectDir.absolutePath,
            "--no-daemon",
            ":1.21.11:runtime:build",
            ":1.21.11:fabric:build",
            ":1.21.11:neoforge:build"
        )
    }
})

val collectJars = tasks.register("collectJars", syncTaskClass, object : Action<Sync> {
    override fun execute(task: Sync) {
        task.group = "build"
        task.description = "收集客户端 mod 与服务端插件的 jar 到根 build/libs"
        task.into(layout.buildDirectory.dir("libs"))
        // 1.21.11
        task.from(fileTree("mod/1.21.11/fabric/build/libs"))
        task.from(fileTree("mod/1.21.11/neoforge/build/libs"))
        // 1.20.1
        task.from(fileTree("mod/1.20.1/build/libs"))
        // 1.12.2
        task.from(fileTree("mod/1.12.2/forge/build/libs"))
        // plugin
        task.from(fileTree("plugin/build/libs"))
    }
})

tasks.register("buildAll", object : Action<Task> {
    override fun execute(task: Task) {
        task.group = "build"
        task.description = "构建 common、1.21.11、1.20.1、1.12.2 客户端与服务端插件并收集 jar 到根 build/libs"
        task.dependsOn("common_build", "mod2111_build", "plugin_build", "forge1122_build", "forge1201_build")
        task.finalizedBy(collectJars)
    }
})

tasks.register("cleanAll", object : Action<Task> {
    override fun execute(task: Task) {
        task.group = "build"
        task.description = "清理所有模块的 build 目录（含根 build 目录）"
        task.dependsOn("plugin_clean", "forge1122_clean", "forge1201_clean")
        task.doLast {
            delete(
                layout.buildDirectory,
                file("common/build"),
                file("plugin/build"),
                file("mod/build"),
                file("mod/1.21.11/runtime/build"),
                file("mod/1.21.11/fabric/build"),
                file("mod/1.21.11/neoforge/build"),
                file("mod/1.21.1/runtime/build"),
                file("mod/1.21.1/fabric/build"),
                file("mod/1.21.1/neoforge/build"),
                file("mod/1.20.1/build"),
                file("mod/1.12.2/build"),
                file("mod/1.12.2/runtime/build"),
                file("mod/1.12.2/forge/build")
            )
        }
    }
})
