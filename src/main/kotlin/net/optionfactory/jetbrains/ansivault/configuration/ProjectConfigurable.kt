package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent


class ProjectConfigurable(private val project: Project) : Configurable {

    private var mySettingsComponent: AppSettingsComponent? = null


    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {

        return "Ansivault Configuration"
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.jButton
    }

    @Nullable
    override fun createComponent(): JComponent? {
        mySettingsComponent = AppSettingsComponent(project)
        reset()
        return mySettingsComponent!!.mainPanel
    }

    override fun isModified(): Boolean {
        val component = mySettingsComponent ?: return false
        val state = VaultSettings.getInstance(project).state
        return component.selectedPlacement != state.editorPlacement || component.encryptVaultId.text.trim() != state.encryptVaultId
    }

    override fun apply() {
        val component = mySettingsComponent ?: return
        val state = VaultSettings.getInstance(project).state
        state.editorPlacement = component.selectedPlacement
        state.encryptVaultId = component.encryptVaultId.text.trim()
    }

    override fun reset() {
        val component = mySettingsComponent ?: return
        val state = VaultSettings.getInstance(project).state
        component.select(state.editorPlacement)
        component.encryptVaultId.text = state.encryptVaultId
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }

}
