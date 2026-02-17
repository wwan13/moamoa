dependencies {
    implementation(project(":moamoa-core:core-shared"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    testImplementation(project(":moamoa-support:support-test"))
}
