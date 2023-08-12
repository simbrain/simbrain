package org.simbrain.util.propertyeditor

import org.simbrain.util.displayInDialog

class AnnotatedPropertyEditorTestObject2 {

    // var simpleInt by UserParameter2(
    //     initValue = 1
    // )

    var intControl by UserParameter2(
        initValue = true
    )

    var conditionallyEnabledInt by UserParameter2(
        initValue = true,
        onUpdate = {
            if (updateEventProperty == AnnotatedPropertyEditorTestObject2::intControl) {
                enableWidget(widgetValue(AnnotatedPropertyEditorTestObject2::intControl))
            }
        }
    )

}

fun main() {
    val editingObject = AnnotatedPropertyEditorTestObject2()
    editingObject.intControl = false
    val editor = AnnotatedPropertyEditor2(listOf(editingObject))
    editor.displayInDialog()
}