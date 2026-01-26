tasks.getByName("bootJar") {
    enabled = true
}

tasks.getByName("jar") {
    enabled = false
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("io.asyncer:r2dbc-mysql")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation(project(":moamoa-infra:infra-mail"))
    implementation(project(":moamoa-infra:infra-redis"))
    implementation(project(":moamoa-infra:infra-security"))
    implementation(project(":moamoa-support:support-api-docs"))
    implementation(project(":moamoa-support:support-templates"))

    implementation(project(":moamoa-admin"))
    implementation(project(":moamoa-core:core-batch"))
}
