package net.optionfactory.jetbrains.ansivault.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerBase
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import net.optionfactory.jetbrains.ansivault.AnsibleVaultSecret
import net.optionfactory.jetbrains.ansivault.configuration.VaultEditorPlacement
import net.optionfactory.jetbrains.ansivault.configuration.VaultSettings
import net.optionfactory.jetbrains.ansivault.gutter.VaultBlock
import net.optionfactory.jetbrains.ansivault.gutter.VaultBlocks
import javax.swing.JSplitPane

@Service(Service.Level.PROJECT)
class VaultFragmentService(private val project: Project) : Disposable {

    private val fragments = mutableMapOf<VirtualFile, VaultFragment>()

    init {
        project.messageBus.connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                fragments.remove(file)?.range?.dispose()
            }
        })
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(FileDocumentManagerListener.TOPIC, object : FileDocumentManagerListener {
            override fun beforeDocumentSaving(document: Document) {
                val fragment = fragments.values.firstOrNull { it.document === document } ?: return
                ApplicationManager.getApplication().invokeLater { commit(fragment) }
            }
        })
    }

    fun open(editor: Editor, marker: RangeHighlighter) {
        if (!marker.isValid) {
            return
        }
        val document = editor.document
        val block = VaultBlocks.at(document, document.getLineNumber(marker.startOffset)) ?: return
        if (fragments.values.any { it.range.isValid && it.range.document === document && it.range.startOffset == block.startOffset }) {
            return
        }
        val secret = AnsibleVaultSecret.search(project) ?: return
        val plainText = try {
            secret.decrypt(block.vaultText)
        } catch (e: Exception) {
            Messages.showErrorDialog(project, e.message ?: "Unable to decrypt the vault", "Ansible Vault")
            return
        }
        val file = LightVirtualFile(tabName(document, block), PlainTextFileType.INSTANCE, plainText)
        file.putUserData(FileDocumentManagerBase.TRACK_NON_PHYSICAL, true)
        val fragmentDocument = FileDocumentManager.getInstance().getDocument(file) ?: return
        fragments[file] = VaultFragment(
            range = document.createRangeMarker(block.startOffset, block.endOffset),
            indent = block.indent,
            secret = secret,
            document = fragmentDocument,
            plainText = plainText,
        )
        when (val placement = VaultSettings.getInstance(project).state.editorPlacement) {
            VaultEditorPlacement.NEW_TAB -> FileEditorManager.getInstance(project).openFile(file, true)
            else -> openSplit(file, placement)
        }
    }

    private fun openSplit(file: LightVirtualFile, placement: VaultEditorPlacement) {
        val manager = FileEditorManagerEx.getInstanceEx(project)
        val window = manager.currentWindow
        if (window == null) {
            manager.openFile(file, true)
            return
        }
        val orientation = when (placement) {
            VaultEditorPlacement.SPLIT_TOP, VaultEditorPlacement.SPLIT_BOTTOM -> JSplitPane.VERTICAL_SPLIT
            else -> JSplitPane.HORIZONTAL_SPLIT
        }
        val fileIsSecondaryComponent = placement == VaultEditorPlacement.SPLIT_RIGHT || placement == VaultEditorPlacement.SPLIT_BOTTOM
        window.split(orientation, true, file, true, fileIsSecondaryComponent)
    }

    private fun tabName(document: Document, block: VaultBlock): String {
        val fileName = FileDocumentManager.getInstance().getFile(document)?.name ?: "ansible-vault.txt"
        val headerLine = document.getLineNumber(block.startOffset)
        val keyPath = VaultBlocks.keyPath(VaultBlocks.lines(document), headerLine)
        return if (keyPath.isEmpty()) "[AnsiVault] $fileName" else "[AnsiVault] $fileName: $keyPath"
    }

    private fun commit(fragment: VaultFragment) {
        val range = fragment.range
        if (!range.isValid) {
            return
        }
        val text = fragment.document.text
        if (text == fragment.plainText) {
            return
        }
        try {
            val encrypted = fragment.secret.encrypt(text, fragment.indent)
            WriteCommandAction.runWriteCommandAction(project) {
                range.document.replaceString(range.startOffset, range.endOffset, encrypted)
            }
            fragment.plainText = text
        } catch (e: Exception) {
            Messages.showErrorDialog(project, e.message ?: "Unable to encrypt the vault", "Ansible Vault")
        }
    }

    override fun dispose() {
        fragments.values.forEach { it.range.dispose() }
        fragments.clear()
    }

    private class VaultFragment(
        val range: RangeMarker,
        val indent: Int,
        val secret: AnsibleVaultSecret,
        val document: Document,
        var plainText: String,
    )

    companion object {
        fun getInstance(project: Project): VaultFragmentService = project.service()
    }
}
