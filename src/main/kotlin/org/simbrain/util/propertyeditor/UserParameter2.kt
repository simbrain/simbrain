package org.simbrain.util.propertyeditor

import org.simbrain.util.Events2
import org.simbrain.util.widgets.YesNoNull
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
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
    val onUpdate: UpdateFunctionContext<O, T>.() -> Unit = { }
) {

    var value: T = initValue
    var widgetValue: T by makeWidget()

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
class UpdateFunctionContext<O, T>(
    private val baseObject: O,
    val updateEventProperty: KProperty<*>,
    private val enableWidgetProvider: (Boolean) -> Unit
) {

    /**
     * Provides the value of a widget that can be used inside the update function.
     * Example: `widgetValue(Neuron::activation)` returns the current value of the text field used to edit activation
     * (NOT the actual activation of the model neuron).
     */
    fun widgetValue(property: KMutableProperty1<O, T>): T {
        property.isAccessible = true
        val delegate = property.getDelegate(baseObject)
        return if (delegate is UserParameter2<*, *>) {
            delegate.widgetValue as T
        } else {
            property.get(baseObject)
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
            it.addActionListener {
                events.valueChanged.fireAndBlock(parameter.property)
            }
        }
    }

    override var value: Boolean
        get() = widget.isSelected
        set(value) {
            widget.isSelected = value
        }

    override fun refresh(property: KProperty<*>) {
        parameter.onUpdate(UpdateFunctionContext(
            parameter.baseObject,
            property,
            enableWidgetProvider = { enabled ->
                widget.isEnabled = enabled
            }
        ))
    }
}

fun <O: Any, T, > UserParameter2<O, T>.makeWidget(isConsistent: Boolean = true): ParameterWidget2<O, T> {
    return when (value) {
        is Boolean? -> BooleanWidget(this as UserParameter2<O, Boolean>, isConsistent) as ParameterWidget2<O, T>
        is Int? -> TODO()
        else -> throw IllegalArgumentException("Unsupported type: ${value!!::class.simpleName}")
    }
}
