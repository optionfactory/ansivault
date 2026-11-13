package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel


class AppSettingsComponent(project: Project) {
    var mainPanel: JPanel? = null
    var jButton = JButton()
    val encryptVaultId = JBTextField()
    val placementButtons = linkedMapOf(
        VaultEditorPlacement.SPLIT_RIGHT to JBRadioButton("Right half of the editor"),
        VaultEditorPlacement.SPLIT_LEFT to JBRadioButton("Left half of the editor"),
        VaultEditorPlacement.SPLIT_TOP to JBRadioButton("Upper half of the editor"),
        VaultEditorPlacement.SPLIT_BOTTOM to JBRadioButton("Bottom half of the editor"),
        VaultEditorPlacement.NEW_TAB to JBRadioButton("New tab"),
    )

    val selectedPlacement: VaultEditorPlacement
        get() = placementButtons.entries.firstOrNull { it.value.isSelected }?.key ?: VaultEditorPlacement.SPLIT_RIGHT

    fun select(placement: VaultEditorPlacement) {
        placementButtons.forEach { (p, button) -> button.isSelected = p == placement }
    }

    init {
        val saved = CredentialManager.hasPasswords(project)
        jButton.text = if (saved) "Forget Saved Passwords" else "No Password saved"
        jButton.isEnabled = saved
        jButton.addActionListener {
            CredentialManager.deletePasswords(project)
            jButton.text = "No Password saved"
            jButton.isEnabled = false
        }

        ButtonGroup().apply {
            placementButtons.values.forEach { add(it) }
        }
        select(VaultEditorPlacement.SPLIT_RIGHT)

        val formBuilder = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("If dialog is used to save the password you can forget from here:"))
            .addComponent(jButton)
            .addVerticalGap(10)
            .addLabeledComponent("Vault id used to encrypt:", encryptVaultId)
            .addComponent(JBLabel("Needed only with multiple vault ids; empty = vault_encrypt_identity from ansible.cfg"))
            .addVerticalGap(10)
            .addComponent(JBLabel("Where to open the vault editor:"))
        placementButtons.values.forEach { formBuilder.addComponent(it) }
        mainPanel = formBuilder
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }


}
