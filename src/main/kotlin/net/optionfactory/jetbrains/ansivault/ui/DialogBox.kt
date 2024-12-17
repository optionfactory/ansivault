package net.optionfactory.jetbrains.ansivault.ui

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

class DialogBox(): DialogWrapper(true) {
    val logger = Logger.getInstance(DialogWrapper::class.java)

    private lateinit var pass: JPasswordField

    init {
        title = "Vault Encrypt"
        isResizable = true
        init()
    }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel()
        val boxLayout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)

        dialogPanel.layout = boxLayout

        val label = JLabel("Secret:")
        label.alignmentX = Component.LEFT_ALIGNMENT
        dialogPanel.add(label, BorderLayout.CENTER)
        pass = JPasswordField(20)
        pass.alignmentX = Component.LEFT_ALIGNMENT
        dialogPanel.add(pass, BorderLayout.CENTER)

        return dialogPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return pass
    }

    override fun doValidate(): ValidationInfo? {
        if (pass.password.isEmpty()) {
            return ValidationInfo("Secret cannot be an empty string", pass)
        }
        return super.doValidate()
    }

    override fun doOKAction() {
        if (!okAction.isEnabled) {
            logger.warn("Ok action is not enabled")
            return
        }

        val credentialAttributes = CredentialAttributes(generateServiceName("AnsibleVaultSecret", "TODO"))
        val credentials = Credentials("vault-secret", pass.password)

        PasswordSafe.instance.set(credentialAttributes, credentials)
        close(OK_EXIT_CODE)
    }
}