package net.optionfactory.jetbrains.ansivault.gutter

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo

data class VaultBlock(
    val startOffset: Int,
    val endOffset: Int,
    val indent: Int,
    val vaultText: String,
)

object VaultBlocks {
    private val HEX_LINE = Regex("^[0-9a-fA-F]+$")
    private val KEY_LINE = Regex("""^(\s*)(?:-\s+)?["']?([^"':#\s][^"':#]*?)["']?\s*:(?:\s|$)""")

    fun headerLines(document: Document): List<Int> {
        return (0 until document.lineCount).filter { line ->
            VaultInfo(lineText(document, line).trim()).isEncryptedVault
        }
    }

    fun at(document: Document, headerLine: Int): VaultBlock? {
        val rawHeader = lineText(document, headerLine)
        val header = rawHeader.trim()
        if (!VaultInfo(header).isEncryptedVault) {
            return null
        }
        val body = mutableListOf(header)
        var lastLine = headerLine
        for (line in headerLine + 1 until document.lineCount) {
            val text = lineText(document, line).trim()
            if (!HEX_LINE.matches(text)) {
                break
            }
            body.add(text)
            lastLine = line
        }
        return VaultBlock(
            startOffset = document.getLineStartOffset(headerLine),
            endOffset = document.getLineEndOffset(lastLine),
            indent = rawHeader.takeWhile { it.isWhitespace() }.length,
            vaultText = body.joinToString("\n"),
        )
    }

    fun lines(document: Document): List<String> {
        return (0 until document.lineCount).map { lineText(document, it) }
    }

    fun keyPath(lines: List<String>, headerLine: Int): String {
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

    private fun lineText(document: Document, line: Int): String {
        return document.getText(TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)))
    }
}
