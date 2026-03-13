tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")

    implementation(project(":moamoa-support:support-api-docs"))
    implementation(project(":moamoa-support:support-logging"))

    implementation(project(":moamoa-crawler"))
    implementation(project(":moamoa-infra:token-jwt-starter"))
    implementation(project(":moamoa-infra:password-crypto-starter"))
    implementation(project(":moamoa-infra:cache-resilient-starter"))

    testImplementation(project(":moamoa-support:support-test"))
}

apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
