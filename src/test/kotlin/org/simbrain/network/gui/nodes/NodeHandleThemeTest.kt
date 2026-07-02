package org.simbrain.network.gui.nodes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.NetworkTheme
import javax.swing.UIManager

class NodeHandleThemeTest {

    private inline fun withDarkMode(dark: Boolean, block: () -> Unit) {
        val previous = UIManager.get("laf.dark")
        try {
            UIManager.put("laf.dark", dark)
            block()
        } finally {
            UIManager.put("laf.dark", previous)
        }
    }

    @Test
    fun `selection and source handle styles carry the expected roles`() {
        assertEquals(NodeHandle.Config.Role.SELECTION, NodeHandle.SELECTION_STYLE.role)
        assertEquals(NodeHandle.Config.Role.SELECTION, NodeHandle.INTERACTION_BOX_SELECTION_STYLE.role)
        assertEquals(NodeHandle.Config.Role.SOURCE, NodeHandle.SOURCE_STYLE.role)
        assertEquals(NodeHandle.Config.Role.SOURCE, NodeHandle.INTERACTION_BOX_SOURCE_STYLE.role)
    }

    @Test
    fun `handle color resolves to the themed palette color for its role in light mode`() {
        withDarkMode(false) {
            assertEquals(NetworkTheme.lightPalette.selectionHandle, NodeHandle.SELECTION_STYLE.resolveColor())
            assertEquals(NetworkTheme.lightPalette.selectionHandle, NodeHandle.INTERACTION_BOX_SELECTION_STYLE.resolveColor())
            assertEquals(NetworkTheme.lightPalette.sourceHandle, NodeHandle.SOURCE_STYLE.resolveColor())
            assertEquals(NetworkTheme.lightPalette.sourceHandle, NodeHandle.INTERACTION_BOX_SOURCE_STYLE.resolveColor())
        }
    }

    @Test
    fun `handle color resolves to the themed palette color for its role in dark mode`() {
        withDarkMode(true) {
            assertEquals(NetworkTheme.darkPalette.selectionHandle, NodeHandle.SELECTION_STYLE.resolveColor())
            assertEquals(NetworkTheme.darkPalette.sourceHandle, NodeHandle.SOURCE_STYLE.resolveColor())
        }
    }

    @Test
    fun `handle color tracks a live light-dark switch instead of being captured once`() {
        withDarkMode(false) {
            assertEquals(NetworkTheme.lightPalette.selectionHandle, NodeHandle.SELECTION_STYLE.resolveColor())
        }
        withDarkMode(true) {
            assertEquals(NetworkTheme.darkPalette.selectionHandle, NodeHandle.SELECTION_STYLE.resolveColor())
        }
        assertNotEquals(NetworkTheme.lightPalette.selectionHandle, NetworkTheme.darkPalette.selectionHandle)
    }
}
