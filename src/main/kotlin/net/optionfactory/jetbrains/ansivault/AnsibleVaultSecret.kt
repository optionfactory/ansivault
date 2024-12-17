package net.optionfactory.jetbrains.ansivault

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import net.optionfactory.jetbrains.ansivault.crypto.VaultHandler
import org.ini4j.Ini
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class AnsibleVaultSecret(val secret: String) {

    companion object {
        val logger = Logger.getInstance(AnsibleVaultSecret::class.java)
        fun search(project: Project): AnsibleVaultSecret {
            val systemProp = System.getProperty("ANSIBLE_CONFIG")
            if (systemProp != null) {
                return AnsibleVaultSecret(systemProp)
            }
            val projectConfig = project.basePath?.let { path ->
                File(path).walk(FileWalkDirection.TOP_DOWN)
                    .asSequence()
                    .filter { file -> file.isFile }
                    .filter { file -> file.name == "ansible.cfg" }
                    .first()
            }
            val homeConfig =
                Path(System.getProperty("user.home")).resolve("ansible.cfg")
                    .first { it.exists() && it.isRegularFile() }?.toFile()
            val systemConfig =
                Path("/etc/ansible/ansible.cfg")
                    .first { it.exists() && it.isRegularFile() }?.toFile()
            val secret = listOfNotNull(projectConfig, homeConfig, systemConfig)
                .map { file ->
                    val ini = Ini(file).get("defaults")?.get("vault_password_file")
                    val secret = ini?.let {
                        val realPath =
                            if (it.startsWith("~")) it.replaceFirst("~", System.getProperty("user.home")) else it
                        Path(realPath).toFile().readLines()[0]
                    }
                    return@map secret
                }
                .first()

            if (secret == null) {
                throw NotImplementedError("TODO: Open a popup to take the password")
            }
            return AnsibleVaultSecret(secret)
        }
    }

    fun encrypt(selectedText: String?): String {
        if (selectedText == null) {
            return ""
        }
        return String(VaultHandler.encrypt(selectedText.toByteArray(), secret))
    }

    fun decrypt(selectedText: String?): String {
        if (selectedText == null) {
            return ""
        }
        return String(VaultHandler.decrypt(selectedText.toByteArray(), secret)!!)
    }


}
