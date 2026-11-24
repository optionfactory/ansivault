package net.optionfactory.jetbrains.ansivault.ui

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret
import net.optionfactory.jetbrains.ansivault.configuration.CredentialManager
import net.optionfactory.jetbrains.ansivault.configuration.ProjectConfigurable
import net.optionfactory.jetbrains.ansivault.crypto.decoders.WrongPasswordException

object VaultDecryption {
    const val TITLE = "Ansible Vault"

    /** Declared in plugin.xml. */
    const val NOTIFICATION_GROUP = "AnsiVault"

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

    /** A non blocking error notification, with a link to the settings showing where the passwords come from. */
    fun showError(project: Project, e: Exception) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(TITLE, e.message ?: "Unable to process the vault", NotificationType.ERROR)
            .addAction(NotificationAction.createSimple("Vault password settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, ProjectConfigurable::class.java)
            })
            .notify(project)
    }
}
