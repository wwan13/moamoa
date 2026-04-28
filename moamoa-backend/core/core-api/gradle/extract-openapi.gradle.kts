import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit

val openApiHttpMethods = setOf("get", "post", "put", "patch", "delete", "head", "options", "trace")
val openApiComponentKeys = listOf(
    "schemas",
    "parameters",
    "responses",
    "requestBodies",
    "headers",
    "securitySchemes",
)
val openApiRefPattern = Regex("^#/components/([^/]+)/(.+)$")

fun requireProjectProperty(name: String): String =
    gradle.startParameter.projectProperties[name]?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("-P$name is required")

fun normalizeHttpMethod(method: String): String {
    val normalized = method.trim().lowercase(Locale.ROOT)
    if (normalized !in openApiHttpMethods) {
        throw GradleException("Unsupported -Pmethod value: $method")
    }
    return normalized
}

fun normalizeOutputMode(): String {
    val mode = gradle.startParameter.projectProperties["mode"]?.trim()?.lowercase(Locale.ROOT) ?: "pretty-json"
    if (mode !in setOf("json", "pretty-json")) {
        throw GradleException("Unsupported -Pmode value: $mode")
    }
    return mode
}

fun findAvailablePort(): Int = ServerSocket(0).use { it.localPort }

fun loadDotEnv(): Map<String, String> {
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) return emptyMap()

    return envFile.readLines()
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val delimiterIndex = line.indexOf("=")
            val key = line.substring(0, delimiterIndex).trim()
            var value = line.substring(delimiterIndex + 1).trim()
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length - 1)
            }
            key to value
        }
}

fun collectRefs(node: Any?, collector: (String, String) -> Unit) {
    when (node) {
        is Map<*, *> -> node.forEach { (key, value) ->
            if (key == "\$ref" && value is String) {
                val match = openApiRefPattern.matchEntire(value)
                if (match != null) {
                    collector(match.groupValues[1], match.groupValues[2])
                }
            } else {
                collectRefs(value, collector)
            }
        }
        is List<*> -> node.forEach { collectRefs(it, collector) }
    }
}

fun deepMutableCopy(node: Any?): Any? =
    when (node) {
        is Map<*, *> -> node.entries.associate { (key, value) -> key.toString() to deepMutableCopy(value) }.toMutableMap()
        is List<*> -> node.map { deepMutableCopy(it) }.toMutableList()
        else -> node
    }

fun filterOpenApiDocument(document: Map<String, Any?>, pathPrefix: String, httpMethod: String): Map<String, Any?> {
    val paths = document["paths"] as? Map<*, *> ?: throw GradleException("OpenAPI document does not contain paths")

    val filteredPaths = linkedMapOf<String, MutableMap<String, Any?>>()
    for ((rawPath, rawItem) in paths) {
        val path = rawPath?.toString() ?: continue
        if (!path.startsWith(pathPrefix)) continue

        val pathItem = rawItem as? Map<*, *> ?: continue
        val filteredOperations = linkedMapOf<String, Any?>()

        for ((rawOperationName, rawOperation) in pathItem) {
            val operationName = rawOperationName?.toString()?.lowercase(Locale.ROOT) ?: continue
            if (operationName !in openApiHttpMethods) continue
            if (operationName != httpMethod) continue
            filteredOperations[operationName] = deepMutableCopy(rawOperation)
        }

        if (filteredOperations.isNotEmpty()) {
            filteredPaths[path] = filteredOperations
        }
    }

    if (filteredPaths.isEmpty()) {
        throw GradleException("No OpenAPI operations matched path=$pathPrefix method=${httpMethod.uppercase(Locale.ROOT)}")
    }

    val filteredDocument = linkedMapOf<String, Any?>()
    document.forEach { (key, value) ->
        when (key) {
            "paths" -> filteredDocument["paths"] = filteredPaths
            "components" -> Unit
            else -> filteredDocument[key] = deepMutableCopy(value)
        }
    }

    val components = document["components"] as? Map<*, *>
    if (components != null) {
        val queue = ArrayDeque<Pair<String, String>>()
        val visited = mutableSetOf<String>()
        val keptComponents = linkedMapOf<String, MutableMap<String, Any?>>()

        fun enqueue(componentType: String, componentName: String) {
            if (componentType !in openApiComponentKeys) return
            val identifier = "$componentType::$componentName"
            if (visited.add(identifier)) {
                queue.addLast(componentType to componentName)
            }
        }

        collectRefs(filteredPaths, ::enqueue)
        collectRefs(document["security"], ::enqueue)

        while (queue.isNotEmpty()) {
            val (componentType, componentName) = queue.removeFirst()
            val definitions = components[componentType] as? Map<*, *> ?: continue
            val definition = definitions[componentName] ?: continue
            val bucket = keptComponents.getOrPut(componentType) { linkedMapOf() }
            bucket.putIfAbsent(componentName, deepMutableCopy(definition))
            collectRefs(definition, ::enqueue)
        }

        if (keptComponents.isNotEmpty()) {
            filteredDocument["components"] = keptComponents
        }
    }

    return filteredDocument
}

