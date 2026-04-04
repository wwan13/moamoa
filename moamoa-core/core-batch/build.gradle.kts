tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("io.asyncer:r2dbc-mysql")

    implementation(project(":moamoa-crawler"))
    implementation(project(":moamoa-infra:cache-resilient-starter"))
    implementation(project(":moamoa-infra:set-redis-starter"))
    implementation(project(":moamoa-infra:queue-redis-starter"))
    implementation(project(":moamoa-infra:mail-mailgun-starter"))
    implementation(project(":moamoa-support:support-webhook"))
    implementation(project(":moamoa-support:support-logging"))
    implementation(project(":moamoa-support:support-templates"))

    testImplementation(project(":moamoa-support:support-test"))
}
