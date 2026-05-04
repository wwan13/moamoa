package server.support.symbolify

import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeAlias
import java.io.File
import java.security.MessageDigest

object KotlinSymbolIndexer {
    fun index(root: File, moduleHint: String, sourceSet: String): List<SymbolRecord> {
        require(root.exists()) { "Root does not exist: ${root.absolutePath}" }

        val disposable = Disposer.newDisposable("symbolify-kotlin-indexer")
        return try {
            setIdeaIoUseFallback()
            val configuration = CompilerConfiguration().apply {
                put(CommonConfigurationKeys.MODULE_NAME, "symbolify")
                put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
            }
            val environment = KotlinCoreEnvironment.createForProduction(
                disposable,
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )
            val psiFactory = KtPsiFactory(environment.project, false)

            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .sortedBy { it.relativeTo(root).path }
                .flatMap { indexFile(it, root, moduleHint, sourceSet, psiFactory) }
                .toList()
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun indexFile(
        file: File,
        root: File,
        moduleHint: String,
        sourceSet: String,
        psiFactory: KtPsiFactory,
    ): List<SymbolRecord> {
        val content = file.readText()
        val lineIndex = LineIndex(content)
        val ktFile = psiFactory.createFile(file.name, content)
        val candidates = mutableListOf<SymbolCandidate>()

        collectDeclarations(
            declarations = ktFile.declarations,
            file = file,
            root = root,
            lineIndex = lineIndex,
            content = content,
            moduleHint = moduleHint,
            sourceSet = sourceSet,
            container = null,
            sink = candidates,
        )

        return candidates
            .sortedWith(compareBy<SymbolCandidate>({ it.absoluteFilePath }, { it.startOffset }, { it.name }, { it.kind }))
            .map { it.toRecord() }
    }

    private fun collectDeclarations(
        declarations: List<KtDeclaration>,
        file: File,
        root: File,
        lineIndex: LineIndex,
        content: String,
        moduleHint: String,
        sourceSet: String,
        container: ContainerMeta?,
        sink: MutableList<SymbolCandidate>,
    ) {
        declarations.forEach { declaration ->
            when (declaration) {
                is KtClass -> {
                    val candidate = buildCandidate(
                        declaration = declaration,
                        name = declaration.name ?: return@forEach,
                        kind = declarationKind(declaration),
                        file = file,
                        root = root,
                        lineIndex = lineIndex,
                        content = content,
                        moduleHint = moduleHint,
                        sourceSet = sourceSet,
                        container = container,
                        tags = emptyList(),
                    )
                    sink += candidate
                    collectDeclarations(
                        declarations = declaration.body?.declarations.orEmpty(),
                        file = file,
                        root = root,
                        lineIndex = lineIndex,
                        content = content,
                        moduleHint = moduleHint,
                        sourceSet = sourceSet,
                        container = candidate.toContainerMeta(),
                        sink = sink,
                    )
                }

                is KtObjectDeclaration -> {
                    val candidate = buildCandidate(
                        declaration = declaration,
                        name = declaration.name ?: if (declaration.isCompanion()) "Companion" else return@forEach,
                        kind = "object",
                        file = file,
                        root = root,
                        lineIndex = lineIndex,
                        content = content,
                        moduleHint = moduleHint,
                        sourceSet = sourceSet,
                        container = container,
                        tags = if (declaration.isCompanion()) listOf("companion-object") else emptyList(),
                    )
                    sink += candidate
                    collectDeclarations(
                        declarations = declaration.body?.declarations.orEmpty(),
                        file = file,
                        root = root,
                        lineIndex = lineIndex,
                        content = content,
                        moduleHint = moduleHint,
                        sourceSet = sourceSet,
                        container = candidate.toContainerMeta(),
                        sink = sink,
                    )
                }

                is KtNamedFunction -> {
                    sink += buildCandidate(
                        declaration = declaration,
                        name = declaration.name ?: return@forEach,
                        kind = "function",
                        file = file,
                        root = root,
                        lineIndex = lineIndex,
                        content = content,
                        moduleHint = moduleHint,
                        sourceSet = sourceSet,
                        container = container,
                        tags = if (declaration.receiverTypeReference != null) listOf("extension-function") else emptyList(),
                    )
                }

                is KtProperty -> {
                    sink += buildCandidate(
                        declaration = declaration,
                        name = declaration.name ?: return@forEach,
                        kind = "property",
                        file = file,
                        root = root,
                        lineIndex = lineIndex,
                        content = content,
                        moduleHint = moduleHint,
                        sourceSet = sourceSet,
                        container = container,
                        tags = emptyList(),
                    )
                }

                is KtTypeAlias -> {
                    sink += buildCandidate(
                        declaration = declaration,
                        name = declaration.name ?: return@forEach,
                        kind = "typealias",
                        file = file,
                        root = root,
                        lineIndex = lineIndex,
                        content = content,
                        moduleHint = moduleHint,
                        sourceSet = sourceSet,
                        container = container,
                        tags = emptyList(),
                    )
                }
            }
        }
    }

    private fun buildCandidate(
        declaration: KtDeclaration,
        name: String,
        kind: String,
        file: File,
        root: File,
        lineIndex: LineIndex,
        content: String,
        moduleHint: String,
        sourceSet: String,
        container: ContainerMeta?,
        tags: List<String>,
    ): SymbolCandidate {
        val startOffset = declaration.textRange.startOffset
        val endOffset = declaration.textRange.endOffset
        val startLineColumn = lineIndex.lineColumn(startOffset)
        val endLineColumn = lineIndex.lineColumn((endOffset - 1).coerceAtLeast(startOffset))
        val relativePath = file.relativeTo(root).invariantSeparatorsPath
        val visibility = extractVisibility(declaration)

        return SymbolCandidate(
            id = stableId(relativePath, kind, name, startLineColumn.first, startLineColumn.second + 1),
            name = name,
            normalizedName = normalizeName(name),
            kind = kind,
            language = "kotlin",
            absoluteFilePath = file.absolutePath,
            moduleHint = moduleHint,
            sourceSet = sourceSet,
            visibility = visibility,
            exported = visibility != "private",
            signature = extractSignature(content, lineIndex, startOffset),
            tags = tags + listOf("relative-path:$relativePath"),
            startLine = startLineColumn.first,
            endLine = endLineColumn.first,
            startColumn = startLineColumn.second + 1,
            endColumn = endLineColumn.second + 1,
            startOffset = startOffset,
            endOffset = endOffset,
            containerName = container?.name,
            containerKind = container?.kind,
        )
    }

    private fun declarationKind(declaration: KtClass): String = when {
        declaration.hasModifier(KtTokens.DATA_KEYWORD) -> "data-class"
        declaration.hasModifier(KtTokens.SEALED_KEYWORD) -> "sealed-class"
        declaration.hasModifier(KtTokens.ANNOTATION_KEYWORD) -> "annotation-class"
        declaration.isEnum() -> "enum"
        declaration.isInterface() -> "interface"
        else -> "class"
    }

    private fun extractVisibility(declaration: KtDeclaration): String? = when {
        declaration.hasModifier(KtTokens.PRIVATE_KEYWORD) -> "private"
        declaration.hasModifier(KtTokens.PROTECTED_KEYWORD) -> "protected"
        declaration.hasModifier(KtTokens.INTERNAL_KEYWORD) -> "internal"
        declaration.hasModifier(KtTokens.PUBLIC_KEYWORD) -> "public"
        else -> null
    }

    private fun extractSignature(content: String, lineIndex: LineIndex, startOffset: Int): String {
        val lineText = content.substring(startOffset, lineIndex.lineEndOffset(startOffset))
        return lineText.substringBefore("{").trim().replace(Regex("\\s+"), " ")
    }

    private fun stableId(relativePath: String, kind: String, name: String, line: Int, column: Int): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val source = "$relativePath|$kind|$name|$line|$column"
        return digest.digest(source.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(12)
    }
}

private data class ContainerMeta(
    val name: String,
    val kind: String,
)

private data class SymbolCandidate(
    val id: String,
    val name: String,
    val normalizedName: String,
    val kind: String,
    val language: String,
    val absoluteFilePath: String,
    val moduleHint: String,
    val sourceSet: String,
    val visibility: String?,
    val exported: Boolean,
    val signature: String?,
    val tags: List<String>,
    val startLine: Int,
    val endLine: Int,
    val startColumn: Int,
    val endColumn: Int,
    val startOffset: Int,
    val endOffset: Int,
    val containerName: String?,
    val containerKind: String?,
) {
    fun toRecord(): SymbolRecord = SymbolRecord(
        id = id,
        name = name,
        normalizedName = normalizedName,
        kind = kind,
        language = language,
        filePath = absoluteFilePath,
        lineStart = startLine,
        lineEnd = endLine,
        columnStart = startColumn,
        columnEnd = endColumn,
        containerName = containerName,
        containerKind = containerKind,
        signature = signature,
        exported = exported,
        visibility = visibility,
        moduleHint = moduleHint,
        sourceSet = sourceSet,
        tags = tags,
    )

    fun toContainerMeta(): ContainerMeta = ContainerMeta(name = name, kind = kind)
}

private class LineIndex(private val content: String) {
    private val lineStarts: IntArray = buildList {
        add(0)
        content.forEachIndexed { index, char ->
            if (char == '\n') {
                add(index + 1)
            }
        }
    }.toIntArray()

    fun lineColumn(offset: Int): Pair<Int, Int> {
        var low = 0
        var high = lineStarts.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val start = lineStarts[mid]
            val next = lineStarts.getOrElse(mid + 1) { Int.MAX_VALUE }
            if (offset < start) {
                high = mid - 1
            } else if (offset >= next) {
                low = mid + 1
            } else {
                return (mid + 1) to (offset - start)
            }
        }
        return lineStarts.size to 0
    }

    fun lineEndOffset(offset: Int): Int {
        val (line, _) = lineColumn(offset)
        val nextStart = lineStarts.getOrElse(line) { Int.MAX_VALUE }
        return if (nextStart == Int.MAX_VALUE) content.length else nextStart - 1
    }
}
