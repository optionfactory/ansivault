package net.optionfactory.jetbrains.ansivault.configuration

import com.intellij.icons.AllIcons
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
import java.awt.Dimension
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


class AppSettingsComponent(private val project: Project) {
    var mainPanel: JPanel? = null
    var jButton = JButton()
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

    init {
        val saved = CredentialManager.hasPasswords(project)
        jButton.text = if (saved) "Forget Saved Passwords" else "No Password saved"
        jButton.isEnabled = saved
        jButton.addActionListener {
            CredentialManager.deletePasswords(project)
            jButton.text = "No Password saved"
            jButton.isEnabled = false
            refreshLookup()
        }

        ButtonGroup().apply {
            placementButtons.values.forEach { add(it) }
        }
        select(VaultEditorPlacement.SPLIT_RIGHT)

        val refresh = JButton("Refresh", AllIcons.Actions.Refresh).apply { addActionListener { refreshLookup() } }
        val lookupPanel = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            add(JBScrollPane(lookupTree).apply { minimumSize = Dimension(0, JBUI.scale(200)) }, BorderLayout.CENTER)
            add(JPanel(BorderLayout()).apply { add(refresh, BorderLayout.WEST) }, BorderLayout.SOUTH)
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
            .addComponent(JBLabel("Where the vault passwords come from:"))
            .addComponentFillVertically(lookupPanel, 0)
            .panel
    }

    /** Rebuilds the tree from the current config files, environment and credential store. */
    fun refreshLookup() {
        val root = DefaultMutableTreeNode()
        val shorten = VaultLookupTree.pathShortener(project.basePath, System.getProperty("user.home"))
        VaultLookupTree.build(VaultLookup.resolve(project), shorten).forEach { root.add(toSwing(it)) }
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
