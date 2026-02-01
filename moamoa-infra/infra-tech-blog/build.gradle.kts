dependencies {
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")

    implementation("org.jsoup:jsoup:1.17.2")

    compileOnly(project(":moamoa-core:core-tech-blog"))

    testImplementation(project(":moamoa-support:support-test"))
}