/**
 * Editor for a coupling's transform chain: available operations on the left, the chain on the right
 * with reorder and per-operation property editing, and a live end-to-end type check that keeps OK
 * disabled while the chain does not fit the coupling's endpoints. Edits are made on copies and applied
 * only on OK, through [org.simbrain.workspace.couplings.CouplingManager.setTransforms] for an existing
 * coupling or coupling creation for a new one — the entry points for both are the top-level show
 * functions, which the Java coupling manager panels call.
 */
package org.simbrain.workspace.gui

import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.Theme
import org.simbrain.util.applyDialogPadding
import org.simbrain.util.display
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.showWarningDialog
import org.simbrain.workspace.Consumer
import org.simbrain.workspace.MismatchedAttributesException
import org.simbrain.workspace.Producer
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.attributeTypeColor
import org.simbrain.workspace.attributeTypeName
import org.simbrain.workspace.attributeTypesMatch
import org.simbrain.workspace.couplings.Coupling
import org.simbrain.workspace.couplings.BroadcastOperation
import org.simbrain.workspace.couplings.CouplingOperation
import org.simbrain.workspace.couplings.couplingOperationTypes
import org.simbrain.workspace.gui.couplingmanager.DesktopCouplingManager
import smile.math.matrix.Matrix
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.lang.reflect.Type
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

