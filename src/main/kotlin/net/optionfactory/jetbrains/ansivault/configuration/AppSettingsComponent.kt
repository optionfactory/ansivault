package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import net.optionfactory.jetbrains.ansivault.VaultLookup
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.Box
import java.awt.Dimension
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


class AppSettingsComponent(private val project: Project) {
    var mainPanel: JPanel? = null
    var jButton = JButton("Forget Saved Passwords").apply { isEnabled = false }
    val encryptVaultId = JBTextField()
    val placementButtons = linkedMapOf(
        VaultEditorPlacement.SPLIT_RIGHT to JBRadioButton("Right half of the editor"),
        VaultEditorPlacement.SPLIT_LEFT to JBRadioButton("Left half of the editor"),
        VaultEditorPlacement.SPLIT_TOP to JBRadioButton("Upper half of the editor"),
        VaultEditorPlacement.SPLIT_BOTTOM to JBRadioButton("Bottom half of the editor"),
        VaultEditorPlacement.NEW_TAB to JBRadioButton("New tab"),
    )
    private val lookupTree = Tree(DefaultTreeModel(DefaultMutableTreeNode())).apply {
        isRootVisible = false
        showsRootHandles = true
        cellRenderer = LookupRenderer()
    }

    val selectedPlacement: VaultEditorPlacement
        get() = placementButtons.entries.firstOrNull { it.value.isSelected }?.key ?: VaultEditorPlacement.SPLIT_RIGHT

    fun select(placement: VaultEditorPlacement) {
        placementButtons.forEach { (p, button) -> button.isSelected = p == placement }
    }

    private val forgetSelected = JButton("Forget Password of Selected Vault Id").apply { isEnabled = false }

    private val selectedSavedVaultId: String?
        get() = ((lookupTree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject as? VaultLookupTree.Node)?.savedVaultId

    init {
        jButton.addActionListener {
            refreshLookup { CredentialManager.deletePasswords(project) }
        }
        forgetSelected.addActionListener {
            val vaultId = selectedSavedVaultId ?: return@addActionListener
            refreshLookup { CredentialManager.deletePassword(project, vaultId) }
        }
        lookupTree.addTreeSelectionListener { forgetSelected.isEnabled = selectedSavedVaultId != null }

        ButtonGroup().apply {
            placementButtons.values.forEach { add(it) }
        }
        select(VaultEditorPlacement.SPLIT_RIGHT)

        val refresh = JButton("Refresh", AllIcons.Actions.Refresh).apply { addActionListener { refreshLookup() } }
        val lookupPanel = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            add(JBScrollPane(lookupTree).apply { minimumSize = Dimension(0, JBUI.scale(200)) }, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                add(refresh)
                add(Box.createHorizontalStrut(JBUI.scale(8)))
                add(forgetSelected)
            }, BorderLayout.SOUTH)
        }
        refreshLookup()

        val formBuilder = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("If dialog is used to save the password you can forget from here:"))
            .addComponent(jButton)
            .addVerticalGap(10)
            .addLabeledComponent("Vault id used to encrypt:", encryptVaultId)
            .addComponent(JBLabel("Needed only with multiple vault ids; empty = vault_encrypt_identity from ansible.cfg"))
            .addVerticalGap(10)
            .addComponent(JBLabel("Where to open the vault editor:"))
        placementButtons.values.forEach { formBuilder.addComponent(it) }
        mainPanel = formBuilder
            .addVerticalGap(10)
            .addComponent(JBLabel("Where the vault passwords come from (select a saved one to forget it):"))
            .addComponentFillVertically(lookupPanel, 0)
            .panel
    }

    /**
     * Rebuilds the tree from the current config files, environment and credential store, after running [before].
     * The credential store is slow (it can be the OS keychain): it is read in background, never on the EDT.
     */
    fun refreshLookup(before: () -> Unit = {}) {
        jButton.isEnabled = false
        forgetSelected.isEnabled = false
        val shorten = VaultLookupTree.pathShortener(project.basePath, System.getProperty("user.home"))
        val application = ApplicationManager.getApplication()
        application.executeOnPooledThread {
            before()
            val nodes = VaultLookupTree.build(VaultLookup.resolve(project), shorten)
            val saved = CredentialManager.hasPasswords(project)
            // only Swing components are touched: safe in any modality, the settings dialog is modal
            application.invokeLater({ show(nodes, saved) }, ModalityState.any())
        }
    }

    private fun show(nodes: List<VaultLookupTree.Node>, saved: Boolean) {
        jButton.text = if (saved) "Forget Saved Passwords" else "No Password saved"
        jButton.isEnabled = saved
        val root = DefaultMutableTreeNode()
        nodes.forEach { root.add(toSwing(it)) }
        lookupTree.model = DefaultTreeModel(root)
        var row = 0
        while (row < lookupTree.rowCount) {
            lookupTree.expandRow(row++)
        }
    }

    private fun toSwing(node: VaultLookupTree.Node): DefaultMutableTreeNode {
        return DefaultMutableTreeNode(node).apply { node.children.forEach { add(toSwing(it)) } }
    }

    private class LookupRenderer : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean,
        ) {
            val node = (value as? DefaultMutableTreeNode)?.userObject as? VaultLookupTree.Node ?: return
            icon = when (node.kind) {
                VaultLookupTree.Kind.GROUP -> AllIcons.Nodes.Folder
                VaultLookupTree.Kind.OK -> AllIcons.General.InspectionsOK
                VaultLookupTree.Kind.INFO -> AllIcons.General.Information
                VaultLookupTree.Kind.WARNING -> AllIcons.General.Warning
                VaultLookupTree.Kind.UNUSED -> AllIcons.General.Remove
            }
            val attributes = when (node.kind) {
                VaultLookupTree.Kind.GROUP -> SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                VaultLookupTree.Kind.UNUSED -> SimpleTextAttributes.GRAYED_ATTRIBUTES
                else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
            }
            append(node.text, attributes)
            node.detail?.let { append("  $it", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES) }
        }
    }
}
