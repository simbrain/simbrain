package org.simbrain.util.propertyeditor

import org.simbrain.util.LabelledItemPanel
import javax.swing.JPanel
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class AnnotatedPropertyEditor2<O: Any>(val editingObjects: List<O>): JPanel() {

    val widgets = editingObjects.first().let { obj ->

        obj::class.memberProperties
            .asSequence()
            .filterIsInstance<KMutableProperty1<O, *>>()
            .onEach { it.isAccessible = true }
            .mapNotNull { property -> property.getDelegate(obj)?.also { property.get(obj) } }
            .filterIsInstance<UserParameter2<O, *>>()
            .map { parameter -> parameter.label to parameter::widgetValue }
            .onEach { (label, parameter) -> parameter.isAccessible = true }
            .map { (label, property) -> label to property.getDelegate() as ParameterWidget2<*, *> }
            .toList()

    }

    init {

        val labelledItemPanel = LabelledItemPanel()

        widgets.forEach { (label, widget) ->
            labelledItemPanel.addItem(label, widget.widget)
            widget.events.valueChanged.on {
                widgets.forEach { (_, w) -> w.refresh(widget.parameter.property) }
            }
            widgets.forEach { (_, w) -> w.refresh(widget.parameter.property) }
        }

        add(labelledItemPanel)
    }

}