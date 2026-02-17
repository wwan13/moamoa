tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("com.mysql:mysql-connector-j")

    implementation(project(":moamoa-core:core-shared"))
    implementation(project(":moamoa-core:core-tech-blog"))
    implementation(project(":moamoa-infra:infra-tech-blog"))
    implementation(project(":moamoa-infra:infra-redis"))
    implementation(project(":moamoa-infra:infra-cache"))
    implementation(project(":moamoa-infra:infra-lmstudio"))
    implementation(project(":moamoa-support:support-webhook"))

    testImplementation(project(":moamoa-support:support-test"))
}
