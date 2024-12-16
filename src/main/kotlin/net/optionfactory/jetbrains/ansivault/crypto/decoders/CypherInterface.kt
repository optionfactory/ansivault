package net.optionfactory.jetbrains.ansivault.crypto.decoders

import java.io.IOException
import java.io.OutputStream

interface CypherInterface {
    @Throws(IOException::class)
    fun decrypt(decodedStream: OutputStream, data: ByteArray, password: String?)

    @Throws(IOException::class)
    fun decrypt(encryptedData: ByteArray, password: String?): ByteArray?

    @Throws(IOException::class)
    fun encrypt(encodedStream: OutputStream, data: ByteArray, password: String?)

    @Throws(IOException::class)
    fun encrypt(data: ByteArray, password: String?): ByteArray?
    fun infoLine(): String
}
