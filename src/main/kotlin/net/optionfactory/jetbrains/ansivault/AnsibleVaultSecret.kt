package net.optionfactory.jetbrains.ansivault

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import net.optionfactory.jetbrains.ansivault.configuration.CredentialManager
import net.optionfactory.jetbrains.ansivault.crypto.VaultHandler
import net.optionfactory.jetbrains.ansivault.ui.DialogBox
import org.ini4j.Ini
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isReadable
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
                    .map { file -> file.toPath() }
                    .firstOrNull()
            }
            val homeConfig = Path(System.getProperty("user.home")).resolve("ansible.cfg")
            val systemConfig = Path("/etc/ansible/ansible.cfg")
            val secret = listOfNotNull(projectConfig, homeConfig, systemConfig)
                .filter { it.exists() && it.isRegularFile() }
                .map { it.toFile() }
                .firstNotNullOfOrNull { file ->
                    val ini = Ini(file).get("defaults")?.get("vault_password_file")
                    val secret = ini?.let {
                        val realPath =
                            Path(if (it.startsWith("~")) it.replaceFirst("~", System.getProperty("user.home")) else it)

                        if (realPath.exists() && realPath.isRegularFile() && realPath.isReadable()) {
                            return@let Files.readAllLines(realPath)[0]
                        }
                        return@let null
                    }
                    return@firstNotNullOfOrNull secret
                }

            if (secret != null) {
                return AnsibleVaultSecret(secret)
            }

            val maybeCredential = CredentialManager.getCredential()
            if (maybeCredential == null) {
                DialogBox().showAndGet()
            }

            val secretFromBox = String(CredentialManager.getCredential()!!.password!!.toByteArray())
            return AnsibleVaultSecret(secretFromBox)
        }
    }

    fun encrypt(text: String?, indent: Int = 0): String {
        if (text == null) {
            return ""
        }
        val indentation = "".padStart(indent)
        val encryptedText = String(VaultHandler.encrypt(text.toByteArray(), secret))
        return encryptedText.lines()
            .map { indentation + it }
            .joinToString("\n")

    }

    fun decrypt(selectedText: String?): String {
        if (selectedText == null) {
            return ""
        }
        return String(VaultHandler.decrypt(selectedText.toByteArray(), secret))
    }


}
