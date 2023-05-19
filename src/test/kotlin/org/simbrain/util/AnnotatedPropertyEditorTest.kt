package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.widgets.NumericWidget
import org.simbrain.util.widgets.TextWithNull

/**
 * Also see [TestObject.java]
 */
class AnnotatedPropertyEditorTest {

    var net = Network()
    val n1 = Neuron(net)
    val n2 = Neuron(net)

    // Todo
    //  Check each data type
    //  Check each widget type (see Parameter Widget and org.simbrain.util.widgets)
    //  Test as many fields of UserParameter as possible. Esp min / max.
    //  Check internal list of todos

    @Test
    fun `test commit numeric widget`() {
        val ape = AnnotatedPropertyEditor(n1)
        (ape.getWidget("Activation")?.component as NumericWidget).value = .75
        ape.commitChanges()
        assertEquals(.75, n1.activation)
    }

    @Test
    fun `test fill field value numeric widget`() {
        n1.forceSetActivation(.75)
        val ape = AnnotatedPropertyEditor(n1)
        val widgetVal = (ape.getWidget("Activation")?.component as NumericWidget).value
        assertEquals(.75, widgetVal)
    }

    @Test
    fun `test commit string widget`() {
        val ape = AnnotatedPropertyEditor(n1)
        (ape.getWidget("Label")?.component as TextWithNull).text = "test"
        ape.commitChanges()
        assertEquals("test", n1.label)
    }

    @Test
    fun `test fill field value string widget`() {
        n1.label = "test"
        val ape = AnnotatedPropertyEditor(n1)
        val widgetVal = (ape.getWidget("Label")?.component as TextWithNull).text
        assertEquals("test", widgetVal)
    }

    @Test
    fun `test fill field value with two consistent values`() {
        n1.forceSetActivation(.75)
        n2.forceSetActivation(.75)
        val ape = AnnotatedPropertyEditor(listOf(n1, n2))
        val widgetVal = (ape.getWidget("Activation")?.component as NumericWidget).value
        assertEquals(.75, widgetVal)
    }

    @Test
    fun `test fill field value with two inconsistent values`() {
        n1.forceSetActivation(.75)
        n2.forceSetActivation(.74)
        val ape = AnnotatedPropertyEditor(listOf(n1, n2))
        val widget = (ape.getWidget("Activation")?.component as NumericWidget)
        assertEquals(true, widget.isNull)
    }

    @Test
    fun `test commit when two inconsistent values were made consistent`() {
        n1.forceSetActivation(.75)
        n2.forceSetActivation(.74)
        val ape = AnnotatedPropertyEditor(listOf(n1, n2))
        val widget = (ape.getWidget("Activation")?.component as NumericWidget)
        widget.value = .8
        ape.commitChanges()
        assertEquals(.8, n1.activation)
        assertEquals(.8, n2.activation)
    }

    @Test
    fun `test commit of null state on numeric widget`() {
        n1.forceSetActivation(.75)
        n2.forceSetActivation(.74)
        val ape = AnnotatedPropertyEditor(listOf(n1, n2))
        val widget = (ape.getWidget("Activation")?.component as NumericWidget)
        ape.commitChanges()
        // States should be unchanged
        assertEquals(.75, n1.activation)
        assertEquals(.74, n2.activation)
    }



}