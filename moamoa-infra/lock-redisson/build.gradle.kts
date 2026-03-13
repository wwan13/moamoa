dependencies {
    implementation(project(":moamoa-infra:lock-api"))
    implementation(project(":moamoa-support:support-logging"))

    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.redisson:redisson:4.2.0")

    testImplementation(project(":moamoa-support:support-test"))
}
