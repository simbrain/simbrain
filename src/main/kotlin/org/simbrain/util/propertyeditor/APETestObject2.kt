package org.simbrain.util.propertyeditor

import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.allPropertiesToString
import org.simbrain.util.displayInDialog

class APETestObject2 {

    // var simpleInt by UserParameter2(
    //     initValue = 1
    // )

    @UserParameter(label = "Annotated Int", description = "Annotated Int Description")
    var annotatedInt = 1

    var testString by GuiEditable(
        initValue = "test"
    )

    var testBoolean by GuiEditable(
        initValue = true
    )

    var conditionallyEnabledBoolean by GuiEditable(
        initValue = true,
        onUpdate = {
            if (updateEventProperty == APETestObject2::testBoolean) {
                enableWidget(widgetValue(APETestObject2::testBoolean))
            }
        }
    )

    var conditionallyEnabledInt by GuiEditable(
        initValue = 1,
        onUpdate = {
            if (updateEventProperty == APETestObject2::testBoolean) {
                showWidget(widgetValue(APETestObject2::testBoolean))
            }
        }
    )

    var neuronUpdateRule: NeuronUpdateRule<*, *> by GuiEditable(
        initValue = LinearRule()
    )

    var dataHolder: ScalarDataHolder by GuiEditable(
        initValue = BiasedScalarData(1.0),
        onUpdate = {
            if (updateEventProperty == APETestObject2::neuronUpdateRule) {
                refreshValue(widgetValue(APETestObject2::neuronUpdateRule).createScalarData())
                // showWidget(widgetValue(APETestObject2::testBoolean))
            }
        }
    )

    var testDouble by GuiEditable(
        initValue = 1.3,
    )

    var testObject: TestObjectBase by GuiEditable(
        initValue = TestInnerObject1(1),
    )

    abstract class TestObjectBase: CopyableObject {
        companion object {
            val types = listOf(
                TestInnerObject1::class,
                TestInnerObject2::class,
            )
        }
    }

    class TestInnerObject1(testInt: Int = 1): TestObjectBase() {
        var test1Int by GuiEditable(
            initValue = testInt,
        )

        override fun copy(): CopyableObject {
            return TestInnerObject1(test1Int)
        }

        override fun toString(): String {
            return "TestInnerObject1(${allPropertiesToString(", ")})"
        }

    }

    class TestInnerObject2(testInt: Int = 2, testBoolean: Boolean = true): TestObjectBase() {

        var test2Boolean by GuiEditable(
            initValue = testBoolean,
        )

        var test2Int by GuiEditable(
            initValue = testInt,
        )

        override fun copy(): CopyableObject {
            return TestInnerObject2(test2Int, test2Boolean)
        }

        override fun toString(): String {
            return "TestInnerObject2(${allPropertiesToString(", ")}})"
        }
    }

    override fun toString(): String {
        return "APETestObject2(${allPropertiesToString(", ")})"
    }

}

fun main() {
    val editingObject = List(2) { APETestObject2() }
    editingObject.first().apply {
        testBoolean = false
        testDouble = 2.3
        testString = "test1"
        // (testObject as AnnotatedPropertyEditorTestObject2.TestInnerObject1).test1Int = 3
        testObject = APETestObject2.TestInnerObject2(3, false)
    }
    val editor = AnnotatedPropertyEditor2(editingObject)
    editor.displayInDialog {
        commit()
        editingObject.forEach { println(it) }
    }


}