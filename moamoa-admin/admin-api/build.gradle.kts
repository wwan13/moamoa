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

    implementation(project(":moamoa-core:core-shared"))
    implementation(project(":moamoa-support:support-api-docs"))
    implementation(project(":moamoa-support:support-logging"))

    implementation(project(":moamoa-core:core-tech-blog"))
    implementation(project(":moamoa-infra:infra-tech-blog"))
    implementation(project(":moamoa-infra:infra-jwt"))
    implementation(project(":moamoa-infra:infra-crypto"))
    implementation(project(":moamoa-infra:infra-redis"))
    implementation(project(":moamoa-infra:infra-cache"))

    testImplementation(project(":moamoa-support:support-test"))
}

apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
