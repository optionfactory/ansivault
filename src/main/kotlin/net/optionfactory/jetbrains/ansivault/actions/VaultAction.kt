package net.optionfactory.jetbrains.ansivault.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo


class VaultAction : AnAction() {
    val logger = Logger.getInstance(VaultAction::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        logger.warn("SomeAction performed %s".format(event))
        val document = event.getData(PlatformDataKeys.EDITOR)?.document
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val ansibleVaultSecret = AnsibleVaultSecret.search(project)

        val editor: Editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel: CaretModel = editor.caretModel
        val selectedText = caretModel.currentCaret.selectedText
        logger.warn("SomeAction selectedText %s".format(selectedText))
        val indentSize = caretModel.currentCaret.selectionStartPosition.column
        selectedText.let {
            val text = it!!
            val result = if (text.startsWith(VAULT_MAGIC)) {
                val ansibleVault = text.lines().drop(1).joinToString("\n").trimIndent()
                if (!VaultInfo(ansibleVault).isEncryptedVault) {
                    return
                }
                logger.warn("Decrypting")
                ansibleVaultSecret.decrypt(ansibleVault)
            } else {
                logger.warn("Encrypting")
                "%s%s".format(VAULT_MAGIC, ansibleVaultSecret.encrypt(selectedText, indentSize))
            }
            val primaryCaret = editor.caretModel.primaryCaret
            val start = primaryCaret.selectionStart
            val end = primaryCaret.selectionEnd
            WriteCommandAction.runWriteCommandAction(project)
            {
                document!!.replaceString(start, end, result)
            }
        }
    }

    companion object {
        const val VAULT_MAGIC = "!vault |\n"
    }
}
