package net.optionfactory.jetbrains.ansivault.crypto.data

import net.optionfactory.jetbrains.ansivault.crypto.data.HexMarshaller.decode

object VaultRawContent {
    fun getVaultData(data: ByteArray): ByteArray {
        val rawData = String(data).lines().drop(1).dropLastWhile { it.isEmpty() }.joinToString(separator = "")
        return decode(rawData)
    }
}
