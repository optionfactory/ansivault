package net.optionfactory.jetbrains.ansivault.crypto.decoders

import de.rtner.security.auth.spi.PBKDF2Engine
import de.rtner.security.auth.spi.PBKDF2Parameters
import java.io.IOException
import java.util.*

class EncryptionKeychain {
    private val password: String?
    val salt: ByteArray
    private val keylen: Int
    private val ivlen: Int
    private val iterations: Int
    private val algo: String

    var encryptionKey: ByteArray
        private set
    var hmacKey: ByteArray
        private set
    var iv: ByteArray
        private set


    constructor(salt: ByteArray, password: String, keylen: Int, ivlen: Int, iterations: Int, algo: String) {
        this.password = password
        this.salt = salt
        this.keylen = keylen
        this.ivlen = ivlen
        this.iterations = iterations
        this.algo = algo
        val rawkeys = createRawKey()
        this.encryptionKey = getEncryptionKey(rawkeys)
        this.hmacKey = getHMACKey(rawkeys)
        this.iv = getIV(rawkeys)
    }

    constructor(saltLen: Int, password: String?, keylen: Int, ivlen: Int, iterations: Int, algo: String) {
        this.password = password
        this.salt = generateSalt(saltLen)
        this.keylen = keylen
        this.ivlen = ivlen
        this.iterations = iterations
        this.algo = algo
        val rawkeys = createRawKey()
        this.encryptionKey = getEncryptionKey(rawkeys)
        this.hmacKey = getHMACKey(rawkeys)
        this.iv = getIV(rawkeys)
    }


    @Throws(IOException::class)
    private fun createRawKey(): ByteArray {
        try {
            val params = PBKDF2Parameters(algo, CHAR_ENCODING, salt, iterations)
            val keylength = ivlen + 2 * keylen
            val pbkdf2Engine = PBKDF2Engine(params)
            val keys = pbkdf2Engine.deriveKey(password, keylength)
            return keys
        } catch (ex: Exception) {
            throw IOException("Cryptofailure: " + ex.message)
        }
    }


    private fun getEncryptionKey(keys: ByteArray): ByteArray {
        val result = Arrays.copyOfRange(keys, 0, keylen)
        return result
    }

    private fun getHMACKey(keys: ByteArray): ByteArray {
        val result = Arrays.copyOfRange(keys, keylen, keylen * 2)
        return result
    }

    private fun getIV(keys: ByteArray): ByteArray {
        val result = Arrays.copyOfRange(keys, keylen * 2, keylen * 2 + ivlen)
        return result
    }

    private fun generateSalt(length: Int): ByteArray {
        val salt = ByteArray(length)
        Random().nextBytes(salt)
        return salt
    }


    companion object {
        private const val CHAR_ENCODING = "UTF-8"
    }
}
