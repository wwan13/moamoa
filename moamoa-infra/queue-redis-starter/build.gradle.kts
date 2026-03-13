plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-infra:queue-api"))
    implementation(project(":moamoa-infra:queue-redis"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-support:support-test"))
}
