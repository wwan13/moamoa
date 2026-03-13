plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-infra:lock-api"))
    implementation(project(":moamoa-infra:lock-redisson"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-support:support-test"))
}
