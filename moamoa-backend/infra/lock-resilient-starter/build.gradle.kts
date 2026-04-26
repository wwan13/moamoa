plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-backend:infra:lock-api"))
    implementation(project(":moamoa-backend:infra:lock-redisson"))
    implementation(project(":moamoa-backend:infra:lock-local"))
    implementation(project(":moamoa-backend:infra:lock-resilient"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
