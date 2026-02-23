tasks.getByName("bootJar") {
    enabled = true
}

tasks.getByName("jar") {
    enabled = false
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("io.asyncer:r2dbc-mysql")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation(project(":moamoa-core:core-shared"))
    implementation(project(":moamoa-infra:infra-mailgun"))
    implementation(project(":moamoa-infra:infra-redis"))
    implementation(project(":moamoa-infra:infra-cache"))
    implementation(project(":moamoa-infra:infra-lock"))
    implementation(project(":moamoa-infra:infra-jwt"))
    implementation(project(":moamoa-infra:infra-crypto"))
    implementation(project(":moamoa-support:support-api-docs"))
    implementation(project(":moamoa-support:support-templates"))
    implementation(project(":moamoa-support:support-webhook"))
    implementation(project(":moamoa-support:support-logging"))
    implementation(project(":moamoa-support:support-monitoring"))

    implementation(project(":moamoa-admin:admin-api"))
    implementation(project(":moamoa-core:core-batch"))

    testImplementation(project(":moamoa-support:support-test"))
}
