dependencies {
    implementation(project(":moamoa-infra:queue-api"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation(project(":moamoa-support:support-test"))
}
