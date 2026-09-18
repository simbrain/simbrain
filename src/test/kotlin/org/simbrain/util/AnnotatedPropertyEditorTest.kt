package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.updaterules.AfdThermoreceptorRule
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.activity_generators.SinusoidalRule
import org.simbrain.util.SimbrainConstants.NULL_STRING
import org.simbrain.util.propertyeditor.*
import smile.math.matrix.Matrix
import java.awt.Color
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JComponent
import javax.swing.JSpinner
import kotlin.reflect.full.declaredMemberProperties

/**
 * Also see [AnnotatedPropertyEditorTestObject.java]
 */
class AnnotatedPropertyEditorTest {

    var net = Network()
    val n1 = Neuron()
    val n2 = Neuron()

    // Todo
    //  Check each data type
    //  Check each widget type (see Parameter Widget and org.simbrain.util.widgets)
    //  Test as many fields of UserParameter as possible. Esp min / max.
    //  Check internal list of todos

    @Test
    fun `test commit numeric widget`() {
        val ape = AnnotatedPropertyEditor(n1)
        val prop = Neuron::class.declaredMemberProperties.first { it.name == "activation" }
        (ape.propertyNameWidgetMap[prop.name] as NumericWidget).widget.value = .75
        ape.commitChanges()
        assertEquals(.75, n1.activation)
    }

    @Test
    fun `test integer spinner increment commits as int`() {
        val testObject = APETestObjectKotlin()
        val ape = AnnotatedPropertyEditor(testObject)
        val widget = ape.getWidgetByLabel("Annotated Int") as NumericWidget
        widget.widget.value = widget.widget.nextValue
        ape.commitChanges()
        assertEquals(2, testObject.annotatedInt)
    }

    @Test
    fun `test fill field value numeric widget`() {
        n1.activation = .75
        val ape = AnnotatedPropertyEditor(n1)
        val prop = Neuron::class.declaredMemberProperties.first { it.name == "activation" }
        val widgetVal = (ape.propertyNameWidgetMap[prop.name] as NumericWidget).widget.value
        assertEquals(.75, widgetVal)
    }

    @Test
    fun `test commit string widget`() {
        val ape = AnnotatedPropertyEditor(n1)
        val prop = NetworkModel::class.declaredMemberProperties.first { it.name == "label" }
        (ape.propertyNameWidgetMap[prop.name] as StringWidget).textField.text = "test"
        ape.commitChanges()
        assertEquals("test", n1.label)
    }

    @Test
    fun `test fill field value string widget`() {
        n1.label = "test"
        val ape = AnnotatedPropertyEditor(n1)
        val prop = NetworkModel::class.declaredMemberProperties.first { it.name == "label" }
        val widgetVal = (ape.propertyNameWidgetMap[prop.name] as StringWidget).textField.text
        assertEquals("test", widgetVal)
    }

    @Test
    fun `test behavior two consistent values`() {
        n1.activation = .75
        n2.activation = .75
        val ape = AnnotatedPropertyEditor(n1, n2)
        val prop = Neuron::class.declaredMemberProperties.first { it.name == "activation" }
        assertEquals(true, (ape.propertyNameWidgetMap[prop.name] as NumericWidget).isConsistent)
        (ape.propertyNameWidgetMap[prop.name] as NumericWidget).widget.value = .25
        ape.commitChanges()
        assertEquals(.25, n1.activation)
        assertEquals(.25, n2.activation)
    }

    @Test
    fun `hidden type option appears in the update rule dropdown only when in use or exposed`() {
        fun updateRuleOptions(vararg neurons: Neuron): List<String> {
            val ape = AnnotatedPropertyEditor(neurons.toList())
            return (ape.propertyNameWidgetMap["updateRule"] as ObjectWidget).typeOptions
        }
        val hiddenName = AfdThermoreceptorRule::class.displayName
        val linearName = LinearRule::class.displayName

        assertFalse(updateRuleOptions(Neuron(LinearRule())).contains(hiddenName))
        assertTrue(updateRuleOptions(Neuron(AfdThermoreceptorRule())).contains(hiddenName))
        assertTrue(updateRuleOptions(Neuron(LinearRule()), Neuron(AfdThermoreceptorRule())).contains(hiddenName))
        assertTrue(updateRuleOptions(Neuron(LinearRule())).contains(linearName))

        val previousProvider = TypeOptionVisibility.exposedTypeNamesProvider
        try {
            TypeOptionVisibility.exposedTypeNamesProvider = { setOf(AfdThermoreceptorRule::class.qualifiedName!!) }
            assertTrue(updateRuleOptions(Neuron(LinearRule())).contains(hiddenName))
        } finally {
            TypeOptionVisibility.exposedTypeNamesProvider = previousProvider
        }
    }

