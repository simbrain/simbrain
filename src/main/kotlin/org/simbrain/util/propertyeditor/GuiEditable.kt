package org.simbrain.util.propertyeditor

import org.simbrain.util.*
import org.simbrain.util.SimbrainConstants.NULL_STRING
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import org.simbrain.util.table.createBasicDataFrameFromColumn
import org.simbrain.util.table.createFrom2DArray
import org.simbrain.util.widgets.ChoicesWithNull
import org.simbrain.util.widgets.ColorSelector
import org.simbrain.util.widgets.YesNoNull
import smile.math.matrix.Matrix
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.text.DecimalFormat
import java.text.NumberFormat
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter
import kotlin.math.min
import kotlin.reflect.*
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

/**
 * A special kind of value that can be parsed by an [AnnotatedPropertyEditor].
 *
 * Examples which illustrates how this works can be found in [APETestObjectKotlin].
 *
 * The [onUpdate] function can be used to set the state of the parameter when the property editor changes state. When
 * doing this, the values of other editor components can be queried using [UpdateFunctionContext.widgetValue].
 *
 * Can only be used in Kotlin. In java use the [UserParameter] annotation. [UserParameter] must also be used when custom
 * getter/setter functions are used.
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
    val useLegacySetter: Boolean = false,
    val tab: String? = null,
    val conditionallyEnabledBy: KMutableProperty1<O, Boolean>? = null,
    val conditionallyVisibleBy: KMutableProperty1<O, Boolean>? = null,
    val useCheckboxFrom: KMutableProperty1<O, Boolean>? = null,
    val typeMapProvider: KFunction<List<Class<out CopyableObject>>>? = null,
    val columnMode: Boolean = false,
    val showLabeledBorder: Boolean = true,
    val getter: (GuiEditableGetterContext<O, T>.() -> T)? = null,
    val setter: (GuiEditableSetterContext<O, T>.(T) -> Unit)? = null,
    private val onUpdate: (UpdateFunctionContext<O, T>).() -> Unit = { }
) {

    var value: T = initValue

    private var _baseObject: O? = null
    val baseObject: O
        get() {
            if (_baseObject == null) throw IllegalStateException("Base object not initialized")
            return _baseObject!!
        }

    @Transient
    private var _property: KProperty1<O, T>? = null
    val property: KProperty1<O, T>
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
        return getter?.invoke(GuiEditableGetterContext()) ?: value
    }

    @JvmName("getValueNoArgs")
    fun getValue(): T = getValue(baseObject, property)

    @JvmName("setValueNoArgs")
    fun setValue(value: T) = setValue(baseObject, property, value)

    operator fun setValue(
        baseObject: O,
        property: KProperty<*>,
        value: T
    ) {
        initInternalValues(baseObject, property)
        if (setter != null) {
            setter.invoke(GuiEditableSetterContext(), value)
        } else {
            this.value = value
        }
    }

    private fun initInternalValues(
        baseObject: O,
        property: KProperty<*>,
    ) {
        if (_baseObject == null) {
            _baseObject = baseObject
        }
        if (_property == null) {
            if (!displayOnly && property !is KMutableProperty<*>) {
                throw IllegalArgumentException("Property $property is not mutable")
            }
            _property = property as KProperty1<O, T>
        }
        if (_label == null) {
            _label = property.name.convertCamelCaseToSpaces()
        }
    }

    fun update(context: UpdateFunctionContext<O, T>) {
        with(context) {
            if (conditionallyEnabledBy?.isWidget() == false) {
                enableWidget(conditionallyEnabledBy.getter.call(baseObject))
            }
            if (conditionallyVisibleBy?.isWidget() == false) {
                showWidget(conditionallyVisibleBy.getter.call(baseObject))
            }
            if (updateEventProperty == conditionallyEnabledBy) {
                enableWidget(widgetValue(conditionallyEnabledBy))
            }
            if (updateEventProperty == conditionallyVisibleBy) {
                showWidget(widgetValue(conditionallyVisibleBy))
            }
        }
        if (context.monitoringPropertyNames == null || context.updateEventProperty.name in context.monitoringPropertyNames!!) {
            onUpdate(context)
        }
    }

}

context(GuiEditable<O, T>)
class GuiEditableGetterContext<O : EditableObject, T> {
    val field get() = value
}

context(GuiEditable<O, T>)
class GuiEditableSetterContext<O : EditableObject, T> {
    var field get() = value
        set(newValue) {
            value = newValue
        }
}

/**
 * Converts [UserParameter] annotation to [GuiEditable].
 */
