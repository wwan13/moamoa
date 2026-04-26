dependencies {
    implementation(project(":moamoa-backend:infra:mail-api"))
    implementation(project(":moamoa-backend:support:support-logging"))

    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
