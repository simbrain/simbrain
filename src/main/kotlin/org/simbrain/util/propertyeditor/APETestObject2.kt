package org.simbrain.util.propertyeditor

import org.simbrain.util.displayInDialog

class APETestObject2 {

    // var simpleInt by UserParameter2(
    //     initValue = 1
    // )

    var testBoolean by UserParameter2(
        initValue = true
    )

    var conditionallyEnabledBoolean by UserParameter2(
        initValue = true,
        onUpdate = {
            if (updateEventProperty == APETestObject2::testBoolean) {
                enableWidget(widgetValue(APETestObject2::testBoolean))
            }
        }
    )

    var conditionallyEnabledInt by UserParameter2(
        initValue = 1,
        onUpdate = {
            if (updateEventProperty == APETestObject2::testBoolean) {
                showWidget(widgetValue(APETestObject2::testBoolean))
            }
        }
    )

    var testDouble by UserParameter2(
        initValue = 1.3,
    )

    var testObject: TestObjectBase by UserParameter2(
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
        var test1Int by UserParameter2(
            initValue = testInt,
        )

        override fun copy(): CopyableObject {
            return TestInnerObject1(test1Int)
        }

        override fun toString(): String {
            return "TestInnerObject1(test1Int=$test1Int)"
        }

    }

    class TestInnerObject2(testInt: Int = 2, testBoolean: Boolean = true): TestObjectBase() {

        var test2Boolean by UserParameter2(
            initValue = testBoolean,
        )

        var test2Int by UserParameter2(
            initValue = testInt,
        )

        override fun copy(): CopyableObject {
            return TestInnerObject2(test2Int, test2Boolean)
        }

        override fun toString(): String {
            return "TestInnerObject2(test2Boolean=$test2Boolean, test2Int=$test2Int)"
        }
    }

    override fun toString(): String {
        return "AnnotatedPropertyEditorTestObject2(testBoolean=$testBoolean, conditionallyEnabledBoolean=$conditionallyEnabledBoolean, conditionallyEnabledInt=$conditionallyEnabledInt, testDouble=$testDouble, testObject=$testObject)"
    }

}


fun main() {
    val editingObject = List(2) { APETestObject2() }
    editingObject.first().apply {
        testBoolean = false
        testDouble = 2.3
        // (testObject as AnnotatedPropertyEditorTestObject2.TestInnerObject1).test1Int = 3
        testObject = APETestObject2.TestInnerObject2(3, false)
    }
    val editor = AnnotatedPropertyEditor2(editingObject)
    editor.displayInDialog {
        commit()
        editingObject.forEach { println(it) }
    }


}