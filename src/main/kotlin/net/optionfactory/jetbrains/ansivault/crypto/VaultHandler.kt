package net.optionfactory.jetbrains.ansivault.crypto

import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultRawContent
import net.optionfactory.jetbrains.ansivault.crypto.decoders.CypherAES256
import net.optionfactory.jetbrains.ansivault.crypto.decoders.CypherFactory
import java.io.IOException

object VaultHandler {
    const val DEFAULT_CYPHER: String = CypherAES256.CYPHER_ID

    @Throws(IOException::class)
    fun encrypt(cleartext: ByteArray, password: String, cypher: String = DEFAULT_CYPHER, vaultId: String? = null): ByteArray {
        val vaultData = String(CypherFactory.getCypher(cypher).encrypt(cleartext, password), Charsets.UTF_8)
        return "${VaultInfo.vaultInfoForCypher(cypher, vaultId)}\n$vaultData".toByteArray(Charsets.UTF_8)
    }

    @Throws(IOException::class)
    fun decrypt(encrypted: ByteArray, password: String): ByteArray {
        val vaultInfo = VaultInfo.fromVaultData(encrypted)
        if (!vaultInfo.isEncryptedVault) {
            throw IOException("File is not an Ansible Encrypted Vault")
        }
        if (!vaultInfo.isValidVault()) {
            throw IOException("Unsupported vault format version '${vaultInfo.vaultVersion}'")
        }
        return vaultInfo.cypher.decrypt(VaultRawContent.getVaultData(encrypted), password)
    }
}
