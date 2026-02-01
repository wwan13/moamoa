dependencies {
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")

    testImplementation(project(":moamoa-support:support-test"))
}