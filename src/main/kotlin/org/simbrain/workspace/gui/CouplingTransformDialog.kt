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
import org.simbrain.workspace.couplings.Coupling
import org.simbrain.workspace.couplings.CouplingOperation
import org.simbrain.workspace.couplings.couplingOperationTypes
import org.simbrain.workspace.gui.couplingmanager.DesktopCouplingManager
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
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

class CouplingTransformDialog(
    private val producer: Producer,
    private val consumer: Consumer,
    initialTransforms: List<CouplingOperation<*, *>>,
    private val onCommit: (List<CouplingOperation<*, *>>) -> Unit
) : StandardDialog() {

    /**
     * The chain under edit. Copies, so parameter edits and cancel never touch a live coupling.
     */
    private val chain = initialTransforms.map { it.copy() as CouplingOperation<*, *> }.toMutableList()

    private val chainListModel = DefaultListModel<CouplingOperation<*, *>>()
    private val chainList = JList(chainListModel)
    private val availableListModel = DefaultListModel<Class<out CouplingOperation<*, *>>>()
    private val availableList = JList(availableListModel)
    private val statusLabel = JLabel()

    /**
     * One throwaway instance per operation type, for display names and endpoint types in the list.
     */
    private val prototypes = couplingOperationTypes.associateWith {
        it.getDeclaredConstructor().newInstance() as CouplingOperation<*, *>
    }

    init {
        title = "Transforms: ${producer.simpleDescription} > ${consumer.simpleDescription}"
        setContentPane(buildContent())
        addCommitTask { onCommit(chain.toList()) }
        couplingOperationTypes.forEach { @Suppress("UNCHECKED_CAST") availableListModel.addElement(it as Class<out CouplingOperation<*, *>>) }
        refreshChainList()
        pack()
    }

    private fun buildContent(): JPanel {
        val mainPanel = JPanel(BorderLayout(0, Theme.componentGap))
        mainPanel.applyDialogPadding()

        val endpointsPanel = JPanel(FlowLayout(FlowLayout.LEFT, Theme.componentGap, 0)).apply {
            border = Theme.sectionBorder("Coupling")
            add(JLabel("From:"))
            add(JLabel("${producer.simpleDescription} (${typeDisplay(producer.type)})").apply {
                foreground = DesktopCouplingManager.getColor(producer.type)
            })
            add(JLabel("To:"))
            add(JLabel("${consumer.simpleDescription} (${typeDisplay(consumer.type)})").apply {
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
                prototypes[value]?.let { text = describe(it) }
                return this
            }
        }
        availableList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) addSelectedOperation()
            }
        })

        panel.add(JScrollPane(availableList).apply { preferredSize = Dimension(210, 220) }, BorderLayout.CENTER)
        panel.add(JButton("Add").apply { addActionListener { addSelectedOperation() } }, BorderLayout.SOUTH)
        return panel
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
                    text = "${index + 1}. ${describe(value)}"
                }
                return this
            }
        }
        chainList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) editSelectedOperation()
            }
        })

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
        chain.add(type.getDeclaredConstructor().newInstance() as CouplingOperation<*, *>)
        refreshChainList()
        chainList.selectedIndex = chain.lastIndex
    }

    private fun editSelectedOperation() {
        val operation = chainList.selectedValue ?: return
        val editor = AnnotatedPropertyEditor(operation)
        StandardDialog().apply {
            setContentPane(editor)
            title = "Edit ${operation.name}"
            addCommitTask {
                editor.commitChanges()
                chainList.repaint()
            }
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
    }

    private fun updateStatus() {
        val error = Coupling.chainError(producer, consumer, chain)
        if (error == null) {
            statusLabel.text = "Chain types match: " +
                    (listOf(typeDisplay(producer.type)) + chain.map { typeDisplay(it.outputType) }).joinToString(" > ")
            statusLabel.foreground = Theme.mutedText
            okButton.isEnabled = true
        } else {
            statusLabel.text = error
            statusLabel.foreground = Theme.errorText
            okButton.isEnabled = false
        }
    }

    private fun describe(operation: CouplingOperation<*, *>) =
        "${operation.name} (${typeDisplay(operation.inputType)} > ${typeDisplay(operation.outputType)})"
}

private fun typeDisplay(type: Type): String = when (type) {
    Double::class.java, java.lang.Double::class.java -> "Number"
    DoubleArray::class.java -> "Array"
    String::class.java -> "Text"
    else -> (type as? Class<*>)?.simpleName ?: type.toString()
}

/**
 * Edit the transform chain of an existing coupling; OK replaces the coupling's chain in place.
 */
fun showTransformEditor(parent: Component?, workspace: Workspace, coupling: Coupling) {
    CouplingTransformDialog(coupling.producer, coupling.consumer, coupling.transforms) { transforms ->
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
    CouplingTransformDialog(producer, consumer, emptyList()) { transforms ->
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
