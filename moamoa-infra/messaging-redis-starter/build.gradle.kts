plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-infra:messaging-api"))
    implementation(project(":moamoa-infra:messaging-redis"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-support:support-test"))
}
