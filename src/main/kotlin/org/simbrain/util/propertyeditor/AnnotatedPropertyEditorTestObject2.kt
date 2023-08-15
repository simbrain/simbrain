package org.simbrain.util.propertyeditor

import org.simbrain.util.displayInDialog

class AnnotatedPropertyEditorTestObject2 {

    // var simpleInt by UserParameter2(
    //     initValue = 1
    // )

    var testBoolean by UserParameter2(
        initValue = true
    )

    var conditionallyEnabledBoolean by UserParameter2(
        initValue = true,
        onUpdate = {
            if (updateEventProperty == AnnotatedPropertyEditorTestObject2::testBoolean) {
                enableWidget(widgetValue(AnnotatedPropertyEditorTestObject2::testBoolean))
            }
        }
    )

}

fun main() {
    val editingObject = AnnotatedPropertyEditorTestObject2()
    editingObject.testBoolean = false
    val editor = AnnotatedPropertyEditor2(listOf(editingObject))
    editor.displayInDialog()
}