class CouplingTransformDialog(
    private val producer: Producer,
    private val consumer: Consumer,
    initialTransforms: List<CouplingOperation<*, *>>,
    private val broadcastSizeHint: Int? = null,
    owner: Window? = null,
    private val onCommit: (List<CouplingOperation<*, *>>) -> Unit
) : StandardDialog(owner, "") {

    /**
     * The chain under edit. Copies, so parameter edits and cancel never touch a live coupling.
     */
    private val chain = initialTransforms.map { it.copy() as CouplingOperation<*, *> }.toMutableList()

    private val chainListModel = DefaultListModel<CouplingOperation<*, *>>()
    private val chainList = JList(chainListModel)
    private val availableListModel = DefaultListModel<Class<out CouplingOperation<*, *>>>()

    private val availableList = object : JList<Class<out CouplingOperation<*, *>>>(availableListModel) {
        override fun getToolTipText(event: MouseEvent): String? {
            val index = locationToIndex(event.point).takeIf { it >= 0 } ?: return null
            if (!getCellBounds(index, index).contains(event.point)) return null
            val operation = prototypes[model.getElementAt(index)] ?: return null
            val slotType = typeAt(insertIndex)
            if (attributeTypesMatch(slotType, operation.inputType)) return null
            return "Takes ${operation.inputType.attributeTypeName}, but the chain carries " +
                    "${slotType.attributeTypeName} at item ${insertIndex + 1}"
        }
    }

    private val statusLabel = JLabel()
    private val addButton = JButton()

    /**
     * Index of the first operation whose input does not match what the chain feeds it, or -1 when
     * every link fits; the chain renderer marks that operation.
     */
    private var firstMismatchIndex = -1

    /**
     * One throwaway instance per operation type, for display names and endpoint types in the list.
     */
    private val prototypes = couplingOperationTypes.associateWith {
        it.getDeclaredConstructor().newInstance() as CouplingOperation<*, *>
    }

    init {
        title = "Transforms: ${producer.simpleDescription} \u2192 ${consumer.simpleDescription}"
        setContentPane(buildContent())
        addCommitTask { onCommit(chain.toList()) }
        couplingOperationTypes.forEach { @Suppress("UNCHECKED_CAST") availableListModel.addElement(it as Class<out CouplingOperation<*, *>>) }
        ToolTipManager.sharedInstance().registerComponent(availableList)
        refreshChainList()
        pack()
    }

    private fun buildContent(): JPanel {
        val mainPanel = JPanel(BorderLayout(0, Theme.componentGap))
        mainPanel.applyDialogPadding()

        val endpointsPanel = JPanel(FlowLayout(FlowLayout.LEFT, Theme.componentGap, 0)).apply {
            border = Theme.sectionBorder("Coupling")
            add(JLabel("From:"))
            add(JLabel("${producer.simpleDescription} (${producer.type.attributeTypeName})").apply {
                foreground = DesktopCouplingManager.getColor(producer.type)
            })
            add(JLabel("To:"))
            add(JLabel("${consumer.simpleDescription} (${consumer.type.attributeTypeName})").apply {
                foreground = DesktopCouplingManager.getColor(consumer.type)
            })
        }
        mainPanel.add(endpointsPanel, BorderLayout.NORTH)

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = buildAvailablePanel()
            rightComponent = buildChainPanel()
            resizeWeight = 0.4
        }
        mainPanel.add(splitPane, BorderLayout.CENTER)

        mainPanel.add(statusLabel, BorderLayout.SOUTH)
        return mainPanel
    }

    private fun buildAvailablePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = Theme.sectionBorder("Available Transforms")

        availableList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        availableList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                prototypes[value]?.let {
                    val dimmed = !fitsAtInsertion(it)
                    text = operationLabel(it.name, it, plain = dimmed || isSelected)
                    // Grey out what cannot follow the chain at the insertion point
                    if (dimmed && !isSelected) {
                        foreground = Theme.mutedText
                    }
                }
                return this
            }
        }
        availableList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) addSelectedOperation()
            }
        })
        availableList.addListSelectionListener { updateAddButton() }

        panel.add(JScrollPane(availableList).apply { preferredSize = Dimension(210, 220) }, BorderLayout.CENTER)
        panel.add(addButton.apply { addActionListener { addSelectedOperation() } }, BorderLayout.SOUTH)
        return panel
    }

    /**
     * Where the next Add lands: after the selected chain item, or at the end when nothing is selected.
     */
    private val insertIndex: Int
        get() = if (chainList.selectedIndex >= 0) chainList.selectedIndex + 1 else chain.size

    /**
     * The type flowing into position [index]: what the operation before it yields, or the producer's
     * type at the head of the chain.
     */
    private fun typeAt(index: Int): java.lang.reflect.Type =
        chain.getOrNull(index - 1)?.outputType ?: producer.type

    private fun fitsAtInsertion(operation: CouplingOperation<*, *>) =
        attributeTypesMatch(typeAt(insertIndex), operation.inputType)

    private fun updateAddButton() {
        addButton.text = "Add as item ${insertIndex + 1}"
        val selected = availableList.selectedValue?.let { prototypes[it] }
        addButton.isEnabled = selected != null && fitsAtInsertion(selected)
        availableList.repaint()
    }

    private fun buildChainPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = Theme.sectionBorder("Transform Chain")

        chainList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        chainList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is CouplingOperation<*, *>) {
                    val mismatched = index == firstMismatchIndex
                    text = operationLabel("${index + 1}. ${value.displayLabel}", value, plain = mismatched || isSelected)
                    if (!isSelected && mismatched) {
                        foreground = Theme.errorText
                    }
                }
                return this
            }
        }
        chainList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) editSelectedOperation()
            }
        })
        chainList.addListSelectionListener { updateAddButton() }

        panel.add(JScrollPane(chainList).apply { preferredSize = Dimension(260, 220) }, BorderLayout.CENTER)

        val controls = JPanel(FlowLayout()).apply {
            add(JButton("Edit").apply { addActionListener { editSelectedOperation() } })
            add(JButton(ResourceManager.getSmallIcon("menu_icons/Up.png")).apply {
                toolTipText = "Move up"
                addActionListener { moveSelected(-1) }
            })
            add(JButton(ResourceManager.getSmallIcon("menu_icons/Down.png")).apply {
                toolTipText = "Move down"
                addActionListener { moveSelected(1) }
            })
            add(JButton("Remove").apply { addActionListener { removeSelectedOperation() } })
        }
        panel.add(controls, BorderLayout.SOUTH)
        return panel
    }

    private fun addSelectedOperation() {
        val type = availableList.selectedValue ?: return
        if (prototypes[type]?.let { fitsAtInsertion(it) } != true) return
        val position = insertIndex
        val operation = type.getDeclaredConstructor().newInstance() as CouplingOperation<*, *>
        // Default a broadcast to the size the consumer's container carries, when that could be sampled
        if (operation is BroadcastOperation && broadcastSizeHint != null) {
            operation.size = broadcastSizeHint
        }
        chain.add(position, operation)
        refreshChainList()
        chainList.selectedIndex = position
    }

    private fun editSelectedOperation() {
        val operation = chainList.selectedValue ?: return
        val editor = AnnotatedPropertyEditor(operation)
        // Owned by this dialog so it stays above it and remains interactable while this dialog is modal
        StandardDialog(this, "Edit ${operation.name}").apply {
            setContentPane(editor)
            addCommitTask {
                editor.commitChanges()
                chainList.repaint()
            }
            isModal = true
            pack()
            setLocationRelativeTo(this@CouplingTransformDialog)
            isVisible = true
        }
    }

    private fun moveSelected(offset: Int) {
        val index = chainList.selectedIndex
        val target = index + offset
        if (index < 0 || target < 0 || target > chain.lastIndex) return
        val operation = chain.removeAt(index)
        chain.add(target, operation)
        refreshChainList()
        chainList.selectedIndex = target
    }

    private fun removeSelectedOperation() {
        val index = chainList.selectedIndex
        if (index < 0) return
        chain.removeAt(index)
        refreshChainList()
    }

    private fun refreshChainList() {
        chainListModel.clear()
        chain.forEach { chainListModel.addElement(it) }
        updateStatus()
        updateAddButton()
    }

    private fun updateStatus() {
        firstMismatchIndex = chain.indices.firstOrNull { index ->
            !attributeTypesMatch(typeAt(index), chain[index].inputType)
        } ?: -1
        val error = Coupling.chainError(producer, consumer, chain)
        if (error == null) {
            statusLabel.text = "Chain types match: " +
                    (listOf(producer.type.attributeTypeName) + chain.map { it.outputType.attributeTypeName }).joinToString(" \u2192 ")
            statusLabel.foreground = Theme.mutedText
            okButton.isEnabled = true
        } else {
            statusLabel.text = error
            statusLabel.foreground = Theme.errorText
            okButton.isEnabled = false
        }
        chainList.repaint()
    }

    /**
     * "Label (In \u2192 Out)" with the type names tinted in their data-type colors; [plain] drops the
     * markup so selection and error foregrounds stay readable.
     */
    private fun operationLabel(label: String, operation: CouplingOperation<*, *>, plain: Boolean): String {
        val input = operation.inputType
        val output = operation.outputType
        return if (plain) {
            "$label (${input.attributeTypeName} \u2192 ${output.attributeTypeName})"
        } else {
            "<html>$label (<font color='${input.attributeTypeColor.hex}'>${input.attributeTypeName}</font> \u2192 " +
                    "<font color='${output.attributeTypeColor.hex}'>${output.attributeTypeName}</font>)</html>"
        }
    }
}

