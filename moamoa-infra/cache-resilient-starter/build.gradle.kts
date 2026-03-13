plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-infra:cache-api"))
    implementation(project(":moamoa-infra:cache-redis"))
    implementation(project(":moamoa-infra:cache-caffeine"))
    implementation(project(":moamoa-infra:cache-resilient"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-support:support-test"))
}
