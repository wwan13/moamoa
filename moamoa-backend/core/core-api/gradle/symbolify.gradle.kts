import org.gradle.api.tasks.JavaExec

val symbolifyOutputDir = layout.buildDirectory.dir("symbolify/core-api")
val symbolifyRuntime = configurations.create("symbolifyRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    add(symbolifyRuntime.name, project(":moamoa-backend:support:support-symbolify"))
}

tasks.register<JavaExec>("symbolifyIndex") {
    group = "symbolify"
    description = "Builds a symbol index for core-api Kotlin sources."
    classpath = symbolifyRuntime
    mainClass.set("server.support.symbolify.SymbolifyCli")
    args(
        "index",
        "--root",
        projectDir.resolve("src/main/kotlin").absolutePath,
        "--output",
        symbolifyOutputDir.get().asFile.absolutePath,
        "--module",
        "core-api",
        "--source-set",
        "main",
    )
}

tasks.register<JavaExec>("symbolifyFind") {
    group = "symbolify"
    description = "Finds symbol definitions from the generated core-api index."
    classpath = symbolifyRuntime
    mainClass.set("server.support.symbolify.SymbolifyCli")

    doFirst {
        val symbolName = project.findProperty("symbolName")?.toString()?.takeIf { it.isNotBlank() }
            ?: error("Provide -PsymbolName=<name>.")
        args(
            "find",
            "--output",
            symbolifyOutputDir.get().asFile.absolutePath,
            "--name",
            symbolName,
        )
    }
}

tasks.register<JavaExec>("symbolifyShow") {
    group = "symbolify"
    description = "Shows a symbol definition snippet from the generated core-api index."
    classpath = symbolifyRuntime
    mainClass.set("server.support.symbolify.SymbolifyCli")

    doFirst {
        val symbolId = project.findProperty("symbolId")?.toString()?.takeIf { it.isNotBlank() }
            ?: error("Provide -PsymbolId=<id>.")
        args(
            "show",
            "--output",
            symbolifyOutputDir.get().asFile.absolutePath,
            "--id",
            symbolId,
        )
    }
}
