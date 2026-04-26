dependencies {
    api("io.github.oshai:kotlin-logging-jvm:6.0.9")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
    api("org.springframework:spring-web")
    api("net.logstash.logback:logstash-logback-encoder:8.1")
    testImplementation(project(":moamoa-backend:support:support-test"))
}
