package net.optionfactory.jetbrains.ansivault

import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret.AnsibleConfig
import net.optionfactory.jetbrains.ansivault.VaultLookup.FileStatus
import net.optionfactory.jetbrains.ansivault.configuration.VaultLookupTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class VaultLookupTest {
    private val project: Path = Files.createTempDirectory("ansivault-lookup")
    private val home: Path = Files.createTempDirectory("ansivault-home")

    private fun write(path: Path, text: String): Path {
        Files.createDirectories(path.parent)
        return Files.writeString(path, text)
    }

    private fun lookup(env: Map<String, String> = emptyMap(), saved: Set<String> = emptySet(), projectEncrypt: String = ""): VaultLookup {
        return VaultLookup.resolve(AnsibleConfig.load(project, env::get, home), { it in saved }, projectEncrypt)
    }

    @Test
    fun `nothing configured asks the default vault id password`() {
        val lookup = lookup()
        assertEquals(listOf("ANSIBLE_CONFIG", "Project", "User", "System"), lookup.configFiles.map { it.label })
        assertTrue(lookup.configFiles.none { it.inUse })
        val source = lookup.sources.single()
        assertEquals("default", source.vaultId)
        assertNull(source.file)
        assertFalse(source.saved)
        assertNull(source.definedBy)
        assertEquals("default", lookup.encryptVaultId!!.vaultId)
        assertEquals("the only vault id", lookup.encryptVaultId.origin)
    }

    @Test
    fun `reads identity list and password file from the project config`() {
        val cfg = write(project.resolve("infra/ansible/ansible.cfg"), """
            [defaults]
            vault_identity_list = dev@dev.pass, prod@prompt
            vault_password_file = missing.pass
            vault_identity = team
            vault_id_match = yes
        """.trimIndent())
        write(cfg.parent.resolve("dev.pass"), "s3cret\n")

        val lookup = lookup(saved = setOf("prod"))

        assertTrue(lookup.configFiles.single { it.label == "Project" }.inUse)
        assertEquals("dev@dev.pass, prod@prompt", lookup.settings.single { it.key == "vault_identity_list" }.value)
        assertEquals(cfg.toString(), lookup.settings.single { it.key == "vault_identity_list" }.origin)
        assertNull(lookup.settings.single { it.key == "vault_encrypt_identity" }.value)
        assertEquals(listOf("dev", "prod", "team"), lookup.sources.map { it.vaultId })
        assertEquals(FileStatus.READABLE, lookup.sources[0].fileStatus)
        assertEquals(cfg.parent.resolve("dev.pass"), lookup.sources[0].file)
        assertNull(lookup.sources[1].fileStatus)
        assertTrue(lookup.sources[1].saved)
        assertEquals(FileStatus.MISSING, lookup.sources[2].fileStatus)
        assertEquals("vault_password_file in $cfg", lookup.sources[2].definedBy)
        assertTrue(lookup.vaultIdMatch)
        assertNull("several vault ids and none chosen", lookup.encryptVaultId)
    }

    @Test
    fun `environment variables win over the config files`() {
        write(project.resolve("ansible.cfg"), "[defaults]\nvault_password_file = file.pass\nvault_encrypt_identity = cfg\n")
        val lookup = lookup(env = mapOf("ANSIBLE_VAULT_PASSWORD_FILE" to "env.pass"))
        assertEquals("env.pass", lookup.settings.single { it.key == "vault_password_file" }.value)
        assertEquals("environment variable ANSIBLE_VAULT_PASSWORD_FILE", lookup.settings.single { it.key == "vault_password_file" }.origin)
        assertEquals("cfg", lookup.encryptVaultId!!.vaultId)
    }

    @Test
    fun `the project setting chooses the vault id to encrypt`() {
        write(project.resolve("ansible.cfg"), "[defaults]\nvault_identity_list = a@prompt, b@prompt\nvault_encrypt_identity = a\n")
        val encrypt = lookup(projectEncrypt = " b ").encryptVaultId!!
        assertEquals("b", encrypt.vaultId)
        assertEquals("AnsiVault project setting", encrypt.origin)
    }

    @Test
    fun `the user config is used when the project has none`() {
        write(home.resolve(".ansible.cfg"), "[defaults]\nvault_password_file = ~/x.pass\n")
        val lookup = lookup()
        assertFalse(lookup.configFiles.single { it.label == "Project" }.exists)
        assertTrue(lookup.configFiles.single { it.label == "User" }.inUse)
    }

    @Test
    fun `the tree shows the search order and the problems`() {
        write(project.resolve("ansible.cfg"), "[defaults]\nvault_identity_list = a@prompt, b@nope.pass\n")
        val tree = VaultLookupTree.build(lookup(saved = setOf("a")))
        assertEquals(listOf("Ansible configuration files", "Vault settings", "Vault passwords", "Encryption"), tree.map { it.text })
        val passwords = tree[2].children
        assertEquals(VaultLookupTree.Kind.OK, passwords[0].kind)
        assertTrue(passwords[0].text, passwords[0].text.contains("saved in the IDE credential store"))
        assertEquals(VaultLookupTree.Kind.WARNING, passwords[1].kind)
        assertTrue(passwords[1].text, passwords[1].text.contains("not found"))
        assertEquals(VaultLookupTree.Kind.WARNING, tree[3].children.single().kind)
    }

    @Test
    fun `paths are shown relative to the project and the home directory`() {
        val shorten = VaultLookupTree.pathShortener("/work/proj", "/home/me")
        assertEquals("Project: infra/ansible.cfg", shorten("Project: /work/proj/infra/ansible.cfg"))
        assertEquals("from vault_password_file in ~/.ansible.cfg", shorten("from vault_password_file in /home/me/.ansible.cfg"))
        assertEquals("System: /etc/ansible/ansible.cfg", shorten("System: /etc/ansible/ansible.cfg"))
        val nested = VaultLookupTree.pathShortener("/home/me/proj", "/home/me")
        assertEquals("a.cfg and ~/b.cfg", nested("/home/me/proj/a.cfg and /home/me/b.cfg"))
    }
}
