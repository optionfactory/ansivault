package net.optionfactory.jetbrains.ansivault.gutter

import com.intellij.openapi.editor.Document
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo

data class VaultBlock(
    val startOffset: Int,
    val endOffset: Int,
    val indent: Int,
    val vaultText: String,
)

/** A vault found in a list of lines: from [headerLine] to [lastLine], both included. */
data class VaultLines(
    val headerLine: Int,
    val lastLine: Int,
    val indent: Int,
    val vaultText: String,
)

object VaultBlocks {
    private val HEX_LINE = Regex("^[0-9a-fA-F]+$")
    private val KEY_LINE = Regex("""^(\s*)(?:-\s+)?["']?([^"':#\s][^"':#]*?)["']?\s*:(?:\s|$)""")

    fun isHeader(line: CharSequence): Boolean {
        val text = line.trim()
        // cheap check first: this runs on every line of every open file
        return text.startsWith(VaultInfo.VAULT_MAGIC) && VaultInfo(text.toString()).isEncryptedVault
    }

    fun headerLines(lines: List<CharSequence>): List<Int> {
        return lines.indices.filter { isHeader(lines[it]) }
    }

    fun at(lines: List<CharSequence>, headerLine: Int): VaultLines? {
        val rawHeader = lines[headerLine]
        if (!isHeader(rawHeader)) {
            return null
        }
        val body = mutableListOf(rawHeader.trim().toString())
        var lastLine = headerLine
        for (line in headerLine + 1 until lines.size) {
            val text = lines[line].trim()
            if (!HEX_LINE.matches(text)) {
                break
            }
            body.add(text.toString())
            lastLine = line
        }
        return VaultLines(
            headerLine = headerLine,
            lastLine = lastLine,
            indent = rawHeader.takeWhile { it.isWhitespace() }.length,
            vaultText = body.joinToString("\n"),
        )
    }

    /** The dotted path of YAML keys enclosing [headerLine], e.g. `db.credentials.password`. */
    fun keyPath(lines: List<CharSequence>, headerLine: Int): String {
        val keys = mutableListOf<String>()
        var currentIndent = lines[headerLine].takeWhile { it.isWhitespace() }.length
        for (line in headerLine downTo 0) {
            val text = lines[line]
            if (text.isBlank() || text.trimStart().startsWith("#")) {
                continue
            }
            val match = KEY_LINE.find(text) ?: continue
            val indent = match.groupValues[1].length
            if (indent < currentIndent) {
                keys.add(0, match.groupValues[2].trim())
                currentIndent = indent
            }
        }
        return keys.joinToString(".")
    }

    fun headerLines(document: Document): List<Int> = headerLines(lines(document))

    fun at(document: Document, headerLine: Int): VaultBlock? {
        val vault = at(lines(document), headerLine) ?: return null
        return VaultBlock(
            startOffset = document.getLineStartOffset(vault.headerLine),
            endOffset = document.getLineEndOffset(vault.lastLine),
            indent = vault.indent,
            vaultText = vault.vaultText,
        )
    }

    /** The lines of [document], read lazily without copying the text. */
    fun lines(document: Document): List<CharSequence> {
        val text = document.immutableCharSequence
        val lineCount = document.lineCount
        return object : AbstractList<CharSequence>() {
            override val size: Int = lineCount
            override fun get(index: Int): CharSequence =
                text.subSequence(document.getLineStartOffset(index), document.getLineEndOffset(index))
        }
    }
}
