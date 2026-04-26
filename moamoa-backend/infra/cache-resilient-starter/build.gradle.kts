plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-backend:infra:cache-api"))
    implementation(project(":moamoa-backend:infra:cache-redis"))
    implementation(project(":moamoa-backend:infra:cache-caffeine"))
    implementation(project(":moamoa-backend:infra:cache-resilient"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
