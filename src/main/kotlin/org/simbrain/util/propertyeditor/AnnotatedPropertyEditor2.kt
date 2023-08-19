package org.simbrain.util.propertyeditor

import org.simbrain.util.LabelledItemPanel
import javax.swing.JPanel
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class AnnotatedPropertyEditor2<O : Any>(val editingObjects: List<O>) : JPanel() {

    val widgets = editingObjects.first().let { obj ->

        obj::class.memberProperties
            .asSequence()
            .filterIsInstance<KMutableProperty1<O, *>>()
            .onEach { it.isAccessible = true }
            .mapNotNull { property -> property.getDelegate(obj)?.also { property.get(obj) } }
            .filterIsInstance<UserParameter2<O, *>>()
            .map { parameter ->
                parameter to makeWidget(
                    parameter,
                    if (parameter.value is CopyableObject) {
                        editingObjects.map { eo -> parameter.property.get(eo)!!::class }.toSet().size == 1
                    } else {
                        editingObjects.map { eo -> parameter.property.get(eo) }.toSet().size == 1
                    }
                )
            }
            .toMap()

    }

    val labelledItemPanel = LabelledItemPanel().also { add(it) }

    val parameterJLabels = widgets.map { (parameter, widget) ->
        val label = labelledItemPanel.addItem(parameter.label, widget.widget)
        widget.events.valueChanged.on {
            widgets.forEach { (_, w) -> w.refresh(widget.parameter.property) }
        }
        parameter to label
    }.toMap()

    fun <T> makeWidget(userParameter: UserParameter2<O, T>, isConsistent: Boolean): ParameterWidget2<O, T> {
        return when (userParameter.value) {
            is Boolean -> BooleanWidget(
                this@AnnotatedPropertyEditor2,
                userParameter as UserParameter2<O, Boolean>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Int, is Short, is Long, is Double, is Float -> NumericWidget2(
                this@AnnotatedPropertyEditor2,
                userParameter as UserParameter2<O, *>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is CopyableObject -> ObjectWidget(
                this@AnnotatedPropertyEditor2,
                editingObjects.map { eo -> userParameter.property.get(eo) as CopyableObject },
                userParameter as UserParameter2<O, CopyableObject>,
                isConsistent
            ) as ParameterWidget2<O, T>

            else -> throw IllegalArgumentException("Unsupported type: ${userParameter.value!!::class.simpleName}")
        }
    }

    fun commit() {
        widgets.forEach { (parameter, widget) ->
            editingObjects.forEach { eo ->
                if (widget.isConsistent) {
                    if (widget is ObjectWidget<*, *>) {
                        widget.objectTypeEditor.commit()
                        (parameter.property as KMutableProperty1<O, Any?>).set(eo, widget.value.copy())
                    } else {
                        (parameter.property as KMutableProperty1<O, Any?>).set(eo, widget.value)
                    }
                }

            }
        }
    }

    init {
        widgets.map { (_, widget) -> widgets.forEach { (_, w) -> w.refresh(widget.parameter.property) } }
    }

}