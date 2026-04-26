dependencies {
    implementation(project(":moamoa-backend:infra:set-api"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
