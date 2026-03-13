dependencies {
    implementation(project(":moamoa-infra:messaging-api"))
    implementation(project(":moamoa-support:support-logging"))

    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation(project(":moamoa-support:support-test"))
}
