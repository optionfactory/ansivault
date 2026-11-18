package net.optionfactory.jetbrains.ansivault.crypto.decoders

import java.io.IOException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Keys derived with PBKDF2 from the vault password: encryption key, HMAC key and IV, in this order. */
class EncryptionKeychain(
    val salt: ByteArray,
    password: String,
    keylen: Int,
    ivlen: Int,
    iterations: Int,
    algo: String,
) {
    val encryptionKey: ByteArray
    val hmacKey: ByteArray
    val iv: ByteArray

    constructor(saltLen: Int, password: String, keylen: Int, ivlen: Int, iterations: Int, algo: String) :
        this(generateSalt(saltLen), password, keylen, ivlen, iterations, algo)

    init {
        val rawKeys = deriveKey(password, salt, iterations, algo, 2 * keylen + ivlen)
        encryptionKey = rawKeys.copyOfRange(0, keylen)
        hmacKey = rawKeys.copyOfRange(keylen, 2 * keylen)
        iv = rawKeys.copyOfRange(2 * keylen, 2 * keylen + ivlen)
        rawKeys.fill(0)
    }

    companion object {
        private val random = SecureRandom()

        private fun generateSalt(length: Int): ByteArray {
            return ByteArray(length).also { random.nextBytes(it) }
        }

        /** The JDK PBKDF2 encodes the password as UTF-8, like ansible. */
        @Throws(IOException::class)
        private fun deriveKey(password: String, salt: ByteArray, iterations: Int, algo: String, length: Int): ByteArray {
            val chars = password.toCharArray()
            val spec = PBEKeySpec(chars, salt, iterations, length * 8)
            try {
                return SecretKeyFactory.getInstance("PBKDF2With$algo").generateSecret(spec).encoded
            } catch (ex: Exception) {
                throw IOException("Cryptofailure: " + ex.message, ex)
            } finally {
                spec.clearPassword()
                chars.fill('\u0000')
            }
        }
    }
}
