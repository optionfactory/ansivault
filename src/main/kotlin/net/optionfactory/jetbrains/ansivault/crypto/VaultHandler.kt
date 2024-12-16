package net.optionfactory.jetbrains.ansivault.crypto

import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultRawContent
import net.optionfactory.jetbrains.ansivault.crypto.decoders.CypherAES256
import net.optionfactory.jetbrains.ansivault.crypto.decoders.CypherFactory
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object VaultHandler {
    val DEFAULT_CYPHER: String = CypherAES256.CYPHER_ID

    const val CHAR_ENCODING: String = "UTF-8"


    @JvmOverloads
    @Throws(IOException::class)
    fun encrypt(cleartext: ByteArray, password: String?, cypher: String = DEFAULT_CYPHER): ByteArray {
        val cypherInstance = CypherFactory.getCypher(cypher)
        val vaultData = cypherInstance!!.encrypt(cleartext, password)
        val vaultDataString = String(vaultData!!)
        val vaultPackage = "${cypherInstance.infoLine()}\n$vaultDataString"
        return vaultPackage.toByteArray()
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun encrypt(clearText: InputStream, cipherText: OutputStream, password: String?, cypher: String = DEFAULT_CYPHER) {
        val clearTextValue = IOUtils.toString(clearText, CHAR_ENCODING)
        cipherText.write(encrypt(clearTextValue.toByteArray(), password, cypher))
    }

    @Throws(IOException::class)
    fun decrypt(encryptedVault: InputStream, decryptedVault: OutputStream, password: String?) {
        val encryptedValue = IOUtils.toString(encryptedVault, CHAR_ENCODING)
        decryptedVault.write(decrypt(encryptedValue.toByteArray(), password))
    }

    @Throws(IOException::class)
    fun decrypt(encrypted: ByteArray, password: String?): ByteArray? {
        val vaultInfo = VaultInfo.fromVaultData(encrypted)
        if (!vaultInfo.isEncryptedVault) {
            throw IOException("File is not an Ansible Encrypted Vault")
        }

        if (!vaultInfo.isValidVault()) {
            throw IOException("The vault is not a format we can handle - check the cypher.")
        }

        val encryptedData = VaultRawContent.getVaultData(encrypted)

        return vaultInfo.cypher.decrypt(encryptedData, password)
    }
}
