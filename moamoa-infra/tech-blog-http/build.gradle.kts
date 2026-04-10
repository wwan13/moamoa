dependencies {
    implementation(project(":moamoa-infra:tech-blog-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")

    testImplementation(project(":moamoa-support:support-test"))
}
