package org.simbrain.util.propertyeditor

import org.simbrain.util.*
import org.simbrain.util.SimbrainConstants.NULL_STRING
import org.simbrain.util.table.MatrixDataWrapper
import org.simbrain.util.table.SimbrainDataViewer
import org.simbrain.util.widgets.ChoicesWithNull
import org.simbrain.util.widgets.ColorSelector
import org.simbrain.util.widgets.DropDownTriangle
import org.simbrain.util.widgets.YesNoNull
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.DecimalFormat
import java.text.NumberFormat
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

/**
 * A special kind of value that can be parsed by an [AnnotatedPropertyEditor].
 *
 * Example activation = UserParameter<Neuron, Double>(0.0)
 *
 * @param O the type of the base object that holds the parameter
 * @param T the type of the value of this property
 */
class GuiEditable<O : EditableObject, T>(
    initValue: T,
    label: String? = null,
    val description: String? = null,
    val min: T? = null,
    val max: T? = null,
    val increment: T? = null,
    val order: Int = 0,
    val displayOnly: Boolean = false,
    val showDetails: Boolean = true,
    val tab: String? = null,
    val onUpdate: UpdateFunctionContext<O, T>.() -> Unit = { }
) {

    var value: T = initValue

    private var _baseObject: O? = null
    val baseObject: O
        get() {
            if (_baseObject == null) throw IllegalStateException("Base object not initialized")
            return _baseObject!!
        }

    private var _property: KMutableProperty1<O, T>? = null
    val property: KMutableProperty1<O, T>
        get() {
            if (_property == null) throw IllegalStateException("Property not initialized")
            return _property!!
        }

    private var _label: String? = label
    val label: String
        get() {
            if (_label == null) throw IllegalStateException("Label not initialized")
            return _label!!
        }

    operator fun getValue(baseObject: O, property: KProperty<*>): T {
        initInternalValues(baseObject, property)
        return value
    }

    @JvmName("getValueNoArgs")
    fun getValue(): T = getValue(baseObject, property)

    operator fun setValue(
        baseObject: O,
        property: KProperty<*>,
        value: T
    ) {
        initInternalValues(baseObject, property)
        this.value = value
    }

    private fun initInternalValues(
        baseObject: O,
        property: KProperty<*>,
    ) {
        if (_baseObject == null) {
            _baseObject = baseObject
        }
        if (_property == null) {
            val kMutableProperty1 = property as KMutableProperty1<O, T>
            _property = kMutableProperty1
        }
        if (_label == null) {
            _label = property.name.convertCamelCaseToSpaces()
        }
    }

}

/**
 * Converts [UserParameter] annotation to [GuiEditable].
 */
fun <O : EditableObject> UserParameter.toGuiEditable(initValue: Any): GuiEditable<O, Any> {

    fun Any.matchDataTypeTo(match: Any): Any? {
        val thisNumber = this as Double
        if (thisNumber.isNaN()) return null
        return when (match) {
            is Int -> thisNumber.toInt()
            is Short -> thisNumber.toInt().toShort()
            is Long -> this.toLong()
            is Float -> this.toFloat()
            else -> this
        }
    }

    return GuiEditable(
        initValue = initValue,
        label = label,
        description = description.ifEmpty { null },
        min = minimumValue.matchDataTypeTo(initValue),
        max = maximumValue.matchDataTypeTo(initValue),
        increment = increment.matchDataTypeTo(initValue),
        order = order,
        displayOnly = displayOnly,
        showDetails = showDetails,
        tab = tab,
        onUpdate = {
        }
    )
}

/**
 * Provides a context for the update function.
 * O and T must match O and T of the parent user parameter.
 */
class UpdateFunctionContext<O : EditableObject, T>(
    private val editor: AnnotatedPropertyEditor<O>,
    private val parameter: GuiEditable<O, T>,
    val updateEventProperty: KProperty<*>,
    private val enableWidgetProvider: (Boolean) -> Unit,
    private val widgetVisibilityProvider: (Boolean) -> Unit,
    private val refreshSourceProvider: (T) -> Unit = {},
) {

    /**
     * Provides the value of a widget that can be used inside the update function.
     * Example: `widgetValue(Neuron::activation)` returns the current value of the text field used to edit activation
     * (NOT the actual activation of the model neuron).
     */
    fun <WT> widgetValue(property: KMutableProperty1<O, WT>): WT {
        property.isAccessible = true
        val parameter = property.getDelegate(parameter.baseObject)
        return if (parameter is GuiEditable<*, *>) {
            editor.parameterWidgetMap[parameter]!!.value as WT
        } else {
            throw IllegalArgumentException("Property $property is not a user parameter")
        }
    }

    fun onChange(property: KProperty<*>, block: () -> Unit) {
        if (this.updateEventProperty == property) {
            block()
        }
    }

    fun enableWidget(enabled: Boolean) {
        enableWidgetProvider(enabled)
    }

    fun showWidget(visible: Boolean) {
        widgetVisibilityProvider(visible)
        editor.parameterJLabels[parameter]?.isVisible = visible
    }

    fun refreshValue(newValue: T) {
        refreshSourceProvider(newValue)
    }
}

