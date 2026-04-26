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

    implementation(project(":moamoa-backend:infra:tech-blog-starter"))
    implementation(project(":moamoa-backend:infra:cache-resilient-starter"))
    implementation(project(":moamoa-backend:infra:set-redis-starter"))
    implementation(project(":moamoa-backend:infra:queue-redis-starter"))
    implementation(project(":moamoa-backend:infra:mail-mailgun-starter"))
    implementation(project(":moamoa-backend:support:support-webhook"))
    implementation(project(":moamoa-backend:support:support-logging"))
    implementation(project(":moamoa-backend:support:support-templates"))

    testImplementation(project(":moamoa-backend:support:support-test"))
}
