dependencies {
    implementation(project(":moamoa-backend:infra:password-api"))

    implementation("org.springframework.security:spring-security-crypto:6.4.4")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
