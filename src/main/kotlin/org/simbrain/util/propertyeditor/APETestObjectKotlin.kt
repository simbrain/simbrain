package org.simbrain.util.propertyeditor

import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.allPropertiesToString
import org.simbrain.util.displayInDialog
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

@APETabOder( "Main", "Test Tab")
class APETestObjectKotlin: EditableObject {

    override var name by GuiEditable(
        initValue = "test",
        displayOnly = true,
    )

    var randomizer: ProbabilityDistribution by GuiEditable(
        initValue = UniformRealDistribution(),
        tab = "Test Tab"
    )

    @UserParameter(label = "Annotated Int", description = "Annotated Int Description", minimumValue = -2.0, maximumValue = 3.0, tab = "Test Tab", order = 10)
    var annotatedInt = 1

    @UserParameter(label = "Annotated Nullable String", description = "Annotated Nullable String Description", tab = "Test Tab", order = 11)
    var nullableString: String? = null

    var testString by GuiEditable(
        initValue = "test",
        tab = "Test Tab",
    )

    var controlBoolean by GuiEditable(
        initValue = true,
        description = "This controls the state of several other widgets in this panel",
        order = 1
    )

    var conditionallyEnabledBoolean by GuiEditable(
        initValue = true,
        conditionallyEnabledBy = APETestObjectKotlin::controlBoolean,
        order = 10
    )

    /**
     * Use this "onChange" approach to perform custom actions when the specified property is updated
     */
    var conditionallyDisabledBoolean by GuiEditable(
        initValue = true,
        onUpdate = {
            enableWidget(!widgetValue(::controlBoolean))
        },
        order = 20
    )

    var conditionalVisibileInt by GuiEditable(
        initValue = 1,
        conditionallyVisibleBy = APETestObjectKotlin::controlBoolean,
        order = 30
    )

    var conditionallyHiddenInt by GuiEditable(
        initValue = 1,
        onUpdate = {
            if (updateEventProperty == APETestObjectKotlin::controlBoolean) {
                showWidget(!widgetValue(APETestObjectKotlin::controlBoolean))
            }
        },
        order = 40
    )

    var testDouble by GuiEditable(
        initValue = 1.3,
        min = -3.1415926535,
        max = 3.1415926535,
        order = 50
    )

    var testObject: TestObjectBase by GuiEditable(
        initValue = TestInnerObject1(1),
        order = 60
    )

    var testDoubleArray by GuiEditable(
        initValue = doubleArrayOf(1.0, -1.0),
        columnMode = false,
        order = 65
    )

    var testIntArray by GuiEditable(
        initValue = intArrayOf(1, -1),
        columnMode = false,
        order = 67
    )

    var neuronUpdateRule: NeuronUpdateRule<*, *> by GuiEditable(
        initValue = LinearRule(),
        showDetails = false,
        order = 70
    )

    var dataHolder: ScalarDataHolder by GuiEditable(
        initValue = SpikingScalarData(false),
        onUpdate = {
            refreshValue(widgetValue(::neuronUpdateRule).createScalarData())
        },
        order = 80
    )

    private var _intValue: Int = 0

    var intValueWithCustomBacking: Int by GuiEditable(
        initValue = _intValue,
        order = 90,
        getter = {
            println("Getting value from _intValue: $_intValue")
            _intValue
         },
        setter = {
            println("Setting value to _intValue: $_intValue")
            _intValue = it
        },
    )

    var intEnablingCheckBox = false

    var intValueWithEnablingCheckBox: Int by GuiEditable(
        initValue = 0,
        order = 100,
        useCheckboxFrom = APETestObjectKotlin::intEnablingCheckBox,
        getter = {
            println("Getting value from _intValue: $_intValue")
            _intValue
         },
        setter = {
            println("Setting value to _intValue: $_intValue")
            _intValue = it
        },
    )

    abstract class TestObjectBase: CopyableObject {
        override fun getTypeList() = listOf(
            TestInnerObject1::class.java,
            TestInnerObject2::class.java,
        )
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
        return "APETestObject2(${allPropertiesToString()})"
    }

}

fun main() {
    val editingObject = List(2) { APETestObjectKotlin() }
    editingObject.first().apply {
        testDouble = 2.3
        testString = "test1"
        name = "test1"
        // (testObject as AnnotatedPropertyEditorTestObject2.TestInnerObject1).test1Int = 3
        testObject = APETestObjectKotlin.TestInnerObject2(3, false)
    }
    val editor = AnnotatedPropertyEditor(editingObject).displayInDialog {
        commitChanges()
        println(editingObject)
        println("is the thing null?: ${editingObject.first().nullableString == null}")
    }

    // AnnotatedPropertyEditor(objectWrapper<ProbabilityDistribution>("randomizer", UniformRealDistribution())).displayInDialog {
    //     commitChanges()
    //     editingObjects.forEach {
    //         val thing = it.editingObject
    //         println(thing.name)
    //     }
    // }


}