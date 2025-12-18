dependencies {
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")

    compileOnly(project(":moamoa-core:core-port"))
}