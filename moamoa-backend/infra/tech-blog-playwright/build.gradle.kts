dependencies {
    implementation(project(":moamoa-backend:infra:tech-blog-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("com.microsoft.playwright:playwright:1.59.0")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
