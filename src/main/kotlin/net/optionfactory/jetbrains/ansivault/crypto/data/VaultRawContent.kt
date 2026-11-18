package net.optionfactory.jetbrains.ansivault.crypto.data

import net.optionfactory.jetbrains.ansivault.crypto.data.HexMarshaller.decode
import java.io.IOException

object VaultRawContent {
    /** The hex decoded body of a vault (everything after the header line). */
    @Throws(IOException::class)
    fun getVaultData(data: ByteArray): ByteArray {
        val body = String(data, Charsets.UTF_8).substringAfter('\n', "")
        try {
            return decode(body)
        } catch (ex: IllegalArgumentException) {
            throw IOException("Malformed vault - the body is not hexadecimal", ex)
        }
    }
}