/**
 * Events to fire when the property dialog changes. Changing boolean values, editing text, etc. will fire this event.
 * Allows some dialog entries to respond to others.
 */
class ParameterEvents<O : EditableObject, T> : Events2() {

    val valueChanged = AddedEvent<KMutableProperty1<O, T>>()

}

sealed class ParameterWidget2<O : EditableObject, T>(val parameter: GuiEditable<O, T>, var isConsistent: Boolean) {

    val events = ParameterEvents<O, T>()

    abstract val widget: JComponent

    abstract val value: T

    abstract fun refresh(property: KProperty<*>)

}

class EnumWidget<O : EditableObject, T : Enum<*>>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, T>,
    isConsistent: Boolean
) : ParameterWidget2<O, T>(parameter, isConsistent) {

    override val widget by lazy {

        val clazz = parameter.property.returnType
        val method = clazz.jvmErasure.functions.find { it.name == "values" }
        val enumValues = method!!.call()
        ChoicesWithNull(enumValues as Array<T>).also {
            if (!isConsistent) {
                it.setNull()
            } else {
                it.selectedItem = parameter.value
            }
        }
    }

    override var value: T
        get() = widget.selectedItem as T
        set(value) {
            // Not used
        }

    override fun refresh(property: KProperty<*>) {
        parameter.onUpdate(UpdateFunctionContext(
            editor,
            parameter,
            property,
            enableWidgetProvider = { enabled ->
                widget.isEnabled = enabled
            },
            widgetVisibilityProvider = { visible ->
                widget.isVisible = visible
            }
        ))
    }
}

class BooleanWidget<O : EditableObject>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, Boolean>,
    isConsistent: Boolean
) : ParameterWidget2<O, Boolean>(parameter, isConsistent) {

    override val widget by lazy {
        YesNoNull().also {
            it.isSelected = parameter.value
            if (!isConsistent) {
                it.setNull()
            }
        }.also {
            it.addActionListener { _ ->
                events.valueChanged.fireAndBlock(parameter.property)
                this@BooleanWidget.isConsistent = true
                it.removeNull()
            }
        }
    }

    override val value: Boolean
        get() = widget.isSelected

    override fun refresh(property: KProperty<*>) {
        parameter.onUpdate(UpdateFunctionContext(
            editor,
            parameter,
            property,
            enableWidgetProvider = { enabled ->
                widget.isEnabled = enabled
            },
            widgetVisibilityProvider = { visible ->
                widget.isVisible = visible
            }
        ))
    }
}

class NumericWidget2<O : EditableObject, T>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, T>,
    isConsistent: Boolean
) : ParameterWidget2<O, T>(parameter, isConsistent) {

    val type = parameter.property.returnType.classifier as KClass<*>

    override val widget by lazy {
        val defaultStepSize = when (type) {
            Int::class -> 1
            Short::class -> 1
            Long::class -> 1
            Float::class -> 0.1f
            Double::class -> 0.1
            else -> throw IllegalArgumentException("Unsupported type $type")
        }

        val step = parameter.increment ?: defaultStepSize as T

        val model = when (type) {
            Int::class -> SpinnerNumberModel(
                parameter.value as Int,
                parameter.min as Int?,
                parameter.max as Int?,
                step as Int
            )

            Short::class -> SpinnerNumberModel(
                parameter.value as Short,
                parameter.min as Short?,
                parameter.max as Short?,
                step as Short
            )

            Long::class -> SpinnerNumberModel(
                parameter.value as Long,
                parameter.min as Long?,
                parameter.max as Long?,
                step as Long
            )

            Float::class -> SpinnerNumberModel(
                parameter.value as Float,
                parameter.min as Float?,
                parameter.max as Float?,
                step as Float
            )

            Double::class -> SpinnerNumberModel(
                parameter.value as Double,
                parameter.min as Double?,
                parameter.max as Double?,
                step as Double
            )

            else -> throw IllegalArgumentException("Unsupported type $type")
        }
        JSpinner(model).also {
            val format = if (type == Int::class || type == Long::class || type == Short::class) {
                NumberFormat.getIntegerInstance(it.getLocale())
            } else {
                NumberFormat.getNumberInstance(it.getLocale()).apply { maximumFractionDigits = 12 }
            } as DecimalFormat
            val formatterEditor = NumberFormatter(format)
            formatterEditor.valueClass = type.javaObjectType
            val factory = DefaultFormatterFactory(formatterEditor)
            val ftf: JFormattedTextField = (it.editor as JSpinner.DefaultEditor).textField
            ftf.columns = 10
            ftf.isEditable = true
            ftf.setFormatterFactory(factory)
            if (!isConsistent) {
                (it.editor as JSpinner.DefaultEditor).textField?.text = NULL_STRING
            }
        }.also {
            it.addChangeListener {
                events.valueChanged.fireAndBlock(parameter.property)
                this@NumericWidget2.isConsistent = true
            }
        }
    }

    override val value: T
        get() = widget.value as T

    override fun refresh(property: KProperty<*>) {
        parameter.onUpdate(UpdateFunctionContext(
            editor,
            parameter,
            property,
            enableWidgetProvider = { enabled ->
                widget.isEnabled = enabled
            },
            widgetVisibilityProvider = { visible ->
                widget.isVisible = visible
            }
        ))
    }

}

