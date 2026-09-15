package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.AfdThermoreceptorRule
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.activity_generators.SinusoidalRule
import org.simbrain.util.SimbrainConstants.NULL_STRING
import org.simbrain.util.propertyeditor.*
import smile.math.matrix.Matrix
import java.awt.Color
import javax.swing.JComboBox
import javax.swing.JButton
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
    fun `differing matrix contents show the placeholder and are not committed`() {
        val o1 = APETestObjectKotlin()
        val o2 = APETestObjectKotlin().apply { testMatrix = Matrix.column(doubleArrayOf(5.0, 6.0)) }
        val ape = AnnotatedPropertyEditor(o1, o2)
        val widget = ape.propertyNameWidgetMap["testMatrix"] as MatrixWidget
        assertFalse(widget.isConsistent)
        assertFalse(widget.isShowingTable)
        assertTrue(widget.canEditInconsistentValues)
        assertEquals("Edit", widget.widget.findButton()?.text)
        assertTrue(widget.widget.findButton()!!.isEnabled)
        ape.commitChanges()
        assertEquals(1.0, o1.testMatrix[0, 0])
        assertEquals(5.0, o2.testMatrix[0, 0])
    }

    @Test
    fun `choosing to edit inconsistent arrays commits the table to every object`() {
        val o1 = APETestObjectKotlin()
        val o2 = APETestObjectKotlin().apply { testDoubleArray = doubleArrayOf(5.0, 6.0) }
        val ape = AnnotatedPropertyEditor(o1, o2)
        val widget = ape.propertyNameWidgetMap["testDoubleArray"] as DoubleArrayWidget
        widget.editInconsistentValues()
        assertTrue(widget.isConsistent)
        assertTrue(widget.isShowingTable)
        ape.commitChanges()
        assertArrayEquals(doubleArrayOf(1.0, -1.0), o1.testDoubleArray)
        assertArrayEquals(doubleArrayOf(1.0, -1.0), o2.testDoubleArray)
    }

    @Test
    fun `inconsistent arrays of different sizes cannot be edited together`() {
        val o1 = APETestObjectKotlin()
        val o2 = APETestObjectKotlin().apply { testStringArray = arrayOf("a", "b", "c") }
        val ape = AnnotatedPropertyEditor(o1, o2)
        val widget = ape.propertyNameWidgetMap["testStringArray"] as StringArrayWidget
        assertFalse(widget.canEditInconsistentValues)
        assertFalse(widget.widget.findButton()!!.isEnabled)
        widget.editInconsistentValues()
        assertFalse(widget.isConsistent)
        ape.commitChanges()
        assertEquals(3, o2.testStringArray.size)
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

    private fun JComponent.findButton(): JButton? =
        components.filterIsInstance<JComponent>().firstNotNullOfOrNull { c ->
            c as? JButton ?: c.findButton()
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
