package net.optionfactory.jetbrains.ansivault.ui

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret
import net.optionfactory.jetbrains.ansivault.VaultLookup
import net.optionfactory.jetbrains.ansivault.configuration.CredentialManager
import net.optionfactory.jetbrains.ansivault.configuration.ProjectConfigurable
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo.Companion.DEFAULT_VAULT_ID
import net.optionfactory.jetbrains.ansivault.crypto.decoders.WrongPasswordException

object VaultDecryption {
    const val TITLE = "Ansible Vault"

    /** Declared in plugin.xml. */
    const val NOTIFICATION_GROUP = "AnsiVault"

    /**
     * Decrypts [vaultText] with the project passwords, see [AnsibleVaultSecret.decryptForEdit].
     * Only the password of the vault's own vault id is asked first; like ansible the others are tried
     * (and asked) only if it fails, unless vault_id_match. When the password of the vault id is the one saved
     * in the IDE credential store and it is wrong, offers to forget it and type it again.
     * Returns null when the user cancels or decryption fails (the error has already been shown).
     */
    fun decryptForEdit(project: Project, vaultText: String): Pair<String, AnsibleVaultSecret>? {
        val vaultId = VaultInfo(vaultText).vaultId ?: DEFAULT_VAULT_ID
        val sources = VaultLookup.resolve(project).sources
        var onlyOwn = sources.any { it.vaultId == vaultId }
        while (true) {
            val secret = AnsibleVaultSecret.search(project, vaultId = vaultId.takeIf { onlyOwn }) ?: return null
            try {
                return secret.decryptForEdit(vaultText)
            } catch (e: WrongPasswordException) {
                if (onlyOwn && !secret.vaultIdMatch && sources.any { it.vaultId != vaultId }) {
                    onlyOwn = false
                    continue
                }
                val own = VaultLookup.resolve(project).sources.firstOrNull { it.vaultId == vaultId }
                if (own == null || own.fromFile || !own.saved) {
                    showError(project, e)
                    return null
                }
                val answer = Messages.showYesNoDialog(
                    project,
                    "The password of vault id '$vaultId' saved in the IDE credential store is wrong.\n\nForget it and type it again?",
                    TITLE,
                    Messages.getQuestionIcon(),
                )
                if (answer != Messages.YES) {
                    return null
                }
                CredentialManager.deletePassword(project, vaultId)
                onlyOwn = true
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
