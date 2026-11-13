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

    /** Label of a 1.2 vault (`$ANSIBLE_VAULT;1.2;AES256;label`), null for 1.1. */
    var vaultId: String? = null
        private set

    init {
        logger.debug("Ansible Vault info: {}", infoLine)

        val header = infoLine.lineSequence().first().trim()
        val infoParts = header.split(INFO_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (infoParts.size == INFO_ELEMENTS || infoParts.size == INFO_ELEMENTS + 1) {
            if (infoParts[MAGIC_PART] == VAULT_MAGIC) {
                isEncryptedVault = true
                vaultVersion = infoParts[VERSION_PART]
                vaultCypher = infoParts[CYPHER_PART]
                vaultId = infoParts.getOrNull(VAULT_ID_PART)
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
        const val VAULT_ID_PART: Int = 3

        const val VAULT_MAGIC: String = "\$ANSIBLE_VAULT"
        const val VAULT_VERSION: String = "1.1"
        const val VAULT_VERSION_WITH_ID: String = "1.2"
        const val DEFAULT_VAULT_ID: String = "default"

        /** Like ansible-vault: the "default" label (or none) is written as 1.1, any other label as 1.2. */
        fun vaultInfoForCypher(vaultCypher: String, vaultId: String? = null): String {
            if (vaultId == null || vaultId == DEFAULT_VAULT_ID) {
                return "$VAULT_MAGIC;$VAULT_VERSION;$vaultCypher"
            }
            return "$VAULT_MAGIC;$VAULT_VERSION_WITH_ID;$vaultCypher;$vaultId"
        }

        fun fromVaultData(data: ByteArray): VaultInfo {
            return VaultInfo(String(data).lines().first())
        }
    }
}
