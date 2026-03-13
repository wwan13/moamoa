tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("com.mysql:mysql-connector-j")

    implementation(project(":moamoa-crawler"))
    implementation(project(":moamoa-infra:cache-resilient-starter"))
    implementation(project(":moamoa-infra:set-redis-starter"))
    implementation(project(":moamoa-infra:queue-redis-starter"))
    implementation(project(":moamoa-support:support-webhook"))
    implementation(project(":moamoa-support:support-logging"))

    testImplementation(project(":moamoa-support:support-test"))
}
