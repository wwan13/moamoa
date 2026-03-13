dependencies {
    implementation(project(":moamoa-infra:cache-api"))
    implementation(project(":moamoa-support:support-logging"))

    implementation("org.springframework.boot:spring-boot-starter-aop")

    testImplementation(project(":moamoa-support:support-test"))
}
