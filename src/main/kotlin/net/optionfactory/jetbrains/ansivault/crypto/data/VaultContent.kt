package net.optionfactory.jetbrains.ansivault.crypto.data

import com.intellij.openapi.diagnostic.Logger
import net.optionfactory.jetbrains.ansivault.crypto.data.Util.hexit
import net.optionfactory.jetbrains.ansivault.crypto.data.Util.unhex
import java.io.IOException

class VaultContent {
    var logger: Logger = Logger.getInstance(VaultContent::class.java)

    val salt: ByteArray?
    val hmac: ByteArray?
    val data: ByteArray

    constructor(encryptedVault: ByteArray) {
        val vaultContents = splitData(encryptedVault)
        salt = unhex(String(vaultContents[0]!!, charset(CHAR_ENCODING)))
        hmac = unhex(String(vaultContents[1]!!, charset(CHAR_ENCODING)))
        data = unhex(String(vaultContents[2]!!, charset(CHAR_ENCODING)))
    }

    constructor(salt: ByteArray?, hmac: ByteArray?, data: ByteArray) {
        this.salt = salt
        this.hmac = hmac
        this.data = data
    }

    fun toByteArray(): ByteArray {
        return toString().toByteArray()
    }

    override fun toString(): String {
        logger.debug(
            "Salt: {} - HMAC: {} - Data: {} - TargetLen: {}",
            salt!!.size, hmac!!.size, data.size, (salt.size + hmac.size + data.size) * 2
        )
        val saltString = hexit(salt)
        logger.debug("Salt String Length: {}", saltString.length)
        val hmacString = hexit(hmac)
        logger.debug("HMAC String Length: {}", hmacString.length)
        val dataString = hexit(data, -1)
        logger.debug("DATA String Length: {}", dataString.length)
        val complete = """
            $saltString
            $hmacString
            $dataString
            """.trimIndent()
        logger.debug("Complete: {} \n{}", complete.length, complete)
        val result = hexit(complete.toByteArray(), 80)
        logger.debug("Result: [{}] {}\n{}", complete.length * 2, result.length, result)
        return result
    }

    @Throws(IOException::class)
    private fun getDataLengths(encodedData: ByteArray): IntArray {
        val result = IntArray(3)

        var idx = 0
        var saltLen = 0
        while (idx < encodedData.size && encodedData[idx] != '\n'.code.toByte()) {
            saltLen++
            idx++
        }
        // Skip the newline
        idx++
        if (idx == encodedData.size) {
            throw IOException("Malformed data - salt incomplete")
        }
        result[0] = saltLen

        var hmacLen = 0
        while (idx < encodedData.size && encodedData[idx] != '\n'.code.toByte()) {
            hmacLen++
            idx++
        }
        // Skip the newline
        idx++
        if (idx == encodedData.size) {
            throw IOException("Malformed data - hmac incomplete")
        }
        result[1] = hmacLen
        var dataLen = 0
        while (idx < encodedData.size) {
            dataLen++
            idx++
        }
        result[2] = dataLen

        return result
    }

    @Throws(IOException::class)
    private fun splitData(encodedData: ByteArray): Array<ByteArray?> {
        val partsLength = getDataLengths(encodedData)

        val result = arrayOfNulls<ByteArray>(3)

        var idx = 0
        var saltIdx = 0
        result[0] = ByteArray(partsLength[0])
        while (idx < encodedData.size && encodedData[idx] != '\n'.code.toByte()) {
            result[0]!![saltIdx++] = encodedData[idx++]
        }
        // Skip the newline
        idx++
        if (idx == encodedData.size) {
            throw IOException("Malformed data - salt incomplete")
        }
        var macIdx = 0
        result[1] = ByteArray(partsLength[1])
        while (idx < encodedData.size && encodedData[idx] != '\n'.code.toByte()) {
            result[1]!![macIdx++] = encodedData[idx++]
        }
        // Skip the newline
        idx++
        if (idx == encodedData.size) {
            throw IOException("Malformed data - hmac incomplete")
        }
        var dataIdx = 0
        result[2] = ByteArray(partsLength[2])
        while (idx < encodedData.size) {
            result[2]!![dataIdx++] = encodedData[idx++]
        }
        return result
    }

    companion object {
        private const val CHAR_ENCODING = "UTF-8"
    }
}
