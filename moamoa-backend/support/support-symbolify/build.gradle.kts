dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.0")

    testImplementation(project(":moamoa-backend:support:support-test"))
}
