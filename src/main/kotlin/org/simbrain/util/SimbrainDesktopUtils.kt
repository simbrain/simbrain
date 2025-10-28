package org.simbrain.util

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.jfree.chart.LegendItem
import org.jfree.chart.LegendItemCollection
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.projection.ProjectionDesktopComponent
import org.simbrain.util.projection.AuxDataColoringManager
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.WorkspacePreferences
import org.simbrain.workspace.gui.DesktopComponent
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
import java.util.*
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

suspend fun SimbrainDesktop.place(workspaceComponent: WorkspaceComponent, placement: suspend Placement.() -> Unit) {
    val (location, width, height) = Placement().apply { placement() }
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

suspend fun SimbrainDesktop.place(workspaceComponent: WorkspaceComponent, x: Int, y: Int, width: Int, height: Int) {
    val desktopComponent = getDesktopComponent(workspaceComponent)
    desktopComponent.parentFrame.bounds = Rectangle(x, y, width, height)
}

fun SimbrainDesktop.placeBlocking(workspaceComponent: WorkspaceComponent, x: Int, y: Int, width: Int, height: Int) {
    val desktopComponent = runBlocking { getDesktopComponent(workspaceComponent) }
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

    val tabbedPane by lazy {
        JTabbedPane()
    }

    fun addTab(tabName: String) {
        launch(Dispatchers.Swing) {
            // If tabbedPane is not yet the main component
            if (centralPanel.components.none { it == tabbedPane }) {
                centralPanel.remove(mainPanel)
                // TODO: Make it possible to NOT have a main panel
                tabbedPane.addTab("Main", mainPanel)
                centralPanel.add(tabbedPane)
                centralPanel.revalidate()
                centralPanel.repaint()
            }
            tabbedPane.addTab(tabName, LabelledItemPanel())
        }
    }

    val mainPanel = LabelledItemPanel().apply {
        centralPanel.add(this)
    }

    fun getTab(name: String?): LabelledItemPanel = (0 until tabbedPane.tabCount)
        .firstOrNull { tabbedPane.getTitleAt(it) == name }
        ?.let { tabbedPane.getComponentAt(it) as? LabelledItemPanel }
        ?: mainPanel
    
    

    fun addComponent(component: JComponent, tab: String? = null) {
        launch(Dispatchers.Swing) { getTab(tab).addItem(component) }
    }

    fun addSeparator(tab: String? = null) {
        launch(Dispatchers.Swing) { getTab(tab).addItem(JSeparator(SwingConstants.HORIZONTAL)) }
    }

    fun addLabelledText(label: String, text: String, tab: String? = null) = JLabel(text).also {
        launch(Dispatchers.Swing) {
            getTab(tab).addItem(label, it)
        }
    }

    fun addButton(
        label: String,
        context: CoroutineContext = EmptyCoroutineContext,
        tab: String? = null,
        task: suspend JButton.(ActionEvent) -> Unit
    ) = JButton(label).also { button ->
        button.addActionListener {
            launch(context) { button.task(it) }
        }
        launch(Dispatchers.Swing) {
            getTab(tab).addItem(button)
            pack()
        }
    }

    fun addTextField(label: String, initValue: String, tab: String? = null, onChange: (String) -> Unit = {}) =
        JTextField(initValue).also { textField ->
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
                getTab(tab).addItem(label, textField)
            }
        }

    inline fun <reified T : Any> addFormattedNumericTextField(
        label: String,
        initValue: T,
        columns: Int = 8,
        maximumFractionDigits: Int = 12,
        tab: String? = null,
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
                NumberFormat.getNumberInstance(textField.getLocale())
                    .apply { this.maximumFractionDigits = maximumFractionDigits }
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
            getTab(tab).addItem(label, textField)
        }
    }

    fun addSlider(
        label: String,
        minValue: Double,
        maxValue: Double,
        initValue: Double,
        increment: Double = 0.1,
        tab: String? = null,
        onChange: suspend (Double) -> Unit = {}
    ) = JSlider(
        (minValue / increment).toInt(),
        (maxValue / increment).toInt(),
        (initValue / increment).toInt()
    ).also { slider ->
        slider.paintTicks = true
        slider.paintLabels = true

        // Set specific labels for min, max, and mid values
        val labelTable = Hashtable<Int, JLabel>()
        val minLabelValue = (minValue / increment).toInt()
        val midLabelValue = ((minValue + maxValue) / (2 * increment)).toInt()
        val maxLabelValue = (maxValue / increment).toInt()

        labelTable[minLabelValue] = JLabel(String.format("%.1f", minValue))
        labelTable[midLabelValue] = JLabel(String.format("%.1f", (minValue + maxValue) / 2))
        labelTable[maxLabelValue] = JLabel(String.format("%.1f", maxValue))

        slider.labelTable = labelTable
        slider.snapToTicks = true

        slider.addChangeListener {
            launch {
                onChange(slider.value * increment)
            }
        }

        launch(Dispatchers.Swing) {
            getTab(tab).addItem(label, slider)
            pack()
        }
    }

    /**
     * Adds a slider with a synchronized text field showing the current value.
     * Both the slider and text field can be used to change the value.
     */
    fun addSliderWithTextField(
        label: String,
        minValue: Double,
        maxValue: Double,
        initValue: Double,
        increment: Double = 0.1,
        textFieldColumns: Int = 5,
        tab: String? = null,
        onChange: suspend (Double) -> Unit = {}
    ) {
        val slider = JSlider(
            (minValue / increment).toInt(),
            (maxValue / increment).toInt(),
            (initValue / increment).toInt()
        )
        
        val textField = JFormattedTextField(NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 2
        })
        textField.columns = textFieldColumns
        textField.value = initValue
        
        // Create a panel to hold both slider and text field
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(slider)
        panel.add(Box.createHorizontalStrut(10))
        panel.add(textField)
        
        // Slider updates text field and calls onChange
        slider.addChangeListener {
            val value = slider.value * increment
            textField.value = value
            launch {
                onChange(value)
            }
        }
        
        // Text field updates slider and calls onChange
        textField.addPropertyChangeListener("value") {
            val value = (textField.value as? Number)?.toDouble() ?: initValue
            val clampedValue = value.coerceIn(minValue, maxValue)
            slider.value = (clampedValue / increment).toInt()
            launch {
                onChange(clampedValue)
            }
        }
        
        launch(Dispatchers.Swing) {
            getTab(tab).addItem(label, panel)
            pack()
        }
    }

    fun addCheckBox(label: String, checked: Boolean, tab: String? = null, onChange: suspend (Boolean) -> Unit = {}) =
        JCheckBox().also { checkBox ->
            checkBox.addActionListener {
                launch {
                    onChange(checkBox.isSelected)
                }
            }
            launch(Dispatchers.Swing) {
                checkBox.isSelected = checked
                getTab(tab).addItem(label, checkBox)
                pack()
            }
        }

    fun <O : EditableObject> addAnnotatedPropertyEditor(editor: AnnotatedPropertyEditor<O>, tab: String? = null) {
        launch(Dispatchers.Swing) { getTab(tab).addItem(editor) }
    }

}

