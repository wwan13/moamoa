package server.support.symbolify

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.time.Instant

private val mapper = jacksonObjectMapper()

data class SymbolRecord(
    val id: String,
    val name: String,
    val normalizedName: String,
    val kind: String,
    val language: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val columnStart: Int,
    val columnEnd: Int,
    val containerName: String?,
    val containerKind: String?,
    val signature: String?,
    val exported: Boolean,
    val visibility: String?,
    val moduleHint: String,
    val sourceSet: String,
    val tags: List<String>,
)

data class IndexManifest(
    val version: Int,
    val generatedAt: String,
    val repoRoot: String,
    val language: String,
    val moduleHint: String,
    val sourceSet: String,
    val symbolCount: Int,
    val fileCount: Int,
)

object SymbolifyCli {
    @JvmStatic
    fun main(rawArgs: Array<String>) {
        require(rawArgs.isNotEmpty()) { "Usage: index|find|show [options]" }

        when (rawArgs.first()) {
            "index" -> runIndex(parseOptions(rawArgs.drop(1)))
            "find" -> runFind(parseOptions(rawArgs.drop(1)))
            "show" -> runShow(parseOptions(rawArgs.drop(1)))
            else -> error("Unknown command: ${rawArgs.first()}")
        }
    }

    private fun runIndex(options: Map<String, String>) {
        val root = requiredFileOption(options, "--root")
        val outputDir = requiredFileOption(options, "--output")
        val moduleHint = options["--module"] ?: "core-api"
        val sourceSet = options["--source-set"] ?: "main"

        val symbols = KotlinSymbolIndexer.index(root, moduleHint, sourceSet)
        writeIndex(outputDir, root, moduleHint, sourceSet, symbols)

        println("Indexed ${symbols.size} symbols into ${outputDir.absolutePath}")
    }

    private fun runFind(options: Map<String, String>) {
        val outputDir = requiredFileOption(options, "--output")
        val name = options["--name"]?.trim().orEmpty()
        require(name.isNotEmpty()) { "Provide --name" }

        val normalizedName = normalizeName(name)
        val byNameFile = File(outputDir, "by-name/$normalizedName.json")
        if (!byNameFile.exists()) {
            println("No symbol named $name")
            return
        }

        val symbols: List<SymbolRecord> = mapper.readValue(byNameFile, mapper.typeFactory.constructCollectionType(List::class.java, SymbolRecord::class.java))
        symbols.forEach { symbol ->
            println(
                listOf(
                    symbol.id,
                    symbol.kind,
                    symbol.moduleHint,
                    "${symbol.filePath}:${symbol.lineStart}-${symbol.lineEnd}",
                    symbol.signature.orEmpty(),
                ).joinToString(" | ")
            )
        }
    }

    private fun runShow(options: Map<String, String>) {
        val outputDir = requiredFileOption(options, "--output")
        val symbolId = options["--id"]?.trim().orEmpty()
        require(symbolId.isNotEmpty()) { "Provide --id" }

        val symbolsFile = File(outputDir, "symbols.jsonl")
        require(symbolsFile.exists()) { "Index file does not exist: ${symbolsFile.absolutePath}" }

        val symbol = symbolsFile.useLines { lines ->
            lines.map { mapper.readValue(it, SymbolRecord::class.java) }.firstOrNull { it.id == symbolId }
        } ?: error("No symbol found for id=$symbolId")

        println("${symbol.name} (${symbol.kind})")
        println("${symbol.filePath}:${symbol.lineStart}-${symbol.lineEnd}")
        symbol.signature?.takeIf { it.isNotBlank() }?.let(::println)
        println()
        printSnippet(symbol)
    }

    private fun writeIndex(
        outputDir: File,
        root: File,
        moduleHint: String,
        sourceSet: String,
        symbols: List<SymbolRecord>,
    ) {
        outputDir.mkdirs()
        File(outputDir, "by-name").mkdirs()
        File(outputDir, "by-file").mkdirs()

        val manifest = IndexManifest(
            version = 1,
            generatedAt = Instant.now().toString(),
            repoRoot = root.absolutePath,
            language = "kotlin",
            moduleHint = moduleHint,
            sourceSet = sourceSet,
            symbolCount = symbols.size,
            fileCount = symbols.map { it.filePath }.distinct().size,
        )
        mapper.writerWithDefaultPrettyPrinter().writeValue(File(outputDir, "manifest.json"), manifest)

        val sortedSymbols = symbols.sortedWith(compareBy<SymbolRecord>({ it.filePath }, { it.lineStart }, { it.name }, { it.kind }))
        File(outputDir, "symbols.jsonl").bufferedWriter().use { writer ->
            sortedSymbols.forEach { symbol ->
                writer.appendLine(mapper.writeValueAsString(symbol))
            }
        }

        sortedSymbols.groupBy { it.normalizedName }.forEach { (normalizedName, matchingSymbols) ->
            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(File(outputDir, "by-name/$normalizedName.json"), matchingSymbols)
        }

        sortedSymbols.groupBy { encodeFileKey(it.filePath) }.forEach { (encodedPath, matchingSymbols) ->
            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(File(outputDir, "by-file/$encodedPath.json"), matchingSymbols)
        }
    }

    private fun printSnippet(symbol: SymbolRecord) {
        val file = File(symbol.filePath)
        require(file.exists()) { "Source file does not exist: ${file.absolutePath}" }

        val lines = file.readLines()
        val startIndex = maxOf(symbol.lineStart - 2, 1)
        val endIndex = minOf(symbol.lineEnd + 2, lines.size)
        for (lineNumber in startIndex..endIndex) {
            val prefix = if (lineNumber in symbol.lineStart..symbol.lineEnd) ">" else " "
            println("$prefix ${lineNumber.toString().padStart(4)} ${lines[lineNumber - 1]}")
        }
    }

    private fun parseOptions(args: List<String>): Map<String, String> {
        val options = linkedMapOf<String, String>()
        var index = 0
        while (index < args.size) {
            val key = args[index]
            require(key.startsWith("--")) { "Unexpected argument: $key" }
            val value = args.getOrNull(index + 1) ?: error("Missing value for $key")
            options[key] = value
            index += 2
        }
        return options
    }

    private fun requiredFileOption(options: Map<String, String>, key: String): File {
        val value = options[key]?.takeIf { it.isNotBlank() } ?: error("Provide $key")
        return File(value)
    }
}

fun normalizeName(name: String): String = name.lowercase()

private fun encodeFileKey(path: String): String = path.replace(File.separatorChar, '_').replace('/', '_')
