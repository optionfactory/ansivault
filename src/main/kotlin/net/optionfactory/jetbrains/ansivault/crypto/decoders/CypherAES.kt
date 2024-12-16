package net.optionfactory.jetbrains.ansivault.crypto.decoders

import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import java.io.IOException
import java.io.OutputStream

class CypherAES : CypherInterface {
    @Throws(IOException::class)
    override fun decrypt(decodedStream: OutputStream, data: ByteArray, password: String?) {
        throw IOException(CYPHER_ID + " is not implemented.")
    }

    @Throws(IOException::class)
    override fun decrypt(data: ByteArray, password: String?): ByteArray? {
        throw IOException(CYPHER_ID + " is not implemented.")
    }

    @Throws(IOException::class)
    override fun encrypt(encodedStream: OutputStream, data: ByteArray, password: String?) {
        throw IOException(CYPHER_ID + " is not implemented.")
    }

    @Throws(IOException::class)
    override fun encrypt(data: ByteArray, password: String?): ByteArray? {
        throw IOException(CYPHER_ID + " is not implemented.")
    }

    override fun infoLine(): String {
        return VaultInfo.Companion.vaultInfoForCypher(CYPHER_ID)
    }

    companion object {
        const val CYPHER_ID: String = "AES"
    }
}
