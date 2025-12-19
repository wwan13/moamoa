plugins {
    kotlin("plugin.jpa") version "1.9.25"
}

tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter-web")

    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")

    compileOnly(project(":moamoa-core:core-port"))
    compileOnly(project(":moamoa-support:support-api-docs"))

    compileOnly(project(":moamoa-client:client-tech-blogs"))
}