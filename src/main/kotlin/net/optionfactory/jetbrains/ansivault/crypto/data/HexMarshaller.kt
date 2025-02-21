package net.optionfactory.jetbrains.ansivault.crypto.data

import com.intellij.openapi.diagnostic.Logger

object HexMarshaller {
    private const val DEFAULT_LINE_LENGTH = 80

    val logger = Logger.getInstance(HexMarshaller::class.java)

    @OptIn(ExperimentalStdlibApi::class)
    fun decode(encoded: String): ByteArray {
        return encoded.trimIndent().replace("\n", "").hexToByteArray()
    }

    fun encode(clear: ByteArray): String {
        return encode(clear, DEFAULT_LINE_LENGTH)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun encode(clear: ByteArray, lineLength: Int): String {
        val format = HexFormat {
            upperCase = true
            bytes {
                bytesPerLine = if (lineLength <= 0) Int.MAX_VALUE else lineLength / 2
            }
        }
        return clear.toHexString(format)
    }

}
