package org.simbrain.util.widgets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import javax.swing.JComboBox
import javax.swing.SwingUtilities

class ModelBackedComboBoxModelTest {

    @Test
    fun `syncing items and selection does not invoke the user callback`() {
        val picks = mutableListOf<String>()
        val model = ModelBackedComboBoxModel<String> { picks.add(it) }
        SwingUtilities.invokeAndWait {
            val comboBox = JComboBox(model)
            model.sync(listOf("a", "b", "c"), "b")
            assertEquals("b", comboBox.selectedItem)
            model.sync(listOf("a", "b"), "a")
            assertEquals("a", comboBox.selectedItem)
        }
        assertEquals(emptyList<String>(), picks)
    }

    @Test
    fun `populating an empty combo box selects nothing`() {
        val picks = mutableListOf<String>()
        val model = ModelBackedComboBoxModel<String> { picks.add(it) }
        SwingUtilities.invokeAndWait {
            val comboBox = JComboBox(model)
            model.sync(listOf("a", "b"), null)
            assertNull(comboBox.selectedItem)
            assertEquals(-1, comboBox.selectedIndex)
        }
        assertEquals(emptyList<String>(), picks)
    }

    @Test
    fun `a selection made through the widget reaches the callback once`() {
        val picks = mutableListOf<String>()
        val model = ModelBackedComboBoxModel<String> { picks.add(it) }
        SwingUtilities.invokeAndWait {
            val comboBox = JComboBox(model)
            model.sync(listOf("a", "b", "c"), "a")
            comboBox.selectedIndex = 2
            assertEquals("c", comboBox.selectedItem)
            comboBox.selectedIndex = 2
        }
        assertEquals(listOf("c"), picks)
    }

    @Test
    fun `a selection that is not among the items leaves nothing selected`() {
        val model = ModelBackedComboBoxModel<String> { }
        SwingUtilities.invokeAndWait {
            val comboBox = JComboBox(model)
            model.sync(listOf("a", "b"), "z")
            assertNull(comboBox.selectedItem)
        }
    }
}
