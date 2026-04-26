dependencies {
    implementation(project(":moamoa-backend:infra:cache-api"))
    implementation(project(":moamoa-backend:support:support-logging"))

    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
