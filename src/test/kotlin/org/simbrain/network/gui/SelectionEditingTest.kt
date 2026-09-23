/** Verifies selection targets and menu descriptions without opening property or training dialogs. */
package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.NetworkComponent
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.NumericWidget
import org.simbrain.util.CmdOrCtrl
import javax.swing.JComponent
import javax.swing.SwingUtilities
import java.awt.Container
import javax.swing.Action
import javax.swing.event.MenuEvent
import org.simbrain.network.gui.nodes.TextNode
import org.simbrain.network.gui.nodes.NeuronNode

class SelectionEditingTest {
    @Test
    fun `edit submenu tracks selected types and leaves command E on all`() {
        SwingUtilities.invokeAndWait {
            val panel = NetworkPanel(NetworkComponent("test", Network()))
            val texts = listOf(TextNode(panel, NetworkTextObject("one")), TextNode(panel, NetworkTextObject("two")))
            val neuron = NeuronNode(panel, Neuron())
            panel.selectionManager.set(texts + neuron)
            val menu = panel.createSelectionEditMenu()
            assertEquals("All...", menu.getItem(0).text)
            assertEquals("2 text objects...", menu.getItem(2).text)
            assertEquals("1 neuron...", menu.getItem(3).text)
            assertEquals(panel.networkActions.editSelectedModelsAction.getValue(Action.ACCELERATOR_KEY), menu.getItem(0).accelerator)
            assertNull(menu.getItem(2).accelerator)
            assertNull(menu.getItem(3).accelerator)
            (CmdOrCtrl + 'E').withKeyStroke { key ->
                val actionKey = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key)
                assertSame(panel.networkActions.editSelectedModelsAction, panel.actionMap.get(actionKey))
            }
            panel.selectionManager.set(texts.take(1))
            menu.menuListeners.forEach { it.menuSelected(MenuEvent(menu)) }
            assertEquals(3, menu.itemCount)
            assertEquals("1 text object...", menu.getItem(2).text)
            panel.selectionManager.clear()
            menu.menuListeners.forEach { it.menuSelected(MenuEvent(menu)) }
            assertFalse(menu.isEnabled)
            assertEquals(0, menu.itemCount)
        }
    }

    @Test
    fun `command E retains the shared menu action after type specific editors are created`() {
        SwingUtilities.invokeAndWait {
            val panel = NetworkPanel(NetworkComponent("test", Network()))
            val editAction = panel.networkActions.editSelectedModelsAction
            fun assertSharedBinding() {
                (CmdOrCtrl + 'E').withKeyStroke { key ->
                    val actionKey = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key)
                    assertSame(editAction, panel.actionMap.get(actionKey))
                }
            }
            assertSharedBinding()
            panel.createNeuronContextMenu()
            assertSharedBinding()
            panel.createSynapseContextMenu()
            assertSharedBinding()
            panel.networkActions.setNeuronPropertiesAction
            assertSharedBinding()
            panel.networkActions.setSynapsePropertiesAction
            assertSharedBinding()
        }
    }

    @Test
    fun `duplicate representations count once and excluded info text is described`() {
        val neuron = Neuron()
        val text = InfoText("status")
        val plan = SelectionEditPlan(listOf(neuron, neuron, text))
        assertEquals(listOf(listOf(neuron)), plan.groups)
        assertEquals("Edit 1 neuron...", plan.description.first)
        assertTrue(plan.description.second.contains("Not included: 1 info text object"))
    }

    @Test
    fun `text supports selection editing while info text and empty selections do not`() {
        assertNull(SelectionEditPlan(emptyList()).description.first)
        assertNull(SelectionEditPlan(listOf(InfoText("status"))).description.first)
        assertEquals("Edit 1 text object...", SelectionEditPlan(listOf(NetworkTextObject("note"))).description.first)
        assertEquals("Edit 1 neuron and 1 text object...",
            SelectionEditPlan(listOf(Neuron(), NetworkTextObject("note"))).description.first)
    }

    @Test
    fun `convolution pooling and flatten connectors get distinct groups`() {
        val source = TensorLayer(TensorShape(4, 4))
        val convolution = ConvolutionConnector(source, TensorLayer(TensorShape(4, 4)), numFilters = 1)
        val pooling = PoolingConnector(source, TensorLayer(TensorShape(2, 2)))
        val flatten = FlattenConnector(source, NeuronArray(16))
        val plan = SelectionEditPlan(listOf(convolution, pooling, flatten))
        assertEquals(3, plan.groups.size)
        assertEquals("Edit 3 models of 3 types...", plan.description.first)
        assertTrue(plan.description.second.contains("1 convolution connector, 1 pooling connector, 1 flatten connector"))
        assertTrue(plan.description.second.contains("Opens 3 dialogs"))
    }

    @Test
    fun `two types list counts and use correct plurals`() {
        val neurons = List(5) { Neuron() }
        val source = NeuronArray(2)
        val target = NeuronArray(2)
        val matrices = List(2) { WeightMatrix(source, target) }
        val plan = SelectionEditPlan(neurons + matrices)
        assertEquals("Edit 5 neurons and 2 weight matrices...", plan.description.first)
    }

    @Test
    fun `plan retains its targets when the original selection changes`() {
        val neuron = Neuron()
        val selection = mutableListOf<NetworkModel>(neuron)
        val plan = SelectionEditPlan(selection)
        selection.clear()
        selection.add(NeuronArray(4))
        assertEquals(listOf(listOf(neuron)), plan.groups)
    }

    @Test
    fun `collections do not implicitly include their children and trainers can have properties`() {
        val neuron = Neuron()
        val collection = NeuronCollection(listOf(neuron))
        assertEquals(listOf(listOf(collection)), SelectionEditPlan(listOf(collection)).groups)
        assertEquals(2, SelectionEditPlan(listOf(collection, neuron)).groups.size)
        assertTrue(Hopfield(3).supportsSelectionEditing)
    }

    @Test
    fun `tensor dialog commits to every captured target and not an unselected model`() {
        SwingUtilities.invokeAndWait {
            val panel = NetworkPanel(NetworkComponent("test", Network()))
            val first = TensorLayer(TensorShape(2, 2))
            val second = TensorLayer(TensorShape(4, 1))
            val other = TensorLayer(TensorShape(2, 2))
            val dialog = SelectionEditPlan(listOf(first, second)).createDialogs(panel).single()
            fun findEditor(container: Container): AnnotatedPropertyEditor<*>? =
                if (container is AnnotatedPropertyEditor<*>) container else
                    container.components.filterIsInstance<Container>().firstNotNullOfOrNull { findEditor(it) }
            try {
                val editor = requireNotNull(findEditor(dialog.contentPane))
                assertEquals(listOf(first, second), editor.editingObjects)
                (editor.propertyNameWidgetMap["priority"] as NumericWidget).widget.value = 5
                editor.commitChanges()
                assertEquals(5, first.priority)
                assertEquals(5, second.priority)
                assertEquals(0, other.priority)
            } finally {
                dialog.dispose()
            }
        }
    }
}
