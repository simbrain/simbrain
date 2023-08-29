package org.simbrain.util

import kotlinx.coroutines.*
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.BorderLayout
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Utility to make it easy to set location of a [SimbrainDesktop] component.
 * Note: these are the width and height of the visible window. In an odor world, for example, the width of the
 * world itself in pixels might be much larger.
 */
data class Placement(
    var location: Point? = null,
    var width: Int? = null,
    var height: Int? = null
)

fun SimbrainDesktop.place(workspaceComponent: WorkspaceComponent, placement: Placement.() -> Unit) {
    workspace.launch {
        val (location, width, height) = Placement().apply(placement)
        val desktopComponent = withContext(Dispatchers.Main) {
            getDesktopComponent(workspaceComponent)
        }
        val bounds = desktopComponent.parentFrame.bounds
        desktopComponent.parentFrame.bounds = Rectangle(
            location?.x ?: bounds.x,
            location?.y ?: bounds.y,
            width ?: bounds.width,
            height ?: bounds.height
        )
    }
}

fun SimbrainDesktop.place(workspaceComponent: WorkspaceComponent, x: Int, y: Int, width: Int, height: Int) {
    val desktopComponent = getDesktopComponent(workspaceComponent)
    desktopComponent.parentFrame.bounds = Rectangle(x, y, width, height)
}

suspend fun SimulationScope.place(workspaceComponent: WorkspaceComponent, x: Int, y: Int, width: Int, height: Int) {
    withGui {
        val desktopComponent = getDesktopComponent(workspaceComponent)
        desktopComponent.parentFrame.bounds = Rectangle(x, y, width, height)
    }
}

class ControlPanelKt(title: String = "Control Panel"): JInternalFrame(title, true, true), CoroutineScope {

    @Transient
    private var job = SupervisorJob()

    @Transient
    override var coroutineContext = Dispatchers.Default + job

    val centralPanel = JPanel(BorderLayout()).apply {
        val scrollPane = JScrollPane(this)
        scrollPane.border = null
        this@ControlPanelKt.add(BorderLayout.CENTER, this)
    }

    val mainPanel = LabelledItemPanel().apply {
        centralPanel.add(this)
    }

    fun addComponent(component: JComponent) {
        mainPanel.addItem(component, 1)
    }

    fun addSeparator() {
        addComponent(JSeparator(SwingConstants.HORIZONTAL))
    }

    fun addButton(label: String, task: suspend (ActionEvent) -> Unit) = JButton(label).apply {
        addActionListener {
            launch { task(it) }
        }
        mainPanel.addItem(this)
        pack()
    }

    fun addTextField(label: String, initValue: String, onChange: (String) -> Unit = {}) = JTextField(initValue).apply {
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                onChange(text)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                onChange(text)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                onChange(text)
            }

        })
        mainPanel.addItem(label, this)
    }

    fun addCheckBox(label: String, checked: Boolean, task: suspend (ActionEvent) -> Unit = {}) = JCheckBox().apply {
        isSelected = checked
        mainPanel.addItem(label, this)
        addActionListener {
            launch {
                task(it)
            }
        }
        pack()
    }

}