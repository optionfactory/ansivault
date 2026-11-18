package net.optionfactory.jetbrains.ansivault.crypto.data

@OptIn(ExperimentalStdlibApi::class)
object HexMarshaller {
    private const val DEFAULT_LINE_LENGTH = 80

    /** Decodes hex text, ignoring whitespace and line breaks. */
    fun decode(encoded: String): ByteArray {
        return encoded.filterNot { it.isWhitespace() }.hexToByteArray()
    }

    /** Encodes as lower case hex (like ansible), in lines of [lineLength] chars; a non positive [lineLength] means one line. */
    fun encode(clear: ByteArray, lineLength: Int = DEFAULT_LINE_LENGTH): String {
        val format = HexFormat {
            bytes {
                bytesPerLine = if (lineLength <= 0) Int.MAX_VALUE else lineLength / 2
            }
        }
        return clear.toHexString(format)
    }
}
