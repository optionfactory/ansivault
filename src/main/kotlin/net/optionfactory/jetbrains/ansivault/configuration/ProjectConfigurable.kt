package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent


class ProjectConfigurable : Configurable {

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
        return mySettingsComponent!!.mainPanel
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {
    }

    override fun reset() {
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }

}
