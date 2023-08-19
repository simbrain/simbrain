package org.simbrain.util.propertyeditor

import org.simbrain.util.Events2
import org.simbrain.util.SimbrainConstants.NULL_STRING
import org.simbrain.util.callNoArgConstructor
import org.simbrain.util.widgets.YesNoNull
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.text.DecimalFormat
import java.text.NumberFormat
import javax.swing.*
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible


/**
 * A special kind of value that can be parsed by an annotated property editor.
 *
 * Example activation = UserParameter<Neuron, Double>(0.0)
 *
 * @param O the type of the base object that holds the parameter
 * @param T the type of the value of this property
 */
class UserParameter2<O: Any, T>(
    initValue: T,
    label: String? = null,
    val min: T? = null,
    val max: T? = null,
    val step: T? = null,
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
        val kMutableProperty1 = property as KMutableProperty1<O, T>
        if (_baseObject == null) {
            _baseObject = baseObject
        }
        if (_property == null) {
            _property = kMutableProperty1
        }
        if (_label == null) {
            _label = property.name
        }
        return value
    }

    @JvmName("getValueNoArgs")
    fun getValue(): T = getValue(baseObject, property)

    operator fun setValue(
        baseObject: O,
        property: KProperty<*>,
        value: T
    ) {
        val kMutableProperty1 = property as KMutableProperty1<O, T>
        if (_baseObject == null) {
            _baseObject = baseObject
        }
        if (_property == null) {
            _property = kMutableProperty1
        }
        if (_label == null) {
            _label = property.name
        }
        this.value = value
    }

}

/**
 * Provides a context for the update function.
 * O and T must match O and T of the parent user parameter.
 */
class UpdateFunctionContext<O: Any, T>(
    private val editor: AnnotatedPropertyEditor2<O>,
    private val parameter: UserParameter2<O, T>,
    val updateEventProperty: KProperty<*>,
    private val enableWidgetProvider: (Boolean) -> Unit,
    private val widgetVisibilityProvider: (Boolean) -> Unit
) {

    /**
     * Provides the value of a widget that can be used inside the update function.
     * Example: `widgetValue(Neuron::activation)` returns the current value of the text field used to edit activation
     * (NOT the actual activation of the model neuron).
     */
    fun <WT> widgetValue(property: KMutableProperty1<O, WT>): WT {
        property.isAccessible = true
        val parameter = property.getDelegate(parameter.baseObject)
        return if (parameter is UserParameter2<*, *>) {
            editor.widgets[parameter]!!.value as WT
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
}

/**
 * Events to fire when the property dialog changes. Changing boolean values, editing text, etc. will fire this event.
 * Allows some dialog entries to respond to others.
 */
class ParameterEvents<O: Any, T>: Events2() {

    val valueChanged = AddedEvent<KMutableProperty1<O, T>>()

}

sealed class ParameterWidget2<O: Any, T>(val parameter: UserParameter2<O, T>, var isConsistent: Boolean) {

    val events = ParameterEvents<O, T>()

    abstract val widget: JComponent

    operator fun getValue(userParameter2: UserParameter2<O, T>, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(userParameter2: UserParameter2<O, T>, property: KProperty<*>, value: T) {
        this.value = value
        events.valueChanged.fireAndBlock(parameter.property)
    }

    abstract var value: T

    abstract fun refresh(property: KProperty<*>)

}

class BooleanWidget<O: Any>(
    val editor: AnnotatedPropertyEditor2<O>,
    parameter: UserParameter2<O, Boolean>,
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

    override var value: Boolean
        get() = widget.isSelected
        set(value) {
            widget.isSelected = value
            isConsistent = true
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

class NumericWidget2<O: Any, T>(
    val editor: AnnotatedPropertyEditor2<O>,
    parameter: UserParameter2<O, T>,
    isConsistent: Boolean
) : ParameterWidget2<O, T>(parameter, isConsistent) {

    val type = parameter.property.returnType.classifier as KClass<*>

    override val widget by lazy {
        val defaultStepSize = when(type) {
            Int::class -> 1
            Short::class -> 1
            Long::class -> 1
            Float::class -> 0.1f
            Double::class -> 0.1
            else -> throw IllegalArgumentException("Unsupported type $type")
        }

        val step = parameter.step ?: defaultStepSize as T

        val model = when(type) {
            Int::class -> SpinnerNumberModel(parameter.value as Int, parameter.min as Int?, parameter.max as Int?, step as Int)
            Short::class -> SpinnerNumberModel(parameter.value as Short, parameter.min as Short?, parameter.max as Short?, step as Short)
            Long::class -> SpinnerNumberModel(parameter.value as Long, parameter.min as Long?, parameter.max as Long?, step as Long)
            Float::class -> SpinnerNumberModel(parameter.value as Float, parameter.min as Float?, parameter.max as Float?, step as Float)
            Double::class -> SpinnerNumberModel(parameter.value as Double, parameter.min as Double?, parameter.max as Double?, step as Double)
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

    override var value: T
        get() = widget.value as T
        set(value) {
            widget.value = value
            this.isConsistent = true
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


class ObjectWidget<O: Any, T: CopyableObject>(
    private val editor: AnnotatedPropertyEditor2<O>,
    private val objectList: List<T>,
    parameter: UserParameter2<O, T>,
    isConsistent: Boolean
) : ParameterWidget2<O, T>(parameter, isConsistent) {

    private var _prototypeObject: T? = null

    override var value: T
        get() = _prototypeObject ?: objectList.first()
        set(value) {
            _prototypeObject = value
        }

    private val typeMap = (value::class.superclasses.asSequence()
        .mapNotNull {
            val property = it.companionObject?.memberProperties?.firstOrNull { prop -> prop.name == "types" } as? KProperty1<Any?, Any?>
            property?.get(it.companionObjectInstance) as? List<KClass<*>>
        }.first()).associateBy { it.simpleName!! }

    private val editorPanelContainer = JPanel()

    lateinit var objectTypeEditor: AnnotatedPropertyEditor2<T>
        private set

    private val dropDown: JComboBox<String> = JComboBox<String>().apply {
        typeMap.keys.forEach {
            addItem(it)
        }
        addActionListener { e: ActionEvent? ->

            // Create the prototype object and refresh editor panel
            try {
                val clazz = typeMap[selectedItem as String]
                if (clazz != null) {
                    val prototypeObject = clazz.callNoArgConstructor() as T
                    objectTypeEditor = AnnotatedPropertyEditor2(listOf(prototypeObject))
                    this@ObjectWidget.value = prototypeObject
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

        topPanel.add(dropDown)

        if (!isConsistent) {
            dropDown.apply {
                addItem(NULL_STRING)
                setSelectedIndex(itemCount - 1)
            }
        } else {
            AnnotatedPropertyEditor2(objectList)
            editorPanelContainer.add(objectTypeEditor)
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
            }
        ))
    }

}