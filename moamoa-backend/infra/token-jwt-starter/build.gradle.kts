plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-backend:infra:token-api"))
    implementation(project(":moamoa-backend:infra:token-jwt"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
