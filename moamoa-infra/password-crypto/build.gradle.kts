dependencies {
    implementation(project(":moamoa-infra:password-api"))

    implementation("org.springframework.security:spring-security-crypto:6.4.4")

    testImplementation(project(":moamoa-support:support-test"))
}
