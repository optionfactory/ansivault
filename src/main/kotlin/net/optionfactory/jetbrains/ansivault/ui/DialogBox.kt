package net.optionfactory.jetbrains.ansivault.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.FormBuilder
import net.optionfactory.jetbrains.ansivault.configuration.CredentialManager
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo.Companion.DEFAULT_VAULT_ID
import javax.swing.JComponent
import javax.swing.JPasswordField

/** Asks the passwords of the given vault ids and saves them in the PasswordSafe. */
class DialogBox(private val project: Project, vaultIds: List<String>) : DialogWrapper(project, true) {
    val logger = Logger.getInstance(DialogWrapper::class.java)

    private val fields = vaultIds.associateWith { JPasswordField(20) }

    init {
        title = "Ansible Vault Password"
        isResizable = true
        init()
    }

    override fun createCenterPanel(): JComponent {
        val form = FormBuilder.createFormBuilder()
        fields.forEach { (vaultId, field) ->
            form.addLabeledComponent(if (vaultId == DEFAULT_VAULT_ID) "Secret:" else "Vault id '$vaultId':", field)
        }
        return form.panel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return fields.values.firstOrNull()
    }

    override fun doValidate(): ValidationInfo? {
        val empty = fields.values.firstOrNull { it.password.isEmpty() }
        if (empty != null) {
            return ValidationInfo("Secret cannot be an empty string", empty)
        }
        return super.doValidate()
    }

    override fun doOKAction() {
        if (!okAction.isEnabled) {
            logger.warn("Ok action is not enabled")
            return
        }
        fields.forEach { (vaultId, field) -> CredentialManager.setPassword(project, vaultId, String(field.password)) }
        close(OK_EXIT_CODE)
    }
}