fun <O : EditableObject> UserParameter.toGuiEditable(obj: O, property: KProperty1<out Any, *>): GuiEditable<O, Any?> {

    fun Any?.toParameterDataType(): Any? {
        val thisNumber = this as Double
        if (thisNumber.isNaN()) return null
        val type = property.returnType.classifier as KClass<*>
        return when (type) {
            Int::class -> (thisNumber).toInt()
            Long::class -> (thisNumber).toLong()
            Float::class -> (thisNumber).toFloat()
            Double::class -> this
            else -> this
        }
    }

    fun loadInitValue(): Any? {
        return try {
            property.getter.call(obj)
        } catch (e: java.lang.IllegalStateException) {
            throw IllegalStateException(
                "Could not access property ${property.javaField?.declaringClass?.kotlin?.simpleName}::${property.name}",
                e
            )
        } catch (e: Exception) {
            throw IllegalStateException(
                "Something went wrong when accessing ${property.javaField?.declaringClass?.kotlin?.simpleName}::${property.name}: [${e::class.simpleName}] ${e.message}",
                e
            )
        }
    }

    return GuiEditable(
        initValue = loadInitValue(),
        label = label,
        description = description.ifEmpty { null },
        min = minimumValue.toParameterDataType(),
        max = maximumValue.toParameterDataType(),
        increment = increment.toParameterDataType(),
        order = order,
        displayOnly = displayOnly,
        showDetails = showDetails,
        useLegacySetter = useLegacySetter,
        columnMode = columnMode,
        tab = tab,
        typeMapProvider = if (typeMapProvider.isNotEmpty()) {
            property.returnType
                .jvmErasure.functions
                .first { it.name == typeMapProvider } as KFunction<List<Class<out CopyableObject>>>
        } else {
           null
       },
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
     * For any property names in this set, if any of them changed, the update function will be called.
     */
    var monitoringPropertyNames: HashSet<String>? = null

    fun initializeOrGetMonitoringPropertyNames(): HashSet<String> {
        if (monitoringPropertyNames == null) {
            monitoringPropertyNames = HashSet()
        }
        return monitoringPropertyNames!!
    }

    /**
     * Provides the value of a widget that can be used inside the update function.
     * Example: `widgetValue(Neuron::activation)` returns the current value of the text field used to edit activation
     * (NOT the actual activation of the model neuron).
     *
     * Note: Since widgetValue is also used to register [monitoringPropertyNames], it must not be hidden behind a conditional
     */
    fun <WT> widgetValue(property: KMutableProperty1<O, WT>): WT {
        initializeOrGetMonitoringPropertyNames().add(property.name)
        return property.withTempPublicAccess {
            editor.propertyNameWidgetMap[property.name]?.value as? WT
                ?: throw IllegalArgumentException("Property $property is not a user parameter")
        }
    }

    /**
     * See [widgetValue].
     * Provides a simpler way to access to property.
     * Example: `widgetValue(::activation)` in Neuron
     */
    fun <WT> widgetValue(property: KMutableProperty0<WT>): WT {
        initializeOrGetMonitoringPropertyNames().add(property.name)
        return property.withTempPublicAccess {
            editor.propertyNameWidgetMap[property.name]?.value as? WT
                ?: throw IllegalArgumentException("Property $property is not a user parameter")
        }
    }

    fun KProperty<*>.isWidget(): Boolean {
        return editor.propertyNameWidgetMap.containsKey(name)
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
class ParameterEvents<O : EditableObject, T> : Events() {

    val valueChanged = OneArgEvent<KProperty1<O, T>>()

}

sealed class ParameterWidget<O : EditableObject, T>(val parameter: GuiEditable<O, T>, var isConsistent: Boolean) {

    val events = ParameterEvents<O, T>()

    abstract val widget: JComponent

    val component by lazy {
        if (parameter.useCheckboxFrom != null) {
            JPanel().also {
                it.layout = BoxLayout(it, BoxLayout.X_AXIS)
                it.add(enablingCheckbox)
                it.add(widget)
            }
        } else {
            widget
        }
    }

    abstract val value: T

    abstract fun refresh(property: KProperty<*>)

    private val enablingCheckbox by lazy {
        if (parameter.useCheckboxFrom != null) {
            JCheckBox().also {
                it.isSelected = parameter.useCheckboxFrom.get(parameter.baseObject)
                widget.isEnabled = it.isSelected
                it.addActionListener { _ ->
                    parameter.useCheckboxFrom.set(parameter.baseObject, it.isSelected)
                    widget.isEnabled = it.isSelected
                }
            }
        } else {
            null
        }
    }

}

class EnumWidget<O : EditableObject, T : Enum<*>>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, T>,
    isConsistent: Boolean
) : ParameterWidget<O, T>(parameter, isConsistent) {

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
            it.addActionListener { _ ->
                events.valueChanged.fire(parameter.property)
                this@EnumWidget.isConsistent = true
                it.removeNull()
            }
        }
    }

    override var value: T
        get() = widget.selectedItem as T
        set(value) {
            // Not used
        }

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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
) : ParameterWidget<O, Boolean>(parameter, isConsistent) {

    override val widget by lazy {
        YesNoNull().also {
            it.isSelected = parameter.value
            if (!isConsistent) {
                it.setNull()
            }
        }.also {
            it.addActionListener { _ ->
                events.valueChanged.fire(parameter.property)
                this@BooleanWidget.isConsistent = true
                it.removeNull()
            }
        }
    }

    override val value: Boolean
        get() = widget.isSelected

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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

class NumericWidget<O : EditableObject, T>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, T>,
    isConsistent: Boolean
) : ParameterWidget<O, T>(parameter, isConsistent) {

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
            Int::class -> CustomSpinnerNumberModel(
                parameter.value as Int,
                parameter.min as Int?,
                parameter.max as Int?,
                step as Int
            )

            Short::class -> CustomSpinnerNumberModel(
                parameter.value as Short,
                parameter.min as Short?,
                parameter.max as Short?,
                step as Short
            )

            Long::class -> CustomSpinnerNumberModel(
                parameter.value as Long,
                parameter.min as Long?,
                parameter.max as Long?,
                step as Long
            )

            Float::class -> CustomSpinnerNumberModel(
                parameter.value as Float,
                parameter.min as Float?,
                parameter.max as Float?,
                step as Float
            )

            Double::class -> CustomSpinnerNumberModel(
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
            ftf.setFocusLostBehavior(JFormattedTextField.PERSIST)
            ftf.columns = 10
            ftf.isEditable = true
            ftf.setFormatterFactory(factory)
            if (!isConsistent) {
                (it.editor as JSpinner.DefaultEditor).textField?.text = NULL_STRING
            }
            ftf.addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    if (ftf.isValid) {
                        ftf.commitEdit()
                    }
                }
            })
        }.also {
            it.addChangeListener {
                events.valueChanged.fire(parameter.property)
                this@NumericWidget.isConsistent = true
            }
        }
    }

    override val value: T
        get() = widget.value as T

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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

    class CustomSpinnerNumberModel<T>(number: T, minimum: T?, maximum: T?, stepSize: T) : SpinnerNumberModel(number, minimum, maximum, stepSize) where T : Number, T : Comparable<T>{
        override fun getNextValue(): T {
            return incrValue(1)
        }

        override fun getPreviousValue(): T {
            return incrValue(-1)
        }

        private fun incrValue(dir: Int): T {
            val newValue = (value as Number).toDouble() + dir * stepSize.toDouble()

            maximum?.let {
                if (newValue > (it as Number).toDouble()) {
                    @Suppress("UNCHECKED_CAST")
                    return it as T
                }
            }

            minimum?.let {
                if (newValue < (it as Number).toDouble()) {
                    @Suppress("UNCHECKED_CAST")
                    return it as T
                }
            }

            @Suppress("UNCHECKED_CAST")
            return newValue as T
        }
    }

}

