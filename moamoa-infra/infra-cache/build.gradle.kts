dependencies {
    implementation(project(":moamoa-core:core-shared"))
    implementation(project(":moamoa-support:support-logging"))
    implementation(project(":moamoa-infra:infra-redis"))
    implementation(project(":moamoa-infra:infra-caffeine"))
    implementation("org.springframework.boot:spring-boot-starter-aop")

    testImplementation(project(":moamoa-support:support-test"))
}
