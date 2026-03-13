plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-infra:mail-api"))
    implementation(project(":moamoa-infra:mail-mailgun"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-support:support-test"))
}
