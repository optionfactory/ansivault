package net.optionfactory.jetbrains.ansivault

import com.intellij.openapi.project.Project
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret.AnsibleConfig
import net.optionfactory.jetbrains.ansivault.configuration.CredentialManager
import net.optionfactory.jetbrains.ansivault.configuration.VaultSettings
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo.Companion.DEFAULT_VAULT_ID
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

/**
 * Where the vault passwords of a project come from: the ansible config files considered, the vault settings
 * found in them (or in the environment) and, for each vault id, its password source.
 * [AnsibleVaultSecret.search] uses it to get the passwords and the settings page shows it.
 */
class VaultLookup(
    val configFiles: List<ConfigFile>,
    val settings: List<Setting>,
    /** In try order, one per vault id. */
    val sources: List<PasswordSource>,
    /** The vault id used to encrypt, null when it has to be chosen (several vault ids, none configured). */
    val encryptVaultId: EncryptVaultId?,
    val vaultIdMatch: Boolean,
) {
    /** A config file location; [path] is null when not set (ANSIBLE_CONFIG) or not found (in the project). */
    data class ConfigFile(val label: String, val path: Path?, val exists: Boolean, val inUse: Boolean)

    /** A vault setting: [value] and [origin] (environment variable or config file) are null when not set. */
    data class Setting(val key: String, val value: String?, val origin: String?)

    enum class FileStatus { READABLE, EMPTY, MISSING, UNREADABLE }

    /**
     * The password of a vault id: read from [file], or, when the file is null (`prompt`) or not usable,
     * taken from the credential store if [saved] there, otherwise asked.
     */
    data class PasswordSource(
        val vaultId: String,
        val file: Path?,
        val fileStatus: FileStatus?,
        /** Executable password files are scripts for ansible: here they are read as text, not run. */
        val executable: Boolean,
        val saved: Boolean,
        /** The setting that defines this source, with its origin; null when nothing is configured. */
        val definedBy: String?,
    ) {
        val fromFile: Boolean get() = fileStatus == FileStatus.READABLE
    }

    data class EncryptVaultId(val vaultId: String, val origin: String)

    companion object {
        const val VAULT_PASSWORD_FILE = "vault_password_file"
        const val VAULT_IDENTITY_LIST = "vault_identity_list"
        const val VAULT_IDENTITY = "vault_identity"
        const val VAULT_ENCRYPT_IDENTITY = "vault_encrypt_identity"
        const val VAULT_ID_MATCH = "vault_id_match"
        val SETTINGS = listOf(VAULT_PASSWORD_FILE, VAULT_IDENTITY_LIST, VAULT_IDENTITY, VAULT_ENCRYPT_IDENTITY, VAULT_ID_MATCH)
        private val TRUE = setOf("true", "yes", "on", "1", "y", "t")

        fun resolve(project: Project): VaultLookup {
            return resolve(
                config = AnsibleConfig.load(project),
                isSaved = { vaultId -> CredentialManager.getPassword(project, vaultId) != null },
                projectEncryptVaultId = VaultSettings.getInstance(project).state.encryptVaultId,
            )
        }

        internal fun resolve(config: AnsibleConfig, isSaved: (String) -> Boolean, projectEncryptVaultId: String): VaultLookup {
            val values = SETTINGS.associateWith { config.get(it) }
            val defaultVaultId = values[VAULT_IDENTITY]?.value ?: DEFAULT_VAULT_ID

            // vault id -> password file (null = prompt); same order as ansible: identity list, then password file
            val configured = values[VAULT_IDENTITY_LIST]?.let { list ->
                AnsibleVaultSecret.parseVaultIdentityList(list.value, defaultVaultId)
                    .map { (id, source) -> Triple(id, list.resolve(source), "$VAULT_IDENTITY_LIST in ${list.origin}") }
            }.orEmpty() + listOfNotNull(values[VAULT_PASSWORD_FILE]?.let {
                Triple(defaultVaultId, it.resolve(it.value), "$VAULT_PASSWORD_FILE in ${it.origin}")
            })

            val sources = configured.ifEmpty { listOf(Triple(defaultVaultId, null, null)) }
                .distinctBy { it.first }
                .map { (vaultId, file, definedBy) ->
                    PasswordSource(
                        vaultId = vaultId,
                        file = file,
                        fileStatus = file?.let { fileStatus(it) },
                        executable = file != null && file.isRegularFile() && Files.isExecutable(file),
                        saved = isSaved(vaultId),
                        definedBy = definedBy,
                    )
                }

            val encryptVaultId = projectEncryptVaultId.trim().ifEmpty { null }?.let { EncryptVaultId(it, "AnsiVault project setting") }
                ?: values[VAULT_ENCRYPT_IDENTITY]?.let { EncryptVaultId(it.value, "$VAULT_ENCRYPT_IDENTITY in ${it.origin}") }
                ?: sources.singleOrNull()?.let { EncryptVaultId(it.vaultId, "the only vault id") }

            val inUse = config.files.toSet()
            return VaultLookup(
                configFiles = config.candidates.map { ConfigFile(it.label, it.path, it.exists, it.path in inUse && usedBy(it.path, values)) },
                settings = SETTINGS.map { key -> Setting(key, values[key]?.value, values[key]?.origin) },
                sources = sources,
                encryptVaultId = encryptVaultId,
                vaultIdMatch = values[VAULT_ID_MATCH]?.value?.lowercase() in TRUE,
            )
        }

        private fun usedBy(path: Path?, values: Map<String, AnsibleConfig.Value?>): Boolean {
            return path != null && values.values.any { it?.origin == path.toString() }
        }

        private fun fileStatus(file: Path): FileStatus {
            return when {
                !file.exists() -> FileStatus.MISSING
                !file.isRegularFile() || !file.isReadable() -> FileStatus.UNREADABLE
                AnsibleVaultSecret.readPasswordFile(file) == null -> FileStatus.EMPTY
                else -> FileStatus.READABLE
            }
        }
    }
}