/**
 * The array size the consumer's side of a coupling carries, or null when it cannot be determined:
 * sampled from the array and matrix producers on the consumer's own container (a neuron array's
 * activations, a data world's row), which by convention match what its consumers accept. Only
 * non-suspending producers are sampled, and only an unambiguous answer counts — if the container's
 * producers disagree about the size, there is no hint.
 */
fun inferTargetArraySize(workspace: Workspace, consumer: Consumer): Int? = with(workspace.couplingManager) {
    consumer.baseObject.producers
        .mapNotNull { candidate ->
            when (val value = candidate.tryGetValueNow()) {
                is DoubleArray -> value.size
                is Matrix -> value.size().toInt()
                else -> null
            }
        }
        .distinct()
        .singleOrNull()
        ?.takeIf { it > 0 }
}

/**
 * The window a spawning component lives in, so the editor can be owned by it: ownership, not
 * modality, is what keeps a dialog stacked above its parent.
 */
private val Component?.windowAncestor: Window?
    get() = this as? Window ?: this?.let { SwingUtilities.getWindowAncestor(it) }

private val java.awt.Color.hex: String
    get() = "#%02x%02x%02x".format(red, green, blue)

/**
 * Edit the transform chain of an existing coupling; OK replaces the coupling's chain in place.
 */
fun showTransformEditor(parent: Component?, workspace: Workspace, coupling: Coupling) {
    CouplingTransformDialog(
        coupling.producer, coupling.consumer, coupling.transforms,
        broadcastSizeHint = inferTargetArraySize(workspace, coupling.consumer),
        owner = parent.windowAncestor
    ) { transforms ->
        try {
            workspace.couplingManager.setTransforms(coupling, transforms)
        } catch (e: MismatchedAttributesException) {
            showWarningDialog(e.message ?: "Transform chain does not match the coupling", "Transforms")
        }
    }.apply {
        setLocationRelativeTo(parent)
        display()
    }
}

/**
 * Create a new coupling from [producer] to [consumer] through a transform chain built in the editor;
 * this is the affordance for couplings whose endpoint types differ (e.g. array to number via a mean).
 */
fun showTransformEditorForNewCoupling(parent: Component?, workspace: Workspace, producer: Producer, consumer: Consumer) {
    CouplingTransformDialog(
        producer, consumer, emptyList(),
        broadcastSizeHint = inferTargetArraySize(workspace, consumer),
        owner = parent.windowAncestor
    ) { transforms ->
        try {
            workspace.couplingManager.createCoupling(producer, consumer, transforms = transforms)
        } catch (e: MismatchedAttributesException) {
            showWarningDialog(e.message ?: "Transform chain does not match the endpoints", "Transforms")
        }
    }.apply {
        setLocationRelativeTo(parent)
        display()
    }
}
