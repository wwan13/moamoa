plugins {
    `java-library`
}

dependencies {
    api(project(":moamoa-backend:infra:tech-blog-api"))
    implementation(project(":moamoa-backend:infra:tech-blog-http"))
    implementation(project(":moamoa-backend:infra:tech-blog-jsoup"))
    implementation(project(":moamoa-backend:infra:tech-blog-playwright"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
