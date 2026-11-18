package net.optionfactory.jetbrains.ansivault.crypto.decoders

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.trace
import net.optionfactory.jetbrains.ansivault.crypto.data.HexMarshaller.encode
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultContent
import java.io.IOException
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CypherAES256 : CypherInterface {

    @Throws(IOException::class)
    fun calculateHMAC(key: ByteArray, data: ByteArray): ByteArray {
        try {
            val mac = Mac.getInstance(KEYGEN_ALGO)
            mac.init(SecretKeySpec(key, KEYGEN_ALGO))
            return mac.doFinal(data)
        } catch (ex: Exception) {
            throw IOException("Error calculating HMAC hash: " + ex.message, ex)
        }
    }

    /** Constant time comparison. */
    @Throws(IOException::class)
    fun verifyHMAC(hmac: ByteArray, key: ByteArray, data: ByteArray): Boolean {
        return MessageDigest.isEqual(hmac, calculateHMAC(key, data))
    }

    @Throws(IOException::class)
    private fun aes(mode: Int, data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        try {
            val cipher = Cipher.getInstance(CYPHER_ALGO)
            cipher.init(mode, SecretKeySpec(key, CYPHER_KEY_ALGO), IvParameterSpec(iv))
            return cipher.doFinal(data)
        } catch (ex: Exception) {
            val operation = if (mode == Cipher.ENCRYPT_MODE) "encrypt" else "decrypt"
            throw IOException("Failed to $operation data: " + ex.message, ex)
        }
    }

    @Throws(IOException::class)
    override fun decrypt(encryptedData: ByteArray, password: String): ByteArray {
        val vaultContent = VaultContent(encryptedData)
        val cypher = vaultContent.data
        logger.debug("Salt: ${vaultContent.salt.size} bytes, HMAC: ${vaultContent.hmac.size} bytes, data: ${cypher.size} bytes")

        val keys = EncryptionKeychain(vaultContent.salt, password, KEYLEN, IVLEN, ITERATIONS, KEYGEN_ALGO)
        traceKeys(keys)

        if (!verifyHMAC(vaultContent.hmac, keys.hmacKey, cypher)) {
            throw WrongPasswordException("HMAC Digest doesn't match - possibly it's the wrong password.")
        }
        logger.debug("Signature matches - decrypting")
        val decrypted = unpad(aes(Cipher.DECRYPT_MODE, cypher, keys.encryptionKey, keys.iv))
        logger.trace { "Decoded:\n${String(decrypted, Charsets.UTF_8)}" }
        return decrypted
    }

    @Throws(IOException::class)
    override fun encrypt(data: ByteArray, password: String): ByteArray {
        val keys = EncryptionKeychain(SALT_LENGTH, password, KEYLEN, IVLEN, ITERATIONS, KEYGEN_ALGO)
        traceKeys(keys)
        val encrypted = aes(Cipher.ENCRYPT_MODE, pad(data), keys.encryptionKey, keys.iv)
        val hmacHash = calculateHMAC(keys.hmacKey, encrypted)
        return VaultContent(keys.salt, hmacHash, encrypted).toByteArray()
    }

    private fun traceKeys(keys: EncryptionKeychain) {
        logger.trace { "Key 1: ${keys.encryptionKey.size} - ${encode(keys.encryptionKey, 100)}" }
        logger.trace { "Key 2: ${keys.hmacKey.size} - ${encode(keys.hmacKey, 100)}" }
        logger.trace { "IV: ${keys.iv.size} - ${encode(keys.iv, 100)}" }
    }

    companion object {
        private val logger: Logger = Logger.getInstance(CypherAES256::class.java)

        const val CYPHER_ID: String = "AES256"
        const val KEYGEN_ALGO: String = "HmacSHA256"
        const val CYPHER_KEY_ALGO: String = "AES"
        const val CYPHER_ALGO: String = "AES/CTR/NoPadding"

        /** ansible pads the clear text with PKCS#7 to the AES block size, even if CTR does not need it. */
        const val BLOCK_SIZE: Int = 16
        private const val SALT_LENGTH = 32
        const val KEYLEN: Int = 32
        const val IVLEN: Int = 16
        const val ITERATIONS: Int = 10000

        fun pad(cleartext: ByteArray): ByteArray {
            val paddingLength = BLOCK_SIZE - (cleartext.size % BLOCK_SIZE)
            return cleartext + ByteArray(paddingLength) { paddingLength.toByte() }
        }

        @Throws(IOException::class)
        fun unpad(decrypted: ByteArray): ByteArray {
            if (decrypted.isEmpty()) {
                return decrypted
            }
            val paddingLength = decrypted.last().toInt() and 0xFF
            val valid = paddingLength in 1..BLOCK_SIZE
                && paddingLength <= decrypted.size
                && (decrypted.size - paddingLength until decrypted.size).all { decrypted[it].toInt() and 0xFF == paddingLength }
            if (!valid) {
                throw IOException("Invalid vault padding")
            }
            return decrypted.copyOfRange(0, decrypted.size - paddingLength)
        }
    }
}
