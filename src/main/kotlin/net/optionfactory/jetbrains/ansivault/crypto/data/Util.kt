package net.optionfactory.jetbrains.ansivault.crypto.data

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Util {
    private const val DEFAULT_LINE_LENGTH = 80

    private val logger: Logger = LoggerFactory.getLogger(Util::class.java)

    internal val hexArray: CharArray = "0123456789ABCDEF".toCharArray()
    internal const val LINE_BREAK: String = "\n"
    internal const val CHAR_ENCODING: String = "UTF-8"

    @OptIn(ExperimentalStdlibApi::class)
    fun unhex(hexed: String): ByteArray {
        return hexed.hexToByteArray()
    }

    fun hexit(unhexed: ByteArray): String {
        return hexit(unhexed, DEFAULT_LINE_LENGTH)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun hexit(unhexed: ByteArray, lineLength: Int): String {
        val format = HexFormat {
            upperCase = true
            bytes {
                bytesPerLine = if (lineLength <= 0) Int.MAX_VALUE else lineLength / 2
            }
        }
        return unhexed.toHexString(format)
    }

    fun getVaultInfo(vaultData: String): VaultInfo {
        val infoString = vaultData.substring(0, vaultData.indexOf(LINE_BREAK))
        return VaultInfo(infoString)
    }

    fun getVaultInfo(vaultData: ByteArray): VaultInfo {
        return getVaultInfo(String(vaultData))
    }

    fun getVaultData(vaultData: String): ByteArray {
        val rawData = vaultData.lines().drop(1).dropLastWhile { it.isEmpty() }.joinToString(separator = "")
        return unhex(rawData)
    }

    fun getVaultData(vaultData: ByteArray): ByteArray {
        return getVaultData(String(vaultData))
    }
}
