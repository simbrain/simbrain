package org.simbrain.util.propertyeditor

import org.simbrain.util.LabelledItemPanel
import org.simbrain.util.UserParameter
import smile.math.matrix.Matrix
import java.awt.Color
import javax.swing.JPanel
import javax.swing.JTabbedPane
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

// TODO: Adapt javadoc from APE
/**
 * A panel that takes a set of objects either annotated with [UserParameter] or set to [GuiEditable] objects, and
 * produces a set of appropriate widgets for editing each object in a provided list.
 *
 * @param O the type of the edited objects. Example Neuron when editing a list of neurons.
 */
class AnnotatedPropertyEditor<O : EditableObject>(val editingObjects: List<O>) : JPanel() {

    constructor(vararg editingObjects: O) : this(editingObjects.toList())

    val parameterWidgetMap = editingObjects.first().let { obj ->


        val delegatedPropertyNames = HashSet<String>()

        /**
         * Properties using [GuiEditable].
         *
         * Ex:
         * ```
         * var testString by GuiEditable(initValue = "test")
         * ````
         */
        val delegated = (obj::class.allSuperclasses + obj::class)
            .map { it.declaredMemberProperties }
            .flatten()
            .filterIsInstance<KMutableProperty1<O, *>>()
            .onEach { it.isAccessible = true }
            .mapNotNull { property ->
                property.getDelegate(obj)?.also {
                    property.get(obj)
                    delegatedPropertyNames.add(property.name)
                }
            }
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
        val annotated = (obj::class.allSuperclasses + obj::class)
            .map { it.declaredMemberProperties }
            .flatten()
            .filterNot {
                // If a property has both a UserParameter annotation and a GuiEditable delegation, only keep the delegation
                delegatedPropertyNames.contains(it.name)
            }
            .mapNotNull {
                (it.annotations
                    .filterIsInstance<UserParameter>()
                    .firstOrNull()
                    ?.toGuiEditable<O>(it.getter.call(obj)!!)
                    ?.also { up -> up.getValue(obj, it) })
            }

        (delegated + annotated)
            .map { parameter ->
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

    val propertyWidgetMap = parameterWidgetMap.map { (parameter, widget) ->
        parameter.property to widget
    }.toMap()

    val labelledItemPanelsByTab = LinkedHashMap<String?, LabelledItemPanel>()

    private fun getLabelledItemPanel(tab: String?): LabelledItemPanel {
        return labelledItemPanelsByTab.getOrPut(if (tab.isNullOrEmpty()) "Main" else tab) {
            LabelledItemPanel()
        }
    }

    val parameterJLabels = parameterWidgetMap.entries
        .sortedBy { (parameter) -> parameter.order }
        .mapNotNull { (parameter, widget) ->
            // object widgets span the dialog and don’t use labels
            if (widget is ObjectWidget<*, *>) {
                getLabelledItemPanel(parameter.tab).addItem(widget.widget)
                widget.events.valueChanged.on {
                    parameterWidgetMap.forEach { (_, w) -> w.refresh(widget.parameter.property) }
                }
                null
            } else {
                val label = getLabelledItemPanel(parameter.tab).addItem(parameter.label, widget.widget)
                label.toolTipText = parameter.description
                widget.events.valueChanged.on {
                    parameterWidgetMap.forEach { (_, w) -> w.refresh(widget.parameter.property) }
                }
                parameter to label
            }
        }.toMap()

    val mainPanel = if (labelledItemPanelsByTab.size == 1) {
        labelledItemPanelsByTab.values.first()
    } else {
        JTabbedPane().also {
            labelledItemPanelsByTab.forEach { (tab, panel) ->
                it.addTab(tab, panel)
            }
        }
    }.also { add(it) }

    /**
     * Returns a string describing what object or objects are being edited
     */
    val titleString: String
        get() {
            return if (editingObjects.size == 1) {
                "Edit ${editingObjects[0].name}"
            } else {
                "Edit ${editingObjects.size} ${editingObjects[0].javaClass.simpleName}s"
            }
        }

    fun <T> makeWidget(userParameter: GuiEditable<O, T>, isConsistent: Boolean): ParameterWidget2<O, T> {
        if (userParameter.displayOnly) {
            return DisplayOnlyWidget(
                this@AnnotatedPropertyEditor,
                userParameter,
                isConsistent
            )
        }

        return when (userParameter.value) {

            is String -> StringWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, String>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Boolean -> BooleanWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, Boolean>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Int, is Short, is Long, is Double, is Float -> NumericWidget2(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, *>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Color -> ColorWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, Color>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Enum<*> -> EnumWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, Enum<*>>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is DoubleArray -> DoubleArrayWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, DoubleArray>,
                isConsistent
            ) as ParameterWidget2<O, T>

            is Matrix -> MatrixWidget2(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, Matrix>,
                isConsistent
            ) as ParameterWidget2<O, T>


            is CopyableObject -> ObjectWidget(
                this@AnnotatedPropertyEditor,
                editingObjects.map { eo -> userParameter.property.get(eo) as CopyableObject },
                userParameter as GuiEditable<O, CopyableObject>,
                isConsistent
            ) as ParameterWidget2<O, T>

            else -> throw IllegalArgumentException("Unsupported type: ${userParameter.value!!::class.simpleName}")
        }
    }

    fun commitChanges() {
        parameterWidgetMap.forEach { (parameter, widget) ->
            editingObjects.forEach { eo ->
                if (widget.isConsistent) {
                    if (widget is ObjectWidget<*, *>) {
                        widget.objectTypeEditor.commitChanges()
                        parameter.property.setter.call(eo, widget.value.copy())
                    } else {
                        parameter.property.setter.call(eo, widget.value)
                    }
                }

            }
        }
    }

    init {
        parameterWidgetMap.map { (_, widget) -> parameterWidgetMap.forEach { (_, w) -> w.refresh(widget.parameter.property) } }
    }

}