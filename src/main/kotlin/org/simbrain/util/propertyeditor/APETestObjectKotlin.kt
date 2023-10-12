package org.simbrain.util.propertyeditor

import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.allPropertiesToString
import org.simbrain.util.displayInDialog
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

class APETestObjectKotlin: EditableObject {

    override var name by GuiEditable(
        initValue = "test",
        displayOnly = true,
    )

    var randomizer: ProbabilityDistribution by GuiEditable(
        initValue = UniformRealDistribution(),
        tab = "Test Tab"
    )

    @UserParameter(label = "Annotated Int", description = "Annotated Int Description", tab = "Test Tab", order = 10)
    var annotatedInt = 1

    var testString by GuiEditable(
        initValue = "test",
        tab = "Test Tab",
    )
    var conditionallyEnabledBoolean by GuiEditable(
        initValue = true,
        onUpdate = {
            if (updateEventProperty == APETestObjectKotlin::testBoolean) {
                enableWidget(widgetValue(APETestObjectKotlin::testBoolean))
            }
        },
        order = 1
    )

    var conditionallyEnabledInt by GuiEditable(
        initValue = 1,
        onUpdate = {
            if (updateEventProperty == APETestObjectKotlin::testBoolean) {
                showWidget(widgetValue(APETestObjectKotlin::testBoolean))
            }
        },
        order = 2
    )


    var testBoolean by GuiEditable(
        initValue = true,
        order = 3
    )

    var testDouble by GuiEditable(
        initValue = 1.3,
        order = 4
    )

    var testObject: TestObjectBase by GuiEditable(
        initValue = TestInnerObject1(1),
        order = 5
    )

    var neuronUpdateRule: NeuronUpdateRule<*, *> by GuiEditable(
        initValue = LinearRule(),
        showDetails = false,
        order = 10
    )

    var dataHolder: ScalarDataHolder by GuiEditable(
        initValue = BiasedScalarData(1.0),
        onUpdate = {
            if (updateEventProperty == APETestObjectKotlin::neuronUpdateRule) {
                refreshValue(widgetValue(APETestObjectKotlin::neuronUpdateRule).createScalarData())
                // showWidget(widgetValue(APETestObject2::testBoolean))
            }
        },
        order = 11
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
    val editingObject = List(2) { APETestObjectKotlin() }
    editingObject.first().apply {
        testBoolean = false
        testDouble = 2.3
        testString = "test1"
        name = "test1"
        // (testObject as AnnotatedPropertyEditorTestObject2.TestInnerObject1).test1Int = 3
        testObject = APETestObjectKotlin.TestInnerObject2(3, false)
    }
    val editor = AnnotatedPropertyEditor(editingObject)
    editor.displayInDialog {
        commitChanges()
        editingObject.forEach { println(it) }
    }

    // AnnotatedPropertyEditor(objectWrapper<ProbabilityDistribution>("randomizer", UniformRealDistribution())).displayInDialog {
    //     commitChanges()
    //     editingObjects.forEach {
    //         val thing = it.editingObject
    //         println(thing.name)
    //     }
    // }


}