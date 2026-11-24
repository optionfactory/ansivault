package net.optionfactory.jetbrains.ansivault.actions

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret
import net.optionfactory.jetbrains.ansivault.crypto.data.VaultInfo
import net.optionfactory.jetbrains.ansivault.gutter.VaultBlocks
import net.optionfactory.jetbrains.ansivault.ui.VaultDecryption
import net.optionfactory.jetbrains.ansivault.ui.VaultFragmentService

/**
 * With a selection: encrypts it, or decrypts it when it is a vault (optionally with its `!vault |` tag).
 * Without a selection: opens the vault editor when the caret is on a vault,
 * or encrypts the value of the `key: value` line at the caret as an inline vault.
 */
class VaultAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabled = event.project != null && editor != null && editor.document.isWritable
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        if (editor.caretModel.primaryCaret.hasSelection()) {
            processSelection(project, editor)
        } else {
            processCaretLine(project, editor)
        }
    }

    private fun processSelection(project: Project, editor: Editor) {
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
            val indent = start - document.getLineStartOffset(document.getLineNumber(start))
            val encrypted = encrypt(project, selectedText, indent) ?: return
            // a whole file vault starts at the beginning of the file, anything else is a YAML value
            if (start == 0) encrypted else "$INLINE_VAULT_TAG\n$encrypted"
        }

        WriteCommandAction.runWriteCommandAction(project, COMMAND_NAME, null, {
            document.replaceString(start, end, result)
        })
    }

    private fun processCaretLine(project: Project, editor: Editor) {
        val document = editor.document
        val line = document.getLineNumber(editor.caretModel.offset)
        val lines = VaultBlocks.lines(document)

        VaultBlocks.enclosingHeader(lines, line)?.let { headerLine ->
            VaultFragmentService.getInstance(project).open(editor, headerLine)
            return
        }

        val lineText = lines[line].toString()
        val yamlValue = YamlValueLine.parse(lineText)
        if (yamlValue == null) {
            HintManager.getInstance().showErrorHint(
                editor,
                "Select the text to encrypt, or place the caret on a vault or on a 'key: value' line",
            )
            return
        }
        val encrypted = encrypt(project, yamlValue.value, yamlValue.keyColumn + VALUE_INDENT) ?: return
        val tag = INLINE_VAULT_TAG + (yamlValue.comment?.let { " $it" } ?: "")
        val lineStart = document.getLineStartOffset(line)
        WriteCommandAction.runWriteCommandAction(project, COMMAND_NAME, null, {
            document.replaceString(lineStart + yamlValue.valueStart, document.getLineEndOffset(line), "$tag\n$encrypted")
        })
    }

    private fun encrypt(project: Project, text: String, indent: Int): String? {
        val secret = AnsibleVaultSecret.search(project, forEncryption = true) ?: return null
        return try {
            secret.encrypt(text, indent)
        } catch (e: Exception) {
            VaultDecryption.showError(project, e)
            null
        }
    }

    companion object {
        private val logger = Logger.getInstance(VaultAction::class.java)
        private const val COMMAND_NAME = "Ansible Vault"

        const val INLINE_VAULT_TAG = "!vault |"

        /** Indentation of an inline vault relative to its key, like `ansible-vault encrypt_string` output. */
        const val VALUE_INDENT = 2

        /** `!vault |`, also with a block chomping indicator or trailing spaces. */
        val INLINE_VAULT = Regex("""^!vault\s*\|[-+]?[ \t]*\r?\n""")
    }
}