class DisplayOnlyWidget<O : EditableObject, T>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, T>,
    isConsistent: Boolean
) : ParameterWidget2<O, T>(parameter, isConsistent) {

    override val widget by lazy {
        JLabel().also {
            it.text = if (this.isConsistent) parameter.value.toString() else NULL_STRING
        }
    }

    override val value: T
        get() = parameter.value

    override fun refresh(property: KProperty<*>) {
        parameter.onUpdate(UpdateFunctionContext(
            editor,
            parameter,
            property,
            enableWidgetProvider = {},
            widgetVisibilityProvider = { visible ->
                widget.isVisible = visible
            }
        ))
    }
}

class StringWidget<O : EditableObject>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, String>,
    isConsistent: Boolean
) : ParameterWidget2<O, String>(parameter, isConsistent) {

    override val widget by lazy {
        JTextField().also {
            it.text = if (this@StringWidget.isConsistent) parameter.value else NULL_STRING
        }.also {
            it.document.addDocumentListener(object : DocumentListener {
                fun update() {
                    events.valueChanged.fireAndBlock(parameter.property)
                    this@StringWidget.isConsistent = true
                }

                override fun insertUpdate(e: DocumentEvent) {
                    update()
                }

                override fun removeUpdate(e: DocumentEvent?) {
                    update()
                }

                override fun changedUpdate(e: DocumentEvent?) {
                    update()
                }
            })
        }
    }

    override val value: String
        get() = widget.text

    override fun refresh(property: KProperty<*>) {
        parameter.onUpdate(UpdateFunctionContext(
            editor,
            parameter,
            property,
            enableWidgetProvider = { enabled ->
                widget.isEnabled = enabled
            },
            widgetVisibilityProvider = { visible ->
                widget.isVisible = visible
            }
        ))
    }
}

class ColorWidget<O : EditableObject>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, Color>,
    isConsistent: Boolean
) : ParameterWidget2<O, Color>(parameter, isConsistent) {

    override val widget by lazy {
        ColorSelector().also {
            if (isConsistent) {
                it.value = parameter.value
            }
        }
    }

    override val value: Color
        get() = widget.value

    override fun refresh(property: KProperty<*>) {
        parameter.onUpdate(UpdateFunctionContext(
            editor,
            parameter,
            property,
            enableWidgetProvider = { enabled ->
                widget.isEnabled = enabled
            },
            widgetVisibilityProvider = { visible ->
                widget.isVisible = visible
            }
        ))
    }
}


class DoubleArrayWidget<O : EditableObject>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, DoubleArray>,
    isConsistent: Boolean
) : ParameterWidget2<O, DoubleArray>(parameter, isConsistent) {

    private var model = MatrixDataWrapper(parameter.value.toMatrix().transpose())

    override val widget by lazy {
        JPanel().apply {
            layout = BorderLayout()
            SimbrainDataViewer(
                model, useDefaultToolbarAndMenu = false, useHeaders = false,
                usePadding = false
            ).also {
                it.table.tableHeader = null
                add(it)
                minimumSize = Dimension(200, min((model.rowCount + 1) * 17 + 2, 100))
                preferredSize = Dimension(200, min((model.rowCount + 1) * 17 + 2, 100))
            }
        }
    }

    override val value: DoubleArray
        get() = model.get2DDoubleArray().first()

    override fun refresh(property: KProperty<*>) {
        parameter.onUpdate(UpdateFunctionContext(
            editor,
            parameter,
            property,
            enableWidgetProvider = { enabled ->
                widget.isEnabled = enabled
            },
            widgetVisibilityProvider = { visible ->
                widget.isVisible = visible
            }
        ))
    }
}

