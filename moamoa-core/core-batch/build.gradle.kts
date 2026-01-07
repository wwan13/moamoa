tasks.getByName("bootJar") {
    enabled = true
}

tasks.getByName("jar") {
    enabled = false
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("com.mysql:mysql-connector-j")

    implementation(project(":moamoa-core:core-tech-blog"))
    implementation(project(":moamoa-infra:infra-tech-blog"))
    implementation(project(":moamoa-infra:infra-redis"))
}