    @Test
    fun `test behavior two inconsistent object values`() {
        n1.updateRule = LinearRule()
        n2.updateRule = SinusoidalRule()
        val ape = AnnotatedPropertyEditor(n1, n2)
        val prop = Neuron::class.declaredMemberProperties.first { it.name == "updateRule" }
        val selectedObjects = (((ape.propertyNameWidgetMap[prop.name] as ObjectWidget).widget).components.filterIsInstance<DetailTrianglePanel>().first().topPanelComponent as JComboBox<*>).selectedObjects
        assertEquals(1, selectedObjects.size)
        assertEquals(NULL_STRING, selectedObjects.first())
    }

    @Test
    fun `equal array and matrix contents across objects are consistent`() {
        val o1 = APETestObjectKotlin()
        val o2 = APETestObjectKotlin()
        val ape = AnnotatedPropertyEditor(o1, o2)
        listOf("testDoubleArray", "testIntArray", "testBooleanArray", "testStringArray", "testMatrix", "testColor").forEach {
            assertTrue(ape.propertyNameWidgetMap[it]!!.isConsistent, "$it should be consistent")
        }
    }

    @Test
    fun `differing matrix cells show as null and are not committed until edited`() {
        val o1 = APETestObjectKotlin()
        val o2 = APETestObjectKotlin().apply { testMatrix = Matrix.column(doubleArrayOf(1.0, 6.0)) }
        val ape = AnnotatedPropertyEditor(o1, o2)
        val widget = ape.propertyNameWidgetMap["testMatrix"] as MatrixWidget
        assertFalse(widget.isConsistent)
        assertTrue(widget.canEditInconsistentValues)
        assertEquals(1.0, widget.model.getValueAt(0, 0))
        assertEquals(null, widget.model.getValueAt(1, 0))
        ape.commitChanges()
        assertEquals(2.0, o1.testMatrix[1, 0])
        assertEquals(6.0, o2.testMatrix[1, 0])
        widget.model.setValueAt(9.0, 1, 0)
        assertTrue(widget.isConsistent)
        ape.commitChanges()
        assertEquals(9.0, o1.testMatrix[1, 0])
        assertEquals(9.0, o2.testMatrix[1, 0])
        assertEquals(1.0, o2.testMatrix[0, 0])
    }

    @Test
    fun `committing a non-square matrix keeps its orientation`() {
        val o1 = APETestObjectKotlin().apply { testMatrix = Matrix.of(arrayOf(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(10.0, 11.0, 12.0))) }
        val ape = AnnotatedPropertyEditor(o1)
        ape.commitChanges()
        assertEquals(2, o1.testMatrix.nrow())
        assertEquals(3, o1.testMatrix.ncol())
        assertEquals(12.0, o1.testMatrix[1, 2])
    }

    @Test
    fun `editing one differing array cell commits it to every object and leaves the other cells alone`() {
        val o1 = APETestObjectKotlin()
        val o2 = APETestObjectKotlin().apply { testDoubleArray = doubleArrayOf(5.0, 6.0) }
        val ape = AnnotatedPropertyEditor(o1, o2)
        val widget = ape.propertyNameWidgetMap["testDoubleArray"] as DoubleArrayWidget
        assertEquals(null, widget.model.getValueAt(0, 0))
        assertEquals(null, widget.model.getValueAt(0, 1))
        widget.model.setValueAt(7.0, 0, 0)
        assertTrue(widget.isConsistent)
        ape.commitChanges()
        assertArrayEquals(doubleArrayOf(7.0, -1.0), o1.testDoubleArray)
        assertArrayEquals(doubleArrayOf(7.0, 6.0), o2.testDoubleArray)
    }

    @Test
    fun `boolean array cells round trip through the table`() {
        val o1 = APETestObjectKotlin()
        val o2 = APETestObjectKotlin().apply { testBooleanArray = booleanArrayOf(false, false) }
        val ape = AnnotatedPropertyEditor(o1, o2)
        val widget = ape.propertyNameWidgetMap["testBooleanArray"] as BooleanArrayWidget
        assertEquals(null, widget.model.getValueAt(0, 0))
        assertEquals(0, widget.model.getValueAt(0, 1))
        widget.model.setValueAt(1, 0, 1)
        ape.commitChanges()
        assertArrayEquals(booleanArrayOf(true, true), o1.testBooleanArray)
        assertArrayEquals(booleanArrayOf(false, true), o2.testBooleanArray)
    }