class ObjectWidget<O : EditableObject, T : CopyableObject>(
    private val editor: AnnotatedPropertyEditor<O>,
    private val objectList: List<T>,
    parameter: GuiEditable<O, T>,
    isConsistent: Boolean
) : ParameterWidget2<O, T>(parameter, isConsistent) {

    private var _prototypeObject: T? = null

    private var detailTriangle: DropDownTriangle? = null

    override val value: T
        get() = _prototypeObject ?: objectList.first()

    /**
     * Finds first superclass with a getTypes method, and if one is found a dropdown is provided that allows the
     * object’s type to be edited. Otherwise, simply embed the object with its own APE.
     */
    private val typeMap = (value::class.superclasses.asSequence()
        .mapNotNull {
            val fromJava =
                (it.staticFunctions.firstOrNull { it.name == "getTypes" }?.call() as? List<Class<*>>)?.map { it.kotlin }
            if (fromJava != null) {
                fromJava
            } else {
                val property =
                    it.companionObject?.memberProperties?.firstOrNull { prop -> prop.name == "types" } as? KProperty1<Any?, Any?>
                property?.get(it.companionObjectInstance) as? List<KClass<*>>
            }
        }.firstOrNull()
    )?.associateBy { it.simpleName!! }

    private val editorPanelContainer = JPanel()

    lateinit var objectTypeEditor: AnnotatedPropertyEditor<T>
        private set

    /**
     * if null, then the object type is not editable
     */
    private val dropDown: JComboBox<String>? = typeMap?.run {
        JComboBox<String>().apply {
            typeMap.keys.forEach {
                addItem(it)
            }
            selectedItem = value::class.simpleName
            addActionListener { e: ActionEvent? ->

                // Create the prototype object and refresh editor panel
                try {
                    val clazz = typeMap[selectedItem as String]
                    if (clazz != null) {
                        val prototypeObject = clazz.callNoArgConstructor() as T
                        objectTypeEditor = AnnotatedPropertyEditor(listOf(prototypeObject))
                        _prototypeObject = prototypeObject
                        editorPanelContainer.removeAll()
                        editorPanelContainer.add(objectTypeEditor)
                        this@ObjectWidget.isConsistent = true
                        removeItem(NULL_STRING)
                        revalidate()
                        events.valueChanged.fireAndBlock(parameter.property)
                        SwingUtilities.getWindowAncestor(this)?.pack()
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }
        }
    }

    override val widget = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        val padding = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        // Top Panel contains the combo box and detail triangle
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.X_AXIS)
        val tb = BorderFactory.createTitledBorder(parameter.label)
        border = tb
        topPanel.alignmentX = JPanel.CENTER_ALIGNMENT
        topPanel.border = padding
        this.add(Box.createRigidArea(Dimension(0, 5)))
        this.add(topPanel)

        dropDown?.let { topPanel.add(it) }

        if (!isConsistent) {
            dropDown?.apply {
                addItem(NULL_STRING)
                setSelectedIndex(itemCount - 1)
            }
        } else {
            val window = SwingUtilities.getWindowAncestor(this)
            if (dropDown != null) {
                // Set up detail triangle
                detailTriangle =
                    DropDownTriangle(
                        DropDownTriangle.UpDirection.LEFT, parameter.showDetails, "Settings", "Settings",
                        window
                    )
                detailTriangle!!.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(arg0: MouseEvent) {
                        editorPanelContainer.isVisible = detailTriangle!!.isDown
                        repaint()
                        window?.pack()
                    }
                })
                editorPanelContainer.isVisible = detailTriangle!!.isDown
                topPanel.add(Box.createHorizontalStrut(30))
                topPanel.add(Box.createHorizontalGlue())
                topPanel.add(detailTriangle)
            }
            objectTypeEditor = AnnotatedPropertyEditor(objectList)
            editorPanelContainer.add(objectTypeEditor)
            if (dropDown == null && objectTypeEditor.parameterWidgetMap.isEmpty()) {
                isVisible = false
            }
        }

        add(editorPanelContainer)
    }

    override fun refresh(property: KProperty<*>) {
        parameter.onUpdate(UpdateFunctionContext(
            editor,
            parameter,
            property,
            enableWidgetProvider = { enabled ->
                widget.isEnabled = enabled
            },
            widgetVisibilityProvider = { visible ->
                widget.isVisible = visible
            },
            refreshSourceProvider = { newValue ->
                _prototypeObject = newValue
                objectTypeEditor = AnnotatedPropertyEditor(listOf(newValue))
                editorPanelContainer.removeAll()
                editorPanelContainer.add(objectTypeEditor)
                if (objectTypeEditor.parameterWidgetMap.isEmpty()) {
                    widget.isVisible = false
                } else {
                    widget.isVisible = true
                }
                SwingUtilities.getWindowAncestor(editorPanelContainer)?.pack()
            }
        ))
    }

}