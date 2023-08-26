package org.simbrain.util.propertyeditor

import org.simbrain.util.LabelledItemPanel
import org.simbrain.util.UserParameter
import java.awt.Color
import javax.swing.JPanel
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

// TODO: Adapt javadoc from APE
/**
 * A panel that takes a set of objects either annotated with [UserParameter] or set to [GuiEditable] objects, and
 * produces a set of appropriate widgets for editing each object in a provided list.
 *
 * @param O the type of the edited objects. Example Neuron when editing a list of neurons.
 */
class AnnotatedPropertyEditor2<O : Any>(val editingObjects: List<O>) : JPanel() {

    val widgets = editingObjects.first().let { obj ->

        /**
         * Properties using [GuiEditable].
         *
         * Ex:
         * ```
         * var testString by GuiEditable(initValue = "test")
         * ````
         */
        val delegated = obj::class.memberProperties
            .asSequence()
            .filterIsInstance<KMutableProperty1<O, *>>()
            .onEach { it.isAccessible = true }
            .mapNotNull { property -> property.getDelegate(obj)?.also { property.get(obj) } }
            .filterIsInstance<GuiEditable<O, *>>()

        /**
         * Properties using [UserParameter] annotations
         *
         * Ex:
         * ```
         * @UserParameter()
         * var testString = initValue
         * ```
         */
        val annotated = obj::class.memberProperties
            .asSequence()
            .mapNotNull {
                (it.annotations
                    .filterIsInstance<UserParameter>()
                    .firstOrNull()
                    ?.toGuiEditable<O>(it.getter.call(obj)!!)
                    ?.also { up -> up.getValue(obj, it) })
            }

        (delegated + annotated).map { parameter ->
                parameter to makeWidget(
                    parameter,
                    if (parameter.value is CopyableObject) {
                        editingObjects.map { eo -> parameter.property.getter.call(eo)!!::class }.toSet().size == 1
                    } else {
                        editingObjects.map { eo -> parameter.property.getter.call(eo) }.toSet().size == 1
                    }
                )
            }.toMap()

    }

    val labelledItemPanel = LabelledItemPanel().also { add(it) }

    val parameterJLabels =
        widgets.entries.sortedBy { (parameter) -> parameter.order }.associate { (parameter, widget) ->
            val label = labelledItemPanel.addItem(parameter.label, widget.widget)
            label.toolTipText = parameter.description
            widget.events.valueChanged.on {
                widgets.forEach { (_, w) -> w.refresh(widget.parameter.property) }
            }
            parameter to label
        }

    fun <T> makeWidget(userParameter: GuiEditable<O, T>, isConsistent: Boolean): ParameterWidget2<O, T> {
        if (userParameter.displayOnly) {
            return DisplayOnlyWidget(
                this@AnnotatedPropertyEditor2,
                userParameter,
                isConsistent
            )
        }

        return when (userParameter.value) {

            is String -> StringWidget(
                this@AnnotatedPropertyEditor2,
                userParameter as GuiEditable<O, String>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Boolean -> BooleanWidget(
                this@AnnotatedPropertyEditor2,
                userParameter as GuiEditable<O, Boolean>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Int, is Short, is Long, is Double, is Float -> NumericWidget2(
                this@AnnotatedPropertyEditor2,
                userParameter as GuiEditable<O, *>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Color -> ColorWidget(
                this@AnnotatedPropertyEditor2,
                userParameter as GuiEditable<O, Color>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Enum<*> -> EnumWidget(
                this@AnnotatedPropertyEditor2,
                userParameter as GuiEditable<O, Enum<*>>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is DoubleArray -> DoubleArrayWidget(
                this@AnnotatedPropertyEditor2,
                userParameter as GuiEditable<O, DoubleArray>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is CopyableObject -> ObjectWidget(
                this@AnnotatedPropertyEditor2,
                editingObjects.map { eo -> userParameter.property.get(eo) as CopyableObject },
                userParameter as GuiEditable<O, CopyableObject>,
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
                        parameter.property.setter.call(eo, widget.value.copy())
                    } else {
                        parameter.property.setter.call(eo, widget.value)
                    }
                }

            }
        }
    }

    init {
        widgets.map { (_, widget) -> widgets.forEach { (_, w) -> w.refresh(widget.parameter.property) } }
    }

}