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
        mySettingsComponent = AppSettingsComponent()
        mySettingsComponent!!.select(VaultSettings.getInstance(project).state.editorPlacement)
        return mySettingsComponent!!.mainPanel
    }

    override fun isModified(): Boolean {
        val selected = mySettingsComponent?.selectedPlacement ?: return false
        return selected != VaultSettings.getInstance(project).state.editorPlacement
    }

    override fun apply() {
        val selected = mySettingsComponent?.selectedPlacement ?: return
        VaultSettings.getInstance(project).state.editorPlacement = selected
    }

    override fun reset() {
        mySettingsComponent?.select(VaultSettings.getInstance(project).state.editorPlacement)
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }

}
