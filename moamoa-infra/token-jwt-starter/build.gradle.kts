plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-infra:token-api"))
    implementation(project(":moamoa-infra:token-jwt"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-support:support-test"))
}
