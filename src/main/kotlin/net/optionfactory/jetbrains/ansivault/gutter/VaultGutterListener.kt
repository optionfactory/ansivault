package net.optionfactory.jetbrains.ansivault.gutter

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import javax.swing.Timer

/** Adds a gutter icon on the header of each vault of the file editors; refreshed when typing pauses. */
class VaultGutterListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (!isFileEditor(editor)) {
            return
        }
        val disposable = Disposer.newDisposable("ansivault.gutter")
        editor.putUserData(DISPOSABLE_KEY, disposable)
        // a Swing timer fires on the EDT; restart() postpones the refresh while typing
        val timer = Timer(REFRESH_DELAY_MS) {
            if (!editor.isDisposed) {
                refresh(editor)
            }
        }
        timer.isRepeats = false
        Disposer.register(disposable) { timer.stop() }
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                timer.restart()
            }
        }, disposable)
        timer.start()
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val disposable = event.editor.getUserData(DISPOSABLE_KEY) ?: return
        event.editor.putUserData(DISPOSABLE_KEY, null)
        Disposer.dispose(disposable)
    }

    /** Main, writable editors of a file: no consoles, diffs, previews or viewers. */
    private fun isFileEditor(editor: Editor): Boolean {
        return editor.project != null
            && editor.editorKind == EditorKind.MAIN_EDITOR
            && !editor.isViewer
            && FileDocumentManager.getInstance().getFile(editor.document) != null
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
        editor.putUserData(MARKERS_KEY, markers)
    }

    companion object {
        private const val REFRESH_DELAY_MS = 300
        private val MARKERS_KEY = Key.create<List<RangeHighlighter>>("ansivault.gutter.markers")
        private val DISPOSABLE_KEY = Key.create<Disposable>("ansivault.gutter.disposable")
    }
}
