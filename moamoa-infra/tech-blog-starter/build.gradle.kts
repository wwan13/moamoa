plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-infra:tech-blog-api"))
    implementation(project(":moamoa-infra:tech-blog-http"))
    implementation(project(":moamoa-infra:tech-blog-jsoup"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-support:support-test"))
}
