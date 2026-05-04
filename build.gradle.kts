import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.Exec

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
}

allprojects {
    group = ""
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}

subprojects {
    if (!buildFile.exists()) {
        return@subprojects
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    dependencies {
        // Spring, Kotlin
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    }

    tasks.test {
        useJUnitPlatform()
    }

    kotlin {
        jvmToolchain(24)
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(24)
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    tasks.named("bootJar") {
        enabled = false
    }

    tasks.named("jar") {
        enabled = true
    }
}

val coreWebDir = rootDir.resolve("moamoa-frontend/core-web")
val symbolifyTarget = providers.gradleProperty("symbolifyTarget").orElse("all").get()

require(symbolifyTarget in setOf("all", "core-api", "core-web")) {
    "Unsupported -PsymbolifyTarget=$symbolifyTarget. Use all, core-api, or core-web."
}

tasks.register<Exec>("symbolifyCoreWebIndex") {
    group = "symbolify"
    description = "Builds the symbol index for core-web."
    workingDir = coreWebDir
    commandLine("npm", "run", "symbolify:index")
}

tasks.register<Exec>("symbolifyCoreWebFind") {
    group = "symbolify"
    description = "Finds symbol definitions from the generated core-web index."
    workingDir = coreWebDir

    doFirst {
        val symbolName = providers.gradleProperty("symbolName").orNull?.takeIf { it.isNotBlank() }
            ?: error("Provide -PsymbolName=<name>.")
        commandLine("npm", "run", "symbolify:find", "--", "--name", symbolName)
    }
}

tasks.register<Exec>("symbolifyCoreWebShow") {
    group = "symbolify"
    description = "Shows a symbol definition snippet from the generated core-web index."
    workingDir = coreWebDir

    doFirst {
        val symbolId = providers.gradleProperty("symbolId").orNull?.takeIf { it.isNotBlank() }
            ?: error("Provide -PsymbolId=<id>.")
        commandLine("npm", "run", "symbolify:show", "--", "--id", symbolId)
    }
}

tasks.register("symbolifyIndex") {
    group = "symbolify"
    description = "Builds symbol indexes for all currently wired symbolify targets."
    dependsOn(
        ":moamoa-backend:core:core-api:symbolifyIndex",
        "symbolifyCoreWebIndex",
    )
}

tasks.register("symbolifyFind") {
    group = "symbolify"
    description = "Finds symbol definitions from wired symbolify targets. Use -PsymbolName=<name> and optional -PsymbolifyTarget=all|core-api|core-web."

    if (symbolifyTarget == "all" || symbolifyTarget == "core-api") {
        dependsOn(":moamoa-backend:core:core-api:symbolifyFind")
    }
    if (symbolifyTarget == "all" || symbolifyTarget == "core-web") {
        dependsOn("symbolifyCoreWebFind")
    }
}

tasks.register("symbolifyShow") {
    group = "symbolify"
    description = "Shows a symbol snippet from one wired symbolify target. Use -PsymbolId=<id> -PsymbolifyTarget=core-api|core-web."

    doFirst {
        require(symbolifyTarget != "all") {
            "symbolifyShow requires -PsymbolifyTarget=core-api or -PsymbolifyTarget=core-web."
        }
    }

    if (symbolifyTarget == "core-api") {
        dependsOn(":moamoa-backend:core:core-api:symbolifyShow")
    }
    if (symbolifyTarget == "core-web") {
        dependsOn("symbolifyCoreWebShow")
    }
}
