package net.optionfactory.jetbrains.ansivault.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import net.optionfactory.jetbrains.ansivault.ui.VaultDecryption

/** Encrypts the selected text, or decrypts it when it is a vault (optionally with its `!vault |` tag). */
class VaultAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabled = event.project != null
            && editor != null
            && editor.document.isWritable
            && editor.selectionModel.hasSelection()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val caret = editor.caretModel.primaryCaret
        val selectedText = caret.selectedText ?: return
        val start = caret.selectionStart
        val end = caret.selectionEnd

        val vaultText = INLINE_VAULT.find(selectedText)
            ?.let { selectedText.substring(it.range.last + 1).trimIndent() }
            ?: selectedText

        val result = if (VaultInfo(vaultText).isEncryptedVault) {
            logger.debug("Decrypting")
            VaultDecryption.decryptForEdit(project, vaultText)?.first ?: return
        } else {
            logger.debug("Encrypting")
            val secret = AnsibleVaultSecret.search(project) ?: return
            val indent = start - document.getLineStartOffset(document.getLineNumber(start))
            val encrypted = try {
                secret.encrypt(selectedText, indent)
            } catch (e: Exception) {
                VaultDecryption.showError(project, e)
                return
            }
            // a whole file vault starts at the beginning of the file, anything else is a YAML value
            if (start == 0) encrypted else "$INLINE_VAULT_TAG\n$encrypted"
        }

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, result)
        }
    }

    companion object {
        private val logger = Logger.getInstance(VaultAction::class.java)

        const val INLINE_VAULT_TAG = "!vault |"

        /** `!vault |`, also with a block chomping indicator or trailing spaces. */
        val INLINE_VAULT = Regex("""^!vault\s*\|[-+]?[ \t]*\r?\n""")
    }
}
