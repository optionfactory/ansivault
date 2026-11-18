package net.optionfactory.jetbrains.ansivault.crypto.data

import net.optionfactory.jetbrains.ansivault.crypto.data.HexMarshaller.decode
import net.optionfactory.jetbrains.ansivault.crypto.data.HexMarshaller.encode
import java.io.IOException

/** The vault payload once hex decoded: `hex(salt)\nhex(hmac)\nhex(data)`. */
class VaultContent(val salt: ByteArray, val hmac: ByteArray, val data: ByteArray) {

    fun toByteArray(): ByteArray {
        return toString().toByteArray(Charsets.UTF_8)
    }

    override fun toString(): String {
        val complete = listOf(encode(salt, -1), encode(hmac, -1), encode(data, -1)).joinToString("\n")
        return encode(complete.toByteArray(Charsets.UTF_8), LINE_LENGTH)
    }

    companion object {
        private const val LINE_LENGTH = 80

        @Throws(IOException::class)
        operator fun invoke(encryptedVault: ByteArray): VaultContent {
            val parts = String(encryptedVault, Charsets.UTF_8).split('\n', limit = 3)
            if (parts.size < 2 || parts[0].isEmpty()) {
                throw IOException("Malformed data - salt incomplete")
            }
            if (parts.size < 3 || parts[1].isEmpty()) {
                throw IOException("Malformed data - hmac incomplete")
            }
            try {
                return VaultContent(decode(parts[0]), decode(parts[1]), decode(parts[2]))
            } catch (ex: IllegalArgumentException) {
                throw IOException("Malformed data - not hexadecimal", ex)
            }
        }
    }
}