    @Test
    fun `inconsistent arrays of different sizes show a placeholder and cannot be edited together`() {
        val o1 = APETestObjectKotlin()
        val o2 = APETestObjectKotlin().apply { testStringArray = arrayOf("a", "b", "c") }
        val ape = AnnotatedPropertyEditor(o1, o2)
        val widget = ape.propertyNameWidgetMap["testStringArray"] as StringArrayWidget
        assertFalse(widget.canEditInconsistentValues)
        assertEquals(NULL_STRING, widget.widget.findLabel()?.text)
        ape.commitChanges()
        assertEquals(3, o2.testStringArray.size)
        assertEquals(2, o1.testStringArray.size)
    }

    @Test
    fun `tensor cells that differ show as null and merge per object on commit`() {
        val l1 = TensorLayer(TensorShape(2, 2))
        val l2 = TensorLayer(TensorShape(2, 2)).apply { biases[3] = 1.0 }
        val ape = AnnotatedPropertyEditor(l1, l2)
        val widget = ape.propertyNameWidgetMap["biases"] as TensorWidget
        assertTrue(widget.canEditInconsistentValues)
        assertEquals(0.0, widget.sliceModels[0].getValueAt(0, 0))
        assertEquals(null, widget.sliceModels[0].getValueAt(1, 1))
        widget.sliceModels[0].setValueAt(5.0, 0, 0)
        ape.commitChanges()
        assertArrayEquals(doubleArrayOf(5.0, 0.0, 0.0, 0.0), l1.biases)
        assertArrayEquals(doubleArrayOf(5.0, 0.0, 0.0, 1.0), l2.biases)
    }

    @Test
    fun `tensors of equal length but different shape cannot be edited together`() {
        val l1 = TensorLayer(TensorShape(2, 2))
        val l2 = TensorLayer(TensorShape(4, 1)).apply { biases[0] = 1.0 }
        val ape = AnnotatedPropertyEditor(l1, l2)
        val widget = ape.propertyNameWidgetMap["biases"] as TensorWidget
        assertFalse(widget.canEditInconsistentValues)
        assertEquals(NULL_STRING, widget.widget.findLabel()?.text)
    }

    @Test
    fun `inconsistent colors show the null state until a color is chosen`() {
        val o1 = APETestObjectKotlin()
        val o2 = APETestObjectKotlin().apply { testColor = Color.RED }
        val ape = AnnotatedPropertyEditor(o1, o2)
        val widget = ape.propertyNameWidgetMap["testColor"] as ColorWidget
        assertFalse(widget.isConsistent)
        assertTrue(widget.widget.isNull)
        ape.commitChanges()
        assertEquals(Color.RED, o2.testColor)
        widget.widget.value = Color.BLUE
        assertTrue(widget.isConsistent)
        assertFalse(widget.widget.isNull)
        ape.commitChanges()
        assertEquals(Color.BLUE, o1.testColor)
        assertEquals(Color.BLUE, o2.testColor)
    }

    private fun JComponent.findLabel(): JLabel? =
        components.filterIsInstance<JComponent>().firstNotNullOfOrNull { c ->
            c as? JLabel ?: c.findLabel()
        }

    @Test
    fun `test behavior with inconsistent values`() {
        n1.activation = .75
        n2.activation = .74
        val ape = AnnotatedPropertyEditor(n1, n2)
        val prop = Neuron::class.declaredMemberProperties.first { it.name == "activation" }
        assertEquals(false, (ape.propertyNameWidgetMap[prop.name] as NumericWidget).isConsistent)
        assertEquals(NULL_STRING, ((ape.getWidgetByLabel("Activation") as NumericWidget).widget.editor as JSpinner.DefaultEditor).textField?.text)
        (ape.propertyNameWidgetMap[prop.name] as NumericWidget).widget.value = .25
        assertEquals("0.25", ((ape.getWidgetByLabel("Activation") as NumericWidget).widget.editor as JSpinner.DefaultEditor).textField?.text)
        ape.commitChanges()
        assertEquals(.25, n1.activation)
        assertEquals(.25, n2.activation)
    }

}
