tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")

    compileOnly("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("io.asyncer:r2dbc-mysql")

    compileOnly(project(":moamoa-core:core-port"))
    compileOnly(project(":moamoa-support:support-api-docs"))

    compileOnly(project(":moamoa-client:client-tech-blogs"))
}