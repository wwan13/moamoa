dependencies {
    implementation(project(":moamoa-infra:mail-api"))
    implementation(project(":moamoa-support:support-logging"))

    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    testImplementation(project(":moamoa-support:support-test"))
}
