dependencies {
    implementation(project(":moamoa-core:core-shared"))
    implementation(project(":moamoa-support:support-logging"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.redisson:redisson:4.2.0")

    testImplementation(project(":moamoa-support:support-test"))
}