class DisplayOnlyWidget<O : EditableObject, T>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, T>,
    isConsistent: Boolean
) : ParameterWidget<O, T>(parameter, isConsistent) {

    override val widget by lazy {
        JLabel().also {
            it.text = if (this.isConsistent) parameter.value.toString() else NULL_STRING
        }
    }

    override val value: T
        get() = parameter.value

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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
    parameter: GuiEditable<O, String?>,
    isConsistent: Boolean
) : ParameterWidget<O, String?>(parameter, isConsistent) {

    private var changed = false

    override val widget by lazy {
        JTextField().also {
            it.text = if (this@StringWidget.isConsistent) parameter.value else NULL_STRING
        }.also {
            it.document.addDocumentListener(object : DocumentListener {
                fun update() {
                    events.valueChanged.fire(parameter.property)
                    this@StringWidget.isConsistent = true
                    changed = true
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

    override val value: String?
        get() = if (changed) widget.text else parameter.value

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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
) : ParameterWidget<O, Color>(parameter, isConsistent) {

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
        parameter.update(UpdateFunctionContext(
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
) : ParameterWidget<O, DoubleArray>(parameter, isConsistent) {

    private var model = if (parameter.columnMode) {
        createBasicDataFrameFromColumn(parameter.value)
    } else {
        createFrom2DArray(arrayOf(parameter.value.toTypedArray()))
    }

    override val widget by lazy {
        JPanel().apply {
            layout = BorderLayout()
            SimbrainTablePanel(
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
        get() = if (parameter.columnMode) {
            model.getDoubleColumn(0)
        } else {
            model.getRow<Double>(0).toDoubleArray()
        }

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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

class IntArrayWidget<O : EditableObject>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, IntArray>,
    isConsistent: Boolean
) : ParameterWidget<O, IntArray>(parameter, isConsistent) {

    private var model = if (parameter.columnMode) {
        createBasicDataFrameFromColumn(parameter.value)
    } else {
        createFrom2DArray(arrayOf(parameter.value.toTypedArray()))
    }

    override val widget by lazy {
        JPanel().apply {
            layout = BorderLayout()
            SimbrainTablePanel(
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

    override val value: IntArray
        get() = if (parameter.columnMode) {
            model.getIntColumn(0)
        } else {
            model.getRow<Int>(0).toIntArray()
        }

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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

class BooleanArrayWidget<O : EditableObject>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, BooleanArray>,
    isConsistent: Boolean
) : ParameterWidget<O, BooleanArray>(parameter, isConsistent) {

    private var model = if (parameter.columnMode) {
        createBasicDataFrameFromColumn(parameter.value.map { if (it) 1 else 0 }.toIntArray())
    } else {
        createFrom2DArray(arrayOf(parameter.value.map { if (it) 1 else 0 }.toTypedArray()))
    }

    override val widget by lazy {
        JPanel().apply {
            layout = BorderLayout()
            SimbrainTablePanel(
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

    override val value: BooleanArray
        get() = if (parameter.columnMode) {
            model.getBooleanColumn(0)
        } else {
            model.getRow<Boolean>(0).toBooleanArray()
        }

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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

class MatrixWidget<O : EditableObject>(
    val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, Matrix>,
    isConsistent: Boolean
) : ParameterWidget<O, Matrix>(parameter, isConsistent) {

    private var model = MatrixDataFrame(
        if (parameter.columnMode) {
            parameter.value
        } else {
            parameter.value.transpose()
        }
    )

    override val widget by lazy {
        JPanel().apply {
            layout = BorderLayout()
            SimbrainTablePanel(
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

    override val value: Matrix
        get() = if (parameter.columnMode) {
            model.data
        } else {
            model.data.transpose()
        }

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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
) : ParameterWidget<O, T>(parameter, isConsistent) {

    private var _prototypeObject: T? = null

    override val value: T
        get() = _prototypeObject ?: objectList.first()

    /**
     * Finds first superclass with a getTypes method, and if one is found a dropdown is provided that allows the
     * object’s type to be edited. Otherwise, simply embed the object with its own APE.
     */
    private val typeMap = (if (parameter.typeMapProvider != null) {
        parameter.typeMapProvider.call(value).map { it.kotlin }
    } else {
        value.getTypeList()?.map { it.kotlin }
    })?.associateBy { it.displayName }

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
            selectedItem = value::class.displayName
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
                        events.valueChanged.fire(parameter.property)
                        SwingUtilities.getWindowAncestor(this)?.pack()
                    }
                } catch (exception: Exception) {
                    println("Failed to create prototype object for $parameter with type $selectedItem and class ${typeMap[selectedItem as String]}")
                    exception.printStackTrace()
                }
            }
        }
    }

    override val widget = JPanel(BorderLayout()).apply {
        if (parameter.showLabeledBorder) {
            border = BorderFactory.createTitledBorder(parameter.label)
        }
        val detailTrianglePanel = DetailTrianglePanel(
            editorPanelContainer,
            defaultOpen = parameter.showDetails,
            topPanelComponent = dropDown
        )
        add(detailTrianglePanel, BorderLayout.CENTER)
        if (!isConsistent) {
            dropDown?.apply {
                addItem(NULL_STRING)
                setSelectedIndex(itemCount - 1)
            }
        } else {
            objectTypeEditor = AnnotatedPropertyEditor(objectList)
            editorPanelContainer.add(objectTypeEditor)
            if (dropDown == null && objectTypeEditor.parameterWidgetMap.isEmpty()) {
                isVisible = false
            }
        }
    }

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(
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

@Target(AnnotationTarget.CLASS)
annotation class APETabOder(vararg val tabs: String)