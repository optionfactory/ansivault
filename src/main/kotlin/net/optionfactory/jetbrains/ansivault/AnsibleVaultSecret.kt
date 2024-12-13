package net.optionfactory.jetbrains.ansivault

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.ini4j.Ini
import java.io.File
import kotlin.io.path.Path

class AnsibleVaultSecret(val secret: String) {

    companion object {
        fun search(project: Project): AnsibleVaultSecret {
            val logger = Logger.getInstance(AnsibleVaultSecret.Companion::class.java)
            val secret = project.basePath?.let {
                File(it).walk(FileWalkDirection.TOP_DOWN)
                    .asSequence()
                    .filter { file -> file.isFile }
                    .filter { file -> file.name == "ansible.cfg" } //TODO: use also "ANSIBLE_CONFIG", ~/.ansible.cfg, /etc/ansible/ansible.cfg
                    .map { file ->
                        val ini = Ini(file).get("defaults")?.get("vault_password_file")
                        val secret = ini?.let {
                            val realPath =
                                if (it.startsWith("~")) it.replaceFirst("~", System.getProperty("user.home")) else it
                            Path(realPath).toFile().readLines()[0]
                        }.orEmpty()
                        return@map secret
                    }
                    .filter { it.isNotBlank() }
                    .first()

            }
            if (secret == null) {
                throw NotImplementedError("TODO: implement fallback password lookup")
            }
            return AnsibleVaultSecret(secret)
        }
    }

    fun encrypt(selectedText: String?): String {
        if (selectedText == null) {
            return ""
        }
        
        return "Encrypt"
    }


}
