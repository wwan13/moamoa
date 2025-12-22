dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    compileOnly(project(":moamoa-core:core-port"))
}
