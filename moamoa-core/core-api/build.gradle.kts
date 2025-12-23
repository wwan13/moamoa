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

    implementation(project(":moamoa-core:core-tech-blog"))
    implementation(project(":moamoa-infra:infra-mail"))
    implementation(project(":moamoa-infra:infra-redis"))
    implementation(project(":moamoa-infra:infra-security"))
    implementation(project(":moamoa-support:support-api-docs"))
    implementation(project(":moamoa-support:support-templates"))

    runtimeOnly(project(":moamoa-admin"))
    runtimeOnly(project(":moamoa-infra:infra-tech-blog"))
}
