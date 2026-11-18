package net.optionfactory.jetbrains.ansivault.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret
import net.optionfactory.jetbrains.ansivault.configuration.CredentialManager
import net.optionfactory.jetbrains.ansivault.crypto.decoders.WrongPasswordException

object VaultDecryption {
    const val TITLE = "Ansible Vault"

    /**
     * Decrypts [vaultText] with the project passwords, see [AnsibleVaultSecret.decryptForEdit].
     * When the passwords are wrong and some come from the credential store, offers to forget them and ask them again.
     * Returns null when the user cancels or decryption fails (the error has already been shown).
     */
    fun decryptForEdit(project: Project, vaultText: String): Pair<String, AnsibleVaultSecret>? {
        while (true) {
            val secret = AnsibleVaultSecret.search(project) ?: return null
            try {
                return secret.decryptForEdit(vaultText)
            } catch (e: WrongPasswordException) {
                if (!CredentialManager.hasPasswords(project)) {
                    showError(project, e)
                    return null
                }
                val answer = Messages.showYesNoDialog(
                    project,
                    "${e.message}\n\nForget the saved vault passwords and type them again?",
                    TITLE,
                    Messages.getQuestionIcon(),
                )
                if (answer != Messages.YES) {
                    return null
                }
                CredentialManager.deletePasswords(project)
            } catch (e: Exception) {
                showError(project, e)
                return null
            }
        }
    }

    fun showError(project: Project, e: Exception) {
        Messages.showErrorDialog(project, e.message ?: "Unable to process the vault", TITLE)
    }
}
