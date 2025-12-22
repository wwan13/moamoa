dependencies {
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.springframework.security:spring-security-crypto:6.4.4")

    compileOnly(project(":moamoa-core:core-port"))
}