package net.optionfactory.jetbrains.ansivault.configuration

import net.optionfactory.jetbrains.ansivault.VaultLookup
import net.optionfactory.jetbrains.ansivault.VaultLookup.FileStatus

/** The [VaultLookup] as a tree to show in the settings: the search order, what was found and where it comes from. */
object VaultLookupTree {

    enum class Kind { GROUP, OK, INFO, WARNING, UNUSED }

    data class Node(
        val text: String,
        val detail: String? = null,
        val kind: Kind,
        val children: List<Node> = emptyList(),
        /** The vault id whose password is saved in the credential store and can be forgotten from this node. */
        val savedVaultId: String? = null,
    )

    /** [shorten] makes the paths in the texts shorter, see [pathShortener]. */
    fun build(lookup: VaultLookup, shorten: (String) -> String = { it }): List<Node> {
        return listOf(configFiles(lookup), settings(lookup), passwords(lookup), encryption(lookup)).map { shortened(it, shorten) }
    }

    /** Paths in the project become relative to it, paths in the home directory start with `~`. */
    fun pathShortener(projectDir: String?, home: String): (String) -> String = { text ->
        val relative = projectDir?.let { text.replace(it.trimEnd('/') + "/", "") } ?: text
        relative.replace(home.trimEnd('/') + "/", "~/")
    }

    private fun shortened(node: Node, shorten: (String) -> String): Node {
        return node.copy(text = shorten(node.text), detail = node.detail?.let(shorten), children = node.children.map { shortened(it, shorten) })
    }

    private fun configFiles(lookup: VaultLookup): Node {
        val children = lookup.configFiles.map { file ->
            when {
                file.path == null && file.label == "ANSIBLE_CONFIG" -> Node("ANSIBLE_CONFIG", "environment variable not set", Kind.UNUSED)
                file.path == null -> Node(file.label, "no ansible.cfg found in the project", Kind.UNUSED)
                !file.exists -> Node("${file.label}: ${file.path}", "not found", Kind.UNUSED)
                file.inUse -> Node("${file.label}: ${file.path}", "found, vault settings read from here", Kind.OK)
                else -> Node("${file.label}: ${file.path}", "found, no vault settings", Kind.INFO)
            }
        }
        return Node("Ansible configuration files", "searched in this order", Kind.GROUP, children)
    }

    private fun settings(lookup: VaultLookup): Node {
        val children = lookup.settings.map { setting ->
            if (setting.value == null) {
                Node(setting.key, "not set", Kind.UNUSED)
            } else {
                Node("${setting.key} = ${setting.value}", "from ${setting.origin}", Kind.OK)
            }
        }
        return Node("Vault settings", "[defaults] section, or ANSIBLE_* environment variables", Kind.GROUP, children)
    }

    private fun passwords(lookup: VaultLookup): Node {
        val children = lookup.sources.map { source ->
            val fallback = if (source.saved) "saved in the IDE credential store" else "asked when needed, then saved in the IDE credential store"
            val (text, kind) = when (source.fileStatus) {
                null -> "prompt: $fallback" to if (source.saved) Kind.OK else Kind.INFO
                FileStatus.READABLE -> "file ${source.file}" + (if (source.executable) " (executable: read as text, not run)" else "") to
                    if (source.executable) Kind.WARNING else Kind.OK
                FileStatus.MISSING -> "file ${source.file} not found: $fallback" to Kind.WARNING
                FileStatus.EMPTY -> "file ${source.file} is empty: $fallback" to Kind.WARNING
                FileStatus.UNREADABLE -> "file ${source.file} not readable: $fallback" to Kind.WARNING
            }
            Node(
                "Vault id '${source.vaultId}': $text",
                source.definedBy?.let { "from $it" } ?: "nothing configured",
                kind,
                savedVaultId = source.vaultId.takeIf { source.saved },
            )
        }
        val detail = if (lookup.vaultIdMatch) "vault_id_match: a vault is decrypted only with the password of its vault id" else "tried in this order"
        return Node("Vault passwords", detail, Kind.GROUP, children)
    }

    private fun encryption(lookup: VaultLookup): Node {
        val encrypt = lookup.encryptVaultId
        val child = when {
            encrypt == null -> Node("Not chosen", "several vault ids: set vault_encrypt_identity, or the vault id above", Kind.WARNING)
            lookup.sources.none { it.vaultId == encrypt.vaultId } -> Node("Vault id '${encrypt.vaultId}'", "from ${encrypt.origin}, but it has no password source", Kind.WARNING)
            else -> Node("Vault id '${encrypt.vaultId}'", "from ${encrypt.origin}", Kind.OK)
        }
        return Node("Encryption", "vault id used to encrypt new vaults", Kind.GROUP, listOf(child))
    }
}
