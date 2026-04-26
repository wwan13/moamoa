dependencies {
    implementation(project(":moamoa-backend:infra:cache-api"))

    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
