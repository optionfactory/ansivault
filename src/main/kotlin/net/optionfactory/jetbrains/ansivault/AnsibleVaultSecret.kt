package net.optionfactory.jetbrains.ansivault

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import net.optionfactory.jetbrains.ansivault.configuration.CredentialManager
import net.optionfactory.jetbrains.ansivault.configuration.VaultSettings
import net.optionfactory.jetbrains.ansivault.crypto.VaultHandler
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo.Companion.DEFAULT_VAULT_ID
import net.optionfactory.jetbrains.ansivault.ui.DialogBox
import org.ini4j.Ini
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

/**
 * The vault passwords of a project, by vault id (insertion order = try order).
 * [encryptVaultId] picks the password used to encrypt when there is more than one;
 * [vaultIdMatch] is ansible's vault_id_match: decrypt only with the password of the vault's own id.
 */
class AnsibleVaultSecret(
    val passwords: Map<String, String>,
    val encryptVaultId: String? = null,
    val vaultIdMatch: Boolean = false,
) {

    constructor(secret: String) : this(mapOf(DEFAULT_VAULT_ID to secret))

    companion object {
        val logger = Logger.getInstance(AnsibleVaultSecret::class.java)
        private const val PROMPT = "prompt"

        fun search(project: Project): AnsibleVaultSecret? {
            val config = AnsibleConfig.load(project)
            val defaultVaultId = config.get("vault_identity")?.value ?: DEFAULT_VAULT_ID
            val identityList = config.get("vault_identity_list")
            val passwordFile = config.get("vault_password_file")
            // vault id -> password file, null = prompt; same order as ansible: identity list, then password file
            val sources = identityList?.let { list ->
                parseVaultIdentityList(list.value, defaultVaultId).map { (id, source) -> id to list.resolve(source) }
            }.orEmpty() + listOfNotNull(passwordFile?.let { defaultVaultId to it.resolve(it.value) })

            val passwords = linkedMapOf<String, String>()
            val toAsk = mutableListOf<String>()
            sources.ifEmpty { listOf(defaultVaultId to null) }.forEach { (vaultId, file) ->
                if (vaultId in passwords || vaultId in toAsk) {
                    return@forEach
                }
                val fromFile = file?.let { readPasswordFile(it) }
                if (file != null && fromFile == null) {
                    logger.warn("Vault password file for '$vaultId' not readable: $file")
                }
                val password = fromFile ?: CredentialManager.getPassword(project, vaultId)
                if (password != null) passwords[vaultId] = password else toAsk.add(vaultId)
            }
            if (toAsk.isNotEmpty()) {
                if (!DialogBox(project, toAsk).showAndGet()) {
                    return null
                }
                toAsk.forEach { vaultId -> CredentialManager.getPassword(project, vaultId)?.let { passwords[vaultId] = it } }
            }
            return AnsibleVaultSecret(
                passwords = passwords,
                encryptVaultId = VaultSettings.getInstance(project).state.encryptVaultId.ifBlank { null }
                    ?: config.get("vault_encrypt_identity")?.value,
                vaultIdMatch = config.get("vault_id_match")?.value?.lowercase() in setOf("true", "yes", "on", "1", "y", "t"),
            )
        }

        /** Parses ansible's `label@source, other@prompt` list; a source without a label gets [defaultVaultId]. */
        fun parseVaultIdentityList(list: String, defaultVaultId: String = DEFAULT_VAULT_ID): List<Pair<String, String>> {
            return list.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { entry ->
                    val vaultId = if ("@" in entry) entry.substringBefore("@") else defaultVaultId
                    val source = if ("@" in entry) entry.substringAfter("@") else entry
                    if (vaultId.isEmpty() || ";" in vaultId) {
                        logger.warn("Invalid vault id '$vaultId' ignored")
                        return@mapNotNull null
                    }
                    vaultId to source
                }
        }

        // shortcut: executable password files (scripts / *-client) are read as text, not executed
        private fun readPasswordFile(path: Path): String? {
            if (!path.exists() || !path.isRegularFile() || !path.isReadable()) {
                return null
            }
            return Files.readAllLines(path).firstOrNull()
        }
    }

    /** Encrypts with the password of [vaultId], or of the configured encrypt vault id when null. */
    fun encrypt(text: String?, indent: Int = 0, vaultId: String? = null): String {
        if (text == null) {
            return ""
        }
        val id = vaultId ?: encryptVaultId ?: passwords.keys.singleOrNull()
        ?: throw IOException("Multiple vault ids (${passwords.keys.joinToString()}): choose the one to encrypt with in Settings > Tools > AnsiVault Configuration")
        val password = passwords[id] ?: throw IOException("No password available for vault id '$id'")
        val indentation = "".padStart(indent)
        val encryptedText = String(VaultHandler.encrypt(text.toByteArray(), password, vaultId = id))
        return encryptedText.lines()
            .map { indentation + it }
            .joinToString("\n")
    }

    fun decrypt(selectedText: String?): String {
        if (selectedText == null) {
            return ""
        }
        return decryptForEdit(selectedText).first
    }

    /**
     * Decrypts and returns, with the clear text, the secret to encrypt it back:
     * same vault id as the original vault and the password that actually opened it (like `ansible-vault edit`).
     */
    fun decryptForEdit(vaultText: String): Pair<String, AnsibleVaultSecret> {
        val vaultId = VaultInfo(vaultText).vaultId ?: DEFAULT_VAULT_ID
        val candidates = if (vaultIdMatch) {
            listOfNotNull(passwords[vaultId])
        } else {
            listOfNotNull(passwords[vaultId]) + passwords.filterKeys { it != vaultId }.values
        }
        var lastError: IOException? = null
        for (password in candidates) {
            try {
                val clear = String(VaultHandler.decrypt(vaultText.toByteArray(), password))
                return clear to AnsibleVaultSecret(mapOf(vaultId to password))
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw IOException("No vault password could decrypt vault id '$vaultId'" + (lastError?.message?.let { ": $it" } ?: ""), lastError)
    }

    /** Ansible settings: environment variable (ANSIBLE_ + upper case key) first, then the config files. */
    private class AnsibleConfig(private val files: List<Path>, private val projectDir: Path) {

        /** A setting value and the directory relative paths in it are resolved against. */
        class Value(val value: String, private val baseDir: Path) {
            /** Resolves a password file path; null for ansible's `prompt`. */
            fun resolve(path: String): Path? {
                return if (path == PROMPT) null else baseDir.resolve(path.replaceFirst(Regex("^~"), System.getProperty("user.home")))
            }
        }

        private val inis = files.associateWith { Ini(it.toFile()) }

        fun get(key: String): Value? {
            System.getenv("ANSIBLE_" + key.uppercase())?.let { return Value(it, projectDir) }
            // shortcut: ansible reads only the first config file found, here each key falls back to the next file
            return files.firstNotNullOfOrNull { file ->
                inis[file]?.get("defaults")?.get(key)?.let { Value(it, file.toAbsolutePath().parent) }
            }
        }

        companion object {
            fun load(project: Project): AnsibleConfig {
                val projectDir = Path(project.basePath ?: ".")
                val envConfig = System.getenv("ANSIBLE_CONFIG")?.let { Path(it) }
                val projectConfig = project.basePath?.let { path ->
                    File(path).walk(FileWalkDirection.TOP_DOWN)
                        .asSequence()
                        .filter { file -> file.isFile }
                        .filter { file -> file.name == "ansible.cfg" }
                        .map { file -> file.toPath() }
                        .firstOrNull()
                }
                val homeConfig = Path(System.getProperty("user.home")).resolve(".ansible.cfg")
                val systemConfig = Path("/etc/ansible/ansible.cfg")
                // same precedence as ansible: ANSIBLE_CONFIG, ./ansible.cfg, ~/.ansible.cfg, /etc/ansible/ansible.cfg
                val files = listOfNotNull(envConfig, projectConfig, homeConfig, systemConfig)
                    .filter { it.exists() && it.isRegularFile() }
                return AnsibleConfig(files, projectDir)
            }
        }
    }
}
