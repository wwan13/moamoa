tasks.getByName("bootJar") {
    enabled = true
}

tasks.getByName("jar") {
    enabled = false
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.7.2")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.7.2")
    runtimeOnly("com.mysql:mysql-connector-j")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation(project(":moamoa-backend:infra:mail-mailgun-starter"))
    implementation(project(":moamoa-backend:infra:cache-resilient-starter"))
    implementation(project(":moamoa-backend:infra:set-redis-starter"))
    implementation(project(":moamoa-backend:infra:queue-redis-starter"))
    implementation(project(":moamoa-backend:infra:lock-resilient-starter"))
    implementation(project(":moamoa-backend:infra:messaging-redis-starter"))
    implementation(project(":moamoa-backend:infra:token-jwt-starter"))
    implementation(project(":moamoa-backend:infra:password-crypto-starter"))
    implementation(project(":moamoa-backend:core:core-enum"))
    implementation(project(":moamoa-backend:support:support-api-docs"))
    implementation(project(":moamoa-backend:support:support-templates"))
    implementation(project(":moamoa-backend:support:support-webhook"))
    implementation(project(":moamoa-backend:support:support-logging"))
    implementation(project(":moamoa-backend:support:support-monitoring"))

    implementation(project(":moamoa-backend:admin"))
    implementation(project(":moamoa-backend:core:core-batch"))

    testImplementation(project(":moamoa-backend:support:support-test"))
}

apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
