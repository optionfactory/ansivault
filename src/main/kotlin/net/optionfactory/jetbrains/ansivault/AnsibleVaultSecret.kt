package net.optionfactory.jetbrains.ansivault

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import net.optionfactory.jetbrains.ansivault.configuration.CredentialManager
import net.optionfactory.jetbrains.ansivault.crypto.VaultHandler
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo.Companion.DEFAULT_VAULT_ID
import net.optionfactory.jetbrains.ansivault.crypto.decoders.WrongPasswordException
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
        internal const val PROMPT = "prompt"

        /**
         * The passwords of the project, read from their files or the credential store, asking the missing ones.
         * With [forEncryption] only the password of the vault id used to encrypt is needed (and asked).
         */
        fun search(project: Project, forEncryption: Boolean = false): AnsibleVaultSecret? {
            val lookup = VaultLookup.resolve(project)
            val encryptVaultId = lookup.encryptVaultId?.vaultId
            val needed = lookup.sources
                .filter { !forEncryption || it.vaultId == encryptVaultId }
                .ifEmpty { lookup.sources }
            val passwords = linkedMapOf<String, String>()
            val toAsk = mutableListOf<String>()
            needed.forEach { source ->
                val fromFile = source.file?.let { readPasswordFile(it) }
                if (source.file != null && fromFile == null) {
                    logger.warn("Vault password file for '${source.vaultId}' not readable: ${source.file}")
                }
                val password = fromFile ?: CredentialManager.getPassword(project, source.vaultId)
                if (password != null) passwords[source.vaultId] = password else toAsk.add(source.vaultId)
            }
            if (toAsk.isNotEmpty()) {
                if (!DialogBox(project, toAsk).showAndGet()) {
                    return null
                }
                toAsk.forEach { vaultId -> CredentialManager.getPassword(project, vaultId)?.let { passwords[vaultId] = it } }
            }
            return AnsibleVaultSecret(
                passwords = passwords,
                encryptVaultId = encryptVaultId,
                vaultIdMatch = lookup.vaultIdMatch,
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
        internal fun readPasswordFile(path: Path): String? {
            if (!path.exists() || !path.isRegularFile() || !path.isReadable()) {
                return null
            }
            return Files.readAllLines(path, Charsets.UTF_8).firstOrNull()?.trim()?.ifEmpty { null }
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
        val encryptedText = String(VaultHandler.encrypt(text.toByteArray(Charsets.UTF_8), password, vaultId = id), Charsets.UTF_8)
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
                val clear = String(VaultHandler.decrypt(vaultText.toByteArray(Charsets.UTF_8), password), Charsets.UTF_8)
                return clear to AnsibleVaultSecret(mapOf(vaultId to password))
            } catch (e: WrongPasswordException) {
                lastError = lastError ?: e
            } catch (e: IOException) {
                // a malformed vault fails the same way with any password
                throw e
            }
        }
        throw WrongPasswordException("No vault password could decrypt vault id '$vaultId'" + (lastError?.message?.let { ": $it" } ?: ""))
    }

    /** Ansible settings: environment variable (ANSIBLE_ + upper case key) first, then the config files. */
    internal class AnsibleConfig(
        /** The config files ansible would consider, in precedence order, found or not. */
        val candidates: List<Candidate>,
        private val projectDir: Path,
        private val env: (String) -> String?,
    ) {

        /** A config file location: [path] is null when it is not set (ANSIBLE_CONFIG) or not found (project). */
        class Candidate(val label: String, val path: Path?) {
            val exists: Boolean = path != null && path.exists() && path.isRegularFile()
        }

        /**
         * A setting value, where it comes from ([origin]: an environment variable or a config file)
         * and the directory relative paths in it are resolved against.
         */
        class Value(val value: String, private val baseDir: Path, val origin: String) {
            /** Resolves a password file path; null for ansible's `prompt`. */
            fun resolve(path: String): Path? {
                return if (path == PROMPT) null else baseDir.resolve(path.replaceFirst(Regex("^~"), System.getProperty("user.home")))
            }
        }

        val files: List<Path> = candidates.filter { it.exists }.mapNotNull { it.path }

        private val inis = files.associateWith { file ->
            try {
                Ini(file.toFile())
            } catch (e: IOException) {
                logger.warn("Unable to read ansible config $file", e)
                null
            }
        }

        fun get(key: String): Value? {
            val envName = "ANSIBLE_" + key.uppercase()
            env(envName)?.let { return Value(it, projectDir, "environment variable $envName") }
            // shortcut: ansible reads only the first config file found, here each key falls back to the next file
            return files.firstNotNullOfOrNull { file ->
                inis[file]?.get("defaults")?.get(key)?.let { Value(it, file.toAbsolutePath().parent, file.toString()) }
            }
        }

        companion object {
            fun load(project: Project): AnsibleConfig {
                return load(project.basePath?.let { Path(it) }, System::getenv, Path(System.getProperty("user.home")))
            }

            /** Same precedence as ansible: ANSIBLE_CONFIG, ./ansible.cfg, ~/.ansible.cfg, /etc/ansible/ansible.cfg */
            fun load(projectDir: Path?, env: (String) -> String?, home: Path): AnsibleConfig {
                val candidates = listOf(
                    Candidate("ANSIBLE_CONFIG", env("ANSIBLE_CONFIG")?.let { Path(it) }),
                    Candidate("Project", projectDir?.let { findProjectConfig(it.toFile()) }),
                    Candidate("User", home.resolve(".ansible.cfg")),
                    Candidate("System", Path("/etc/ansible/ansible.cfg")),
                )
                return AnsibleConfig(candidates, projectDir ?: Path("."), env)
            }

            private const val CONFIG_FILE = "ansible.cfg"
            private const val MAX_DEPTH = 5
            private val SKIPPED_DIRS = setOf("node_modules", "build", "target", "out", "dist", "venv", "__pycache__")

            /**
             * The project root ansible.cfg, or the shallowest one in the project (breadth first,
             * at most [MAX_DEPTH] levels, skipping hidden and build/dependency directories).
             */
            fun findProjectConfig(root: File): Path? {
                var level = listOf(root)
                repeat(MAX_DEPTH + 1) {
                    level.map { File(it, CONFIG_FILE) }.firstOrNull { it.isFile }?.let { return it.toPath() }
                    level = level.flatMap { dir ->
                        dir.listFiles { file -> file.isDirectory && !file.name.startsWith(".") && file.name !in SKIPPED_DIRS }
                            .orEmpty()
                            .sortedBy { it.name }
                    }
                    if (level.isEmpty()) {
                        return null
                    }
                }
                return null
            }
        }
    }
}
