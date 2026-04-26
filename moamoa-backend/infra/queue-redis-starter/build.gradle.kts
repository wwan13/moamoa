plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-backend:infra:queue-api"))
    implementation(project(":moamoa-backend:infra:queue-redis"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
