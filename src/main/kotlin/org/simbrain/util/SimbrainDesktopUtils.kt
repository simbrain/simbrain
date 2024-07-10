package org.simbrain.util

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.awt.BorderLayout
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.io.FileInputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
        launch(Dispatchers.Swing) { mainPanel.addItem(component, 1) }
    }

    fun addSeparator() {
        launch(Dispatchers.Swing) { mainPanel.addItem(JSeparator(SwingConstants.HORIZONTAL)) }
    }

    fun addLabelledText(label: String, text: String) = JLabel(text).also {
        launch(Dispatchers.Swing) {
            mainPanel.addItem(label, it)
        }
    }

    fun addButton(label: String, context: CoroutineContext = EmptyCoroutineContext, task: suspend JButton.(ActionEvent) -> Unit) = JButton(label).also { button ->
        button.addActionListener {
            launch(context) { button.task(it) }
        }
        launch(Dispatchers.Swing) {
            mainPanel.addItem(button)
            pack()
        }
    }

    /**
     * Blocks passed to onChange are applied every time the text field is edited
     * If an onChange block is provided and the values are converted to numbers, [addFormattedNumericTextField]
     * should be used instead.
     */
    fun addTextField(label: String, initValue: String, onChange: (String) -> Unit = {}) = JTextField(initValue).also { textField ->
        textField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                onChange(textField.text)
            }
            override fun removeUpdate(e: DocumentEvent?) {
                onChange(textField.text)
            }
            override fun changedUpdate(e: DocumentEvent?) {
                onChange(textField.text)
            }
        })
        launch(Dispatchers.Swing) {
            mainPanel.addItem(label, textField)
        }
    }

    /**
     * Add a text field that will parse numbers.
     * The initvalue determines the type.
     *
     * Blocks passed to onChange are applied every time the text field is edited
     */
    inline fun <reified T: Any> addFormattedNumericTextField(
        label: String,
        initValue: T,
        columns: Int = 8,
        maximumFractionDigits: Int = 12,
        crossinline onChange: (T) -> Unit
    ) = JFormattedTextField(initValue).also { textField ->
        textField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                onChange(textField.value as T)
            }
            override fun removeUpdate(e: DocumentEvent?) {
                onChange(textField.value as T)
            }
            override fun changedUpdate(e: DocumentEvent?) {
                onChange(textField.value as T)
            }
        })
        launch(Dispatchers.Swing) {
            val format = if (T::class == Int::class || T::class == Long::class || T::class == Short::class) {
                NumberFormat.getIntegerInstance(textField.getLocale())
            } else {
                NumberFormat.getNumberInstance(textField.getLocale()).apply { this.maximumFractionDigits = maximumFractionDigits }
            } as DecimalFormat
            val formatterEditor = NumberFormatter(format)
            formatterEditor.valueClass = T::class.java
            val factory = DefaultFormatterFactory(formatterEditor)
            textField.setFocusLostBehavior(JFormattedTextField.PERSIST)
            textField.columns = columns
            textField.isEditable = true
            textField.setFormatterFactory(factory)
            textField.addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    if (textField.isValid) {
                        textField.commitEdit()
                    }
                }
            })
            mainPanel.addItem(label, textField)
        }
    }

    fun addCheckBox(label: String, checked: Boolean, onChange: suspend (Boolean) -> Unit = {}) = JCheckBox().also { checkBox ->
        checkBox.addActionListener {
            launch {
                onChange(checkBox.isSelected)
            }
        }
        launch(Dispatchers.Swing) {
            checkBox.isSelected = checked
            mainPanel.addItem(label, checkBox)
            pack()
        }
    }

    fun <O: EditableObject> addAnnotatedPropertyEditor(editor: AnnotatedPropertyEditor<O>) {
        launch(Dispatchers.Swing) { mainPanel.addItem(editor) }
    }

}

suspend fun SimbrainDesktop.loadWorkspaceZipFromFileChooser(): Boolean {
    val simulationChooser = SFileChooser(workspace.currentDirectory, "Zip Archive", "zip")
    val simFile = simulationChooser.showOpenDialog()
    val serializer = WorkspaceSerializer(workspace)

    // not calling clearWorkspace because control panels created by simulations should be left in place
    if (simFile != null) {
        workspace.removeAllComponents()
        workspace.updater.updateManager.reset()
        workspace.couplingManager.clear()
        withContext(Dispatchers.IO) {
            serializer.deserialize(FileInputStream(simFile))
        }
        return true
    }
    return false
}