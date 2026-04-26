plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-backend:infra:password-api"))
    implementation(project(":moamoa-backend:infra:password-crypto"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
