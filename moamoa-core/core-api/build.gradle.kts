tasks.getByName("bootJar") {
    dependsOn(":moamoa-admin:build")
    enabled = true
}

tasks.getByName("jar") {
    enabled = false
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("io.asyncer:r2dbc-mysql")

    implementation(project(":moamoa-core:core-port"))
    implementation(project(":moamoa-support:support-api-docs"))

    runtimeOnly(project(":moamoa-admin"))
    runtimeOnly(project(":moamoa-client:client-tech-blogs"))
}
