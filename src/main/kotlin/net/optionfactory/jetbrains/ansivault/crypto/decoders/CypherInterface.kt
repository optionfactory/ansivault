package net.optionfactory.jetbrains.ansivault.crypto.decoders

import java.io.IOException

interface CypherInterface {
    @Throws(IOException::class)
    fun decrypt(encryptedData: ByteArray, password: String): ByteArray

    @Throws(IOException::class)
    fun encrypt(data: ByteArray, password: String): ByteArray
}
