package net.optionfactory.jetbrains.ansivault.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret

class VaultAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        Logger.getInstance(VaultAction::class.java).warn("SomeAction performed %s".format(event))
        val document = event.getData(PlatformDataKeys.EDITOR)?.document


        AnsibleVaultSecret.search()

    }
}
