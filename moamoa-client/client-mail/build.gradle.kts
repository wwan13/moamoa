dependencies {
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    compileOnly(project(":moamoa-core:core-port"))
}