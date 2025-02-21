package net.optionfactory.jetbrains.ansivault.crypto.data

import com.intellij.openapi.diagnostic.Logger
import net.optionfactory.jetbrains.ansivault.crypto.decoders.CypherFactory
import net.optionfactory.jetbrains.ansivault.crypto.decoders.CypherInterface

class VaultInfo(infoLine: String) {
    private var logger: Logger = Logger.getInstance(VaultInfo::class.java)

    var isEncryptedVault: Boolean = false
        private set
    var vaultVersion: String? = null
        private set
    private var vaultCypher: String? = null

    init {
        logger.debug("Ansible Vault info: {}", infoLine)

        val infoParts = infoLine.split(INFO_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (infoParts.size == INFO_ELEMENTS) {
            if (infoParts[MAGIC_PART] == VAULT_MAGIC) {
                isEncryptedVault = true
                vaultVersion = infoParts[VERSION_PART]
                vaultCypher = infoParts[CYPHER_PART]
            }
        }
    }

    val cypher: CypherInterface
        get() = CypherFactory.getCypher(vaultCypher!!)

    fun isValidVault(): Boolean {
        return isEncryptedVault
    }

    companion object {
        const val INFO_SEPARATOR: String = ";"
        const val INFO_ELEMENTS: Int = 3
        const val MAGIC_PART: Int = 0
        const val VERSION_PART: Int = 1
        const val CYPHER_PART: Int = 2

        const val VAULT_MAGIC: String = "\$ANSIBLE_VAULT"
        const val VAULT_VERSION: String = "1.1"

        fun vaultInfoForCypher(vaultCypher: String): String {
            val infoLine = "$VAULT_MAGIC;$VAULT_VERSION;$vaultCypher"
            return infoLine
        }

        fun fromVaultData(data: ByteArray): VaultInfo {
            return VaultInfo(String(data).lines().first())
        }
    }
}