suspend fun SimbrainDesktop.loadWorkspaceZipFromFileChooser(): Boolean {
    val simulationChooser = SFileChooser(WorkspacePreferences.simulationDirectory, "Zip Archive", "zip")
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

suspend fun SimbrainDesktop.createClassifierProjectionPlot(smileClassifier: ClassifierNetwork): ProjectionComponent {
    val projectionComponent = ProjectionComponent("${smileClassifier.displayName} Plot").apply {
        projector.coloringManager = AuxDataColoringManager()
    }
    val colors = generateColorSequence().take(smileClassifier.outputNeuronGroup.size).toList()
    with(smileClassifier) {
        classifier.trainingData.inputs.forEach {
            val vector = it.toDoubleArray()
            val colorIndex = classifier.predict(vector).let { prediction ->
                if (prediction < 0) 0 else prediction
            }
            projectionComponent.projector.addDataPoint(DataPoint(vector, aux = colors[colorIndex]))
        }
    }
    val deregisterDeleteEvent = smileClassifier.events.deleted.on {
        workspace.removeWorkspaceComponent(projectionComponent)
    }
    val deregisterUpdateEvent = smileClassifier.events.updated.on {
        val datapoint = DataPoint(
            smileClassifier.inputNeuronGroup.activationArray,
            aux = colors.getOrNull(smileClassifier.winner)
        )
        projectionComponent.projector.addDataPoint(datapoint)
    }
    workspace.addWorkspaceComponent(
        projectionComponent
    )
    (getDesktopComponent(projectionComponent) as ProjectionDesktopComponent).apply {
        projector.dataset.currentPoint = null
        chart.apply {
            xyPlot.fixedLegendItems = LegendItemCollection().apply {
                (colors zip smileClassifier.outputNeuronGroup.labelArray).forEach { (color, label) ->
                    add(LegendItem(label, color))
                }
            }
        }
    }
    workspace.events.componentRemoved.on {
        if (it == projectionComponent) {
            deregisterDeleteEvent()
            deregisterUpdateEvent()
        }
    }
    return projectionComponent
}

fun createDropdownButton(icon: Icon, actions: List<Action>, toolTip: String? = null): JButton {
    val button = JButton().apply {
        this.icon = icon
        text = "▾"
        horizontalTextPosition = SwingConstants.RIGHT
        verticalTextPosition = SwingConstants.CENTER
        if (toolTip != null) {
            toolTipText = toolTip
        }
    }
    
    val popupMenu = JPopupMenu()
    for (action in actions) {
        popupMenu.add(action)
    }
    
    button.addActionListener { e ->
        val btn = e.source as JButton
        popupMenu.show(btn, 0, btn.height)
    }
    
    button.componentPopupMenu = popupMenu
    return button
}

context(SimbrainDesktop)
suspend inline fun <reified T : DesktopComponent<*>> WorkspaceComponent.getDesktopComponentAs(): T {
    return getDesktopComponent(this) as T
}