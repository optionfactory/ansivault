package net.optionfactory.jetbrains.ansivault.gutter

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import net.optionfactory.jetbrains.ansivault.ui.VaultFragmentService
import javax.swing.Icon

class VaultGutterIconRenderer(
    private val project: Project,
    private val editor: Editor,
    private val marker: RangeHighlighter,
) : GutterIconRenderer() {

    override fun getIcon(): Icon = ICON

    override fun getTooltipText(): String = "Edit Ansible Vault"

    override fun getClickAction(): AnAction = object : AnAction() {
        override fun actionPerformed(event: AnActionEvent) {
            VaultFragmentService.getInstance(project).open(editor, marker)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is VaultGutterIconRenderer && other.marker == marker
    }

    override fun hashCode(): Int = marker.hashCode()

    companion object {
        private val ICON: Icon = IconLoader.getIcon("/icons/icon.svg", VaultGutterIconRenderer::class.java)
    }
}