fun waitForApiDocs(process: Process, port: Int): String {
    val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:$port/v3/api-docs"))
        .timeout(Duration.ofSeconds(3))
        .GET()
        .build()

    repeat(120) {
        if (!process.isAlive) {
            throw GradleException("core-api process exited before /v3/api-docs became available")
        }

        runCatching {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }.getOrNull()?.let { response ->
            if (response.statusCode() in 200..299) {
                return response.body()
            }
        }

        Thread.sleep(1_000)
    }

    throw GradleException("Timed out waiting for /v3/api-docs on port $port")
}

tasks.register("extractOpenApiSpec") {
    group = "documentation"
    description = "Boots core-api briefly and prints the filtered OpenAPI JSON to stdout"
    dependsOn("classes")

    doLast {
        val pathPrefix = requireProjectProperty("path")
        val httpMethod = normalizeHttpMethod(requireProjectProperty("method"))
        val outputMode = normalizeOutputMode()
        val port = findAvailablePort()
        val runtimeClasspath = files(
            layout.buildDirectory.dir("classes/kotlin/main"),
            layout.buildDirectory.dir("resources/main"),
            configurations.getByName("runtimeClasspath"),
        ).asPath
        val javaExecutable = File(System.getProperty("java.home"), "bin/java").absolutePath
        val processLogs = Collections.synchronizedList(mutableListOf<String>())

        val processBuilder = ProcessBuilder(
            javaExecutable,
            "-Dserver.port=$port",
            "-Dspring.main.lazy-initialization=true",
            "-Dspring.task.scheduling.enabled=false",
            "-Dspring.main.banner-mode=off",
            "-Dspringdoc.api-docs.path=/v3/api-docs",
            "-cp",
            runtimeClasspath,
            "server.core.CoreApiApplicationKt",
        )
        processBuilder.redirectErrorStream(true)
        processBuilder.environment().putAll(loadDotEnv() + System.getenv())

        val process = processBuilder.start()
        val logThread = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    synchronized(processLogs) {
                        if (processLogs.size >= 200) processLogs.removeAt(0)
                        processLogs.add(line)
                    }
                }
            }
        }.apply {
            isDaemon = true
            start()
        }

        try {
            val rawJson = waitForApiDocs(process, port)
            val document = JsonSlurper().parseText(rawJson) as? Map<*, *>
                ?: throw GradleException("Failed to parse /v3/api-docs response")
            @Suppress("UNCHECKED_CAST")
            val filteredDocument = filterOpenApiDocument(document as Map<String, Any?>, pathPrefix, httpMethod)
            val outputJson = JsonOutput.toJson(filteredDocument)
            if (outputMode == "json") {
                System.out.print(outputJson)
            } else {
                System.out.print(JsonOutput.prettyPrint(outputJson))
            }
        } catch (e: Exception) {
            val recentLogs = synchronized(processLogs) { processLogs.joinToString(System.lineSeparator()) }
            val message = buildString {
                append(e.message ?: "extractOpenApiSpec failed")
                if (recentLogs.isNotBlank()) {
                    append(System.lineSeparator())
                    append("--- core-api logs ---")
                    append(System.lineSeparator())
                    append(recentLogs)
                }
            }
            throw GradleException(message, e)
        } finally {
            process.destroy()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                process.waitFor(5, TimeUnit.SECONDS)
            }
            logThread.join(1_000)
        }
    }
}
