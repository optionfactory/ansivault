package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.FormBuilder
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel


class AppSettingsComponent {
    var mainPanel: JPanel? = null
    var jButton = JButton()
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
        val get = CredentialManager.getCredential()

        jButton.text = if (get == null) "No Password saved" else "Forget Saved Password"
        jButton.setEnabled(get != null)
        jButton.addActionListener { CredentialManager.deleteCredential() }

        ButtonGroup().apply {
            placementButtons.values.forEach { add(it) }
        }
        select(VaultEditorPlacement.SPLIT_RIGHT)

        val formBuilder = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("If dialog is used to save the password you can forget from here:"))
            .addComponent(jButton)
            .addVerticalGap(10)
            .addComponent(JBLabel("Where to open the vault editor:"))
        placementButtons.values.forEach { formBuilder.addComponent(it) }
        mainPanel = formBuilder
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }


}
