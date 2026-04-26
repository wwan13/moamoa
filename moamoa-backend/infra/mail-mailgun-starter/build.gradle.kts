plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-backend:infra:mail-api"))
    implementation(project(":moamoa-backend:infra:mail-mailgun"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
