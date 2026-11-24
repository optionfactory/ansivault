package net.optionfactory.jetbrains.ansivault.actions

/**
 * A `key: value` YAML line whose value is a single line scalar, plain or quoted, optionally followed by a comment.
 * Offsets are relative to the line start.
 */
data class YamlValueLine(
    /** Column of the key: a block scalar replacing the value must be indented more than this. */
    val keyColumn: Int,
    val valueStart: Int,
    /** The scalar value, unquoted. */
    val value: String,
    /** The trailing comment, with its `#`, or null. */
    val comment: String?,
) {
    companion object {
        private val KEY_VALUE = Regex("""^(\s*(?:-\s+)?)("(?:[^"\\]|\\.)*"|'(?:[^']|'')*'|[^\s#'"\-?:,\[\]{}&*!|>%@`][^#]*?)(\s*:\s+)(\S.*)$""")
        /** Values that are not a single line scalar: block scalars, tags (e.g. an existing `!vault`), anchors, aliases, flow collections. */
        private const val NOT_SCALAR_START = "|>!&*[{"

        fun parse(line: String): YamlValueLine? {
            val match = KEY_VALUE.find(line.trimEnd()) ?: return null
            val keyColumn = match.groupValues[1].length
            val valueStart = match.groups[4]!!.range.first
            val raw = match.groupValues[4]
            val (value, rest) = when (raw.first()) {
                '"' -> doubleQuoted(raw) ?: return null
                '\'' -> singleQuoted(raw) ?: return null
                in NOT_SCALAR_START -> return null
                else -> plain(raw)
            }
            val comment = rest.trim().ifEmpty { null }
            if (comment != null && !comment.startsWith("#")) {
                return null
            }
            return YamlValueLine(keyColumn, valueStart, value, comment)
        }

        /** A plain scalar ends at the first ` #`. */
        private fun plain(raw: String): Pair<String, String> {
            val commentAt = Regex("""\s#""").find(raw)?.range?.first ?: raw.length
            return raw.substring(0, commentAt).trim() to raw.substring(commentAt)
        }

        private fun doubleQuoted(raw: String): Pair<String, String>? {
            val value = StringBuilder()
            var i = 1
            while (i < raw.length) {
                when (val c = raw[i]) {
                    '"' -> return value.toString() to raw.substring(i + 1)
                    '\\' -> {
                        val next = raw.getOrNull(++i) ?: return null
                        value.append(
                            when (next) {
                                'n' -> '\n'
                                't' -> '\t'
                                'r' -> '\r'
                                '0' -> '\u0000'
                                '"', '\\', '/', ' ' -> next
                                else -> return null
                            }
                        )
                    }
                    else -> value.append(c)
                }
                i++
            }
            return null
        }

        private fun singleQuoted(raw: String): Pair<String, String>? {
            val value = StringBuilder()
            var i = 1
            while (i < raw.length) {
                if (raw[i] == '\'') {
                    if (raw.getOrNull(i + 1) == '\'') {
                        value.append('\'')
                        i += 2
                        continue
                    }
                    return value.toString() to raw.substring(i + 1)
                }
                value.append(raw[i++])
            }
            return null
        }
    }
}
