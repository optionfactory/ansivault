package net.optionfactory.jetbrains.ansivault.crypto.decoders

import com.intellij.openapi.diagnostic.Logger
import net.optionfactory.jetbrains.ansivault.crypto.data.Util.hexit
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultContent
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import java.io.IOException
import java.io.OutputStream
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CypherAES256 : CypherInterface {
    var logger: Logger = Logger.getInstance(CypherAES256::class.java)

    private fun hasValidAESProvider(): Boolean {
        var canCrypt = false
        try {
            val maxKeyLen = Cipher.getMaxAllowedKeyLength(CYPHER_ALGO)
            logger.debug("Available keylen: {}", maxKeyLen)
            if (maxKeyLen >= AES_KEYLEN) {
                canCrypt = true
            } else {
                logger.warn(
                    "JRE doesn't support $AES_KEYLEN keylength for $CYPHER_KEY_ALGO\nInstall unrestricted policy files from:\n$JDK8_UPF_URL"
                )
            }
        } catch (ex: Exception) {
            logger.warn("Failed to check for proper cypher algorithms", ex)
        }
        return canCrypt
    }

    @Throws(IOException::class)
    fun calculateHMAC(key: ByteArray, data: ByteArray?): ByteArray? {
        var computedMac: ByteArray? = null

        try {
            val hmacKey = SecretKeySpec(key, KEYGEN_ALGO)
            val mac = Mac.getInstance(KEYGEN_ALGO)
            mac.init(hmacKey)
            computedMac = mac.doFinal(data)
        } catch (ex: Exception) {
            throw IOException("Error decrypting HMAC hash: " + ex.message)
        }

        return computedMac
    }

    @Throws(IOException::class)
    fun verifyHMAC(hmac: ByteArray?, key: ByteArray, data: ByteArray?): Boolean {
        val matches = false
        val calculated = calculateHMAC(key, data)
        return hmac.contentEquals(calculated)
    }

    fun paddingLength(decrypted: ByteArray): Int {
        if (decrypted.size == 0) {
            logger.debug("Empty decoded text has no padding.")
            return 0
        }

        logger.debug("Padding length: {}", decrypted[decrypted.size - 1])
        return decrypted[decrypted.size - 1].toInt()
    }

    fun unpad(decrypted: ByteArray): ByteArray {
        val length = decrypted.size - paddingLength(decrypted)
        return Arrays.copyOfRange(decrypted, 0, length)
    }

    @Throws(IOException::class)
    fun pad(cleartext: ByteArray): ByteArray? {
        var padded: ByteArray? = null

        try {
            val blockSize = Cipher.getInstance(CYPHER_ALGO).blockSize
            logger.debug("Padding to block size: {}", blockSize)
            var padding_length = (blockSize - (cleartext.size % blockSize))
            if (padding_length == 0) {
                padding_length = blockSize
            }
            padded = cleartext.copyOf(cleartext.size + padding_length)
            padded[padded.size - 1] = padding_length.toByte()
        } catch (ex: Exception) {
            IOException("Error calculating padding for " + CYPHER_ALGO + ": " + ex.message)
        }

        return padded
    }

    @Throws(IOException::class)
    fun decryptAES(cypher: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(key, CYPHER_KEY_ALGO)
        val ivSpec = IvParameterSpec(iv)
        try {
            val cipher = Cipher.getInstance(CYPHER_ALGO)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decrypted = cipher.doFinal(cypher)
            return unpad(decrypted)
        } catch (ex: Exception) {
            throw IOException("Failed to decrypt data: " + ex.message)
        }
    }

    @Throws(IOException::class)
    fun encryptAES(cleartext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(key, CYPHER_KEY_ALGO)
        val ivSpec = IvParameterSpec(iv)
        try {
            val cipher = Cipher.getInstance(CYPHER_ALGO)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(cleartext)
            return encrypted
        } catch (ex: Exception) {
            throw IOException("Failed to encrypt data: " + ex.message)
        }
    }

    @Throws(IOException::class)
    override fun decrypt(encryptedData: ByteArray, password: String?): ByteArray? {
        var decrypted: ByteArray? = null

        if (!hasValidAESProvider()) {
            throw IOException("Missing valid AES256 provider - install unrestricted policy profiles.")
        }

        val vaultContent = VaultContent(encryptedData)

        val salt = vaultContent.salt
        val hmac = vaultContent.hmac
        val cypher = vaultContent.data
        logger.debug("Salt: {} - {}", salt!!.size, hexit(salt, 100))
        logger.debug("HMAC: {} - {}", hmac!!.size, hexit(hmac, 100))
        logger.debug("Data: {} - {}", cypher!!.size, hexit(cypher, 100))

        val keys = EncryptionKeychain(salt, password, KEYLEN, IVLEN, ITERATIONS, KEYGEN_ALGO)

        val cypherKey = keys.encryptionKey
        logger.debug(
            "Key 1: {} - {}", cypherKey!!.size, hexit(
                cypherKey, 100
            )
        )
        val hmacKey = keys.hmacKey
        logger.debug(
            "Key 2: {} - {}", hmacKey!!.size, hexit(
                hmacKey, 100
            )
        )
        val iv = keys.iv
        logger.debug("IV: {} - {}", iv!!.size, hexit(iv, 100))

        if (verifyHMAC(hmac, hmacKey, cypher)) {
            logger.debug("Signature matches - decrypting")
            decrypted = decryptAES(cypher, cypherKey, iv)
            logger.debug("Decoded:\n{}", String(decrypted, charset(CHAR_ENCODING)))
        } else {
            throw IOException("HMAC Digest doesn't match - possibly it's the wrong password.")
        }

        return decrypted
    }

    @Throws(IOException::class)
    override fun decrypt(decodedStream: OutputStream, encryptedData: ByteArray, password: String?) {
        decodedStream.write(decrypt(encryptedData, password))
    }

    @Throws(IOException::class)
    override fun encrypt(encodedStream: OutputStream, data: ByteArray, password: String?) {
        encodedStream.write(encrypt(data, password))
    }

    override fun infoLine(): String {
        return VaultInfo.Companion.vaultInfoForCypher(CYPHER_ID)
    }

    @Throws(IOException::class)
    override fun encrypt(data: ByteArray, password: String?): ByteArray? {
        var data = data
        val keys = EncryptionKeychain(SALT_LENGTH, password, KEYLEN, IVLEN, ITERATIONS, KEYGEN_ALGO)
        val cypherKey = keys.encryptionKey
        logger.debug(
            "Key 1: {} - {}", cypherKey!!.size, hexit(
                cypherKey, 100
            )
        )
        val hmacKey = keys.hmacKey
        logger.debug(
            "Key 2: {} - {}", hmacKey!!.size, hexit(
                hmacKey, 100
            )
        )
        val iv = keys.iv
        logger.debug("IV: {} - {}", iv!!.size, hexit(iv, 100))
        logger.debug("Original data length: {}", data.size)
        data = pad(data)!!
        logger.debug("Padded data length: {}", data.size)
        val encrypted = encryptAES(data, keys.encryptionKey, keys.iv)
        val hmacHash = calculateHMAC(keys.hmacKey, encrypted)
        val vaultContent = VaultContent(keys.salt, hmacHash, encrypted)
        return vaultContent.toByteArray()
    }

    companion object {
        const val CYPHER_ID: String = "AES256"
        const val AES_KEYLEN: Int = 256
        const val CHAR_ENCODING: String = "UTF-8"
        const val KEYGEN_ALGO: String = "HmacSHA256"
        const val CYPHER_KEY_ALGO: String = "AES"
        const val CYPHER_ALGO: String = "AES/CTR/NoPadding"
        private const val JDK8_UPF_URL =
            "http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html"

        private const val SALT_LENGTH = 32
        const val KEYLEN: Int = 32
        const val IVLEN: Int = 16
        const val ITERATIONS: Int = 10000
    }
}
