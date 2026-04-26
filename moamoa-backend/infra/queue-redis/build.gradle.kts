dependencies {
    implementation(project(":moamoa-backend:infra:queue-api"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
