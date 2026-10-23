package net.optionfactory.jetbrains.ansivault.gutter

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key

class VaultGutterListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.project == null) {
            return
        }
        val disposable = Disposer.newDisposable("ansivault.gutter")
        editor.putUserData(DISPOSABLE_KEY, disposable)
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                refreshLater(editor)
            }
        }, disposable)
        refreshLater(editor)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val disposable = event.editor.getUserData(DISPOSABLE_KEY) ?: return
        event.editor.putUserData(DISPOSABLE_KEY, null)
        Disposer.dispose(disposable)
    }

    private fun refreshLater(editor: Editor) {
        ApplicationManager.getApplication().invokeLater {
            if (!editor.isDisposed) {
                refresh(editor)
            }
        }
    }

    private fun refresh(editor: Editor) {
        val project = editor.project ?: return
        val markup = editor.markupModel
        editor.getUserData(MARKERS_KEY)?.forEach { markup.removeHighlighter(it) }
        val document = editor.document
        val markers = VaultBlocks.headerLines(document).map { line ->
            val marker = markup.addRangeHighlighter(
                document.getLineStartOffset(line),
                document.getLineEndOffset(line),
                HighlighterLayer.LAST,
                null,
                HighlighterTargetArea.LINES_IN_RANGE,
            )
            marker.gutterIconRenderer = VaultGutterIconRenderer(project, editor, marker)
            marker
        }
        editor.putUserData(MARKERS_KEY, markers.toMutableList())
    }

    companion object {
        private val MARKERS_KEY = Key.create<MutableList<RangeHighlighter>>("ansivault.gutter.markers")
        private val DISPOSABLE_KEY = Key.create<Disposable>("ansivault.gutter.disposable")
    }
}
