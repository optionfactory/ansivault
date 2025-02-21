package net.optionfactory.jetbrains.ansivault.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo


class VaultAction : AnAction() {
    val logger = Logger.getInstance(VaultAction::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        logger.warn("SomeAction performed %s".format(event))
        val document = event.getData(PlatformDataKeys.EDITOR)?.document
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val ansibleVaultSecret = AnsibleVaultSecret.search(project)

        val editor: Editor? = event.getData(CommonDataKeys.EDITOR)
        if (editor == null) {
            return
        }
        val caretModel: CaretModel = editor.caretModel
        val selectedText = caretModel.currentCaret.selectedText
        logger.warn("SomeAction selectedText %s".format(selectedText))
        val indentSize = caretModel.currentCaret.selectionStartPosition.column
        selectedText.let {
            var text = it!!

            if (text.startsWith(INLINE_VAULT)) {
                text = text.lines().drop(1).joinToString("\n").trimIndent()
            }

            val result = if (VaultInfo(text).isEncryptedVault) {
                logger.warn("Decrypting")
                ansibleVaultSecret.decrypt(text)
            } else {
                logger.warn("Encrypting")
                val encrypted = ansibleVaultSecret.encrypt(selectedText, indentSize)
                if (caretModel.currentCaret.selectionStartPosition == VisualPosition(0, 0)) {
                    encrypted
                } else {
                    "$INLINE_VAULT$encrypted"
                }
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
        const val INLINE_VAULT = "!vault |\n"
    }
}
