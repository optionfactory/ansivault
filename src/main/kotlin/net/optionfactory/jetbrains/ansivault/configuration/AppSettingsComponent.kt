package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JPanel


class AppSettingsComponent {
    var mainPanel: JPanel? = null
    var jButton = JButton()

    init {
        val get = CredentialManager.getCredential()

        jButton.text = if (get == null) "No Password saved" else "Forget Saved Password"
        jButton.setEnabled(get != null)
        jButton.addActionListener { CredentialManager.deleteCredential() }


        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("If dialog is used to save the password you can forget from here:"))
            .addComponent(jButton)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }


}
