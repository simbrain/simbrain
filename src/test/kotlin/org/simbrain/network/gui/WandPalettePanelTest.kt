/**
 * How long the wand palette's popup panel follows its palette (while it is on screen, and no longer), and what the
 * menu on each of its rows offers and leaves open behind it.
 */
package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.awaitUntil
import org.simbrain.util.onEdt
import java.awt.Component
import java.awt.Container
import java.awt.Window
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JToggleButton
import javax.swing.MenuSelectionManager
import javax.swing.SwingUtilities

class WandPalettePanelTest {

    private fun palette() = WandPalette().apply {
        addAction(AdjustValueAction.activate())
        addAction(AdjustValueAction.inhibit())
        addAction(AdjustValueAction.setValue(0.0))
    }

    /**
     * A palette popup open on a frame, the way the toolbar button shows it. Nothing done in it is saved.
     */
    private class OpenPalette(palette: WandPalette) {
        val anchor = JButton("Wand")
        val outside = JButton("Outside")
        val panel = WandPalettePanel(palette, persist = {})
        val popup = WandPalettePopup().apply { add(panel) }

        // Never the active window, so that the popups are not closed by it losing focus to another application
        val frame = JFrame().apply {
            focusableWindowState = false
            contentPane = JPanel().apply {
                add(anchor)
                add(outside)
            }
            setSize(500, 400)
            isVisible = true
        }

        init {
            popup.show(anchor, 0, anchor.height)
        }

        val kebabs get() = panel.descendants().filterIsInstance<JToggleButton>().filter { it.icon is KebabIcon }

        /**
         * The row menu now open.
         */
        val rowMenu get() = MenuSelectionManager.defaultManager().selectedPath.firstOrNull() as JPopupMenu

        fun rowMenuItem(text: String) = rowMenu.components.filterIsInstance<JMenuItem>().first { it.text == text }

        val paletteIsTheOpenMenu get() = MenuSelectionManager.defaultManager().selectedPath.toList() == listOf(popup)

        private fun Component.descendants(): List<Component> =
            listOf(this) + ((this as? Container)?.components?.flatMap { it.descendants() } ?: emptyList())
    }

    private fun withOpenPalette(palette: WandPalette, test: (OpenPalette) -> Unit) {
        val open = onEdt { OpenPalette(palette) }
        try {
            test(open)
        } finally {
            onEdt {
                MenuSelectionManager.defaultManager().clearSelectedPath()
                open.popup.isVisible = false
                open.frame.dispose()
            }
        }
    }

    /**
     * Chooses a menu item the way a click on it does: the open menus are closed, then the item acts.
     */
    private fun choose(item: JMenuItem) {
        MenuSelectionManager.defaultManager().clearSelectedPath()
        item.doClick(0)
    }

    private fun press(target: Component) {
        target.dispatchEvent(
            MouseEvent(target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 2, 2, 1, false, MouseEvent.BUTTON1)
        )
    }

    @Test
    fun `a row's menu offers edit and delete over the open palette`() = withOpenPalette(palette()) { open ->
        onEdt { open.kebabs[1].doClick(0) }

        onEdt {
            assertEquals(listOf("Edit...", "Delete"), open.rowMenu.components.filterIsInstance<JMenuItem>().map { it.text })
            assertTrue(open.rowMenu.isVisible)
            assertTrue(open.popup.isVisible)
            assertTrue(open.kebabs[1].isSelected)
        }
    }

    @Test
    fun `a row has no delete button of its own`() = withOpenPalette(palette()) { open ->
        onEdt {
            val rowButtons = open.kebabs.map { kebab -> kebab.parent.components.filterIsInstance<JButton>().map { it.text } }
            assertTrue(rowButtons.all { texts -> "×" !in texts }, "Row buttons: $rowButtons")
        }
    }

    @Test
    fun `deleting from a row's menu removes that action and leaves the palette open`() {
        val palette = palette()
        val deleted = palette.actions[1]
        withOpenPalette(palette) { open ->
            onEdt { open.kebabs[1].doClick(0) }
            onEdt { choose(open.rowMenuItem("Delete")) }

            awaitUntil { onEdt { open.paletteIsTheOpenMenu && open.kebabs.size == 2 } }
            onEdt {
                assertFalse(deleted in palette.actions)
                assertEquals(2, palette.actions.size)
                assertTrue(open.popup.isVisible)
            }
        }
    }

    @Test
    fun `the last action left cannot be deleted`() {
        val palette = WandPalette().apply { addAction(AdjustValueAction.activate()) }
        withOpenPalette(palette) { open ->
            onEdt { open.kebabs.single().doClick(0) }

            onEdt {
                assertFalse(open.rowMenuItem("Delete").isEnabled)
                assertTrue(open.rowMenuItem("Edit...").isEnabled)
            }
        }
    }

    @Test
    fun `editing from a row's menu closes the palette and opens a titled dialog`() = withOpenPalette(palette()) { open ->
        onEdt { open.kebabs[0].doClick(0) }
        // The dialog is modal, so choosing the item does not return until it closes
        SwingUtilities.invokeLater { choose(open.rowMenuItem("Edit...")) }

        fun dialog() = Window.getWindows().filterIsInstance<JDialog>().firstOrNull { it.isVisible && it.title == "Edit wand action" }
        try {
            awaitUntil(message = "No dialog titled Edit wand action. Dialogs showing: " +
                    Window.getWindows().filterIsInstance<JDialog>().filter { it.isVisible }.map { it.title }) {
                onEdt { dialog() != null }
            }
            onEdt { assertFalse(open.popup.isVisible) }
        } finally {
            onEdt { Window.getWindows().filterIsInstance<JDialog>().filter { it.isVisible }.forEach { it.dispose() } }
        }
    }

    @Test
    fun `a press elsewhere in the palette closes a row's menu and not the palette`() = withOpenPalette(palette()) { open ->
        onEdt { open.kebabs[1].doClick(0) }
        val rowMenu = onEdt { open.rowMenu }

        onEdt { press(open.panel) }

        awaitUntil { onEdt { open.paletteIsTheOpenMenu } }
        onEdt {
            assertFalse(rowMenu.isVisible)
            assertTrue(open.popup.isVisible)
            assertFalse(open.kebabs[1].isSelected)
        }
    }

    @Test
    fun `a second click on the kebab button closes its menu and not the palette`() = withOpenPalette(palette()) { open ->
        onEdt { open.kebabs[1].doClick(0) }
        val rowMenu = onEdt { open.rowMenu }

        onEdt {
            press(open.kebabs[1])
            assertTrue(rowMenu.isVisible, "The press is left to the button")
            open.kebabs[1].doClick(0)
        }

        awaitUntil { onEdt { open.paletteIsTheOpenMenu } }
        onEdt {
            assertFalse(rowMenu.isVisible)
            assertTrue(open.popup.isVisible)
        }
    }

    @Test
    fun `escape closes a row's menu and not the palette`() = withOpenPalette(palette()) { open ->
        onEdt { open.kebabs[1].doClick(0) }
        val rowMenu = onEdt { open.rowMenu }

        onEdt {
            open.outside.dispatchEvent(
                KeyEvent(open.outside, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED)
            )
        }

        awaitUntil { onEdt { open.paletteIsTheOpenMenu } }
        onEdt {
            assertFalse(rowMenu.isVisible)
            assertTrue(open.popup.isVisible)
        }
    }

    @Test
    fun `a press outside the palette closes a row's menu and the palette with it`() = withOpenPalette(palette()) { open ->
        onEdt { open.kebabs[1].doClick(0) }
        val rowMenu = onEdt { open.rowMenu }

        onEdt { press(open.outside) }

        awaitUntil { onEdt { !open.popup.isVisible } }
        onEdt {
            assertFalse(rowMenu.isVisible)
            assertTrue(MenuSelectionManager.defaultManager().selectedPath.isEmpty())
        }
    }

    @Test
    fun `opening another row's menu closes the first and keeps the palette open`() = withOpenPalette(palette()) { open ->
        onEdt { open.kebabs[0].doClick(0) }
        val firstMenu = onEdt { open.rowMenu }

        onEdt {
            press(open.kebabs[2])
            open.kebabs[2].doClick(0)
        }

        onEdt {
            assertFalse(firstMenu.isVisible)
            assertTrue(open.rowMenu.isVisible)
            assertTrue(open.rowMenu !== firstMenu)
        }
        // The first menu's closing has had its turn by the time the palette could have been closed by it
        onEdt { }
        onEdt {
            assertTrue(open.popup.isVisible)
            assertTrue(open.rowMenu.isVisible)
            assertTrue(open.kebabs[2].isSelected)
        }
    }

    @Test
    fun `a panel on screen is told when the selected action changes`() {
        val palette = palette()
        val selectionChanges = AtomicInteger()
        onEdt { JDialog().apply { contentPane = WandPalettePanel(palette) { selectionChanges.incrementAndGet() }; pack() } }

        palette.selectAction(1)

        awaitUntil { selectionChanges.get() == 1 }
    }

    @Test
    fun `a panel taken off screen is no longer told when the selected action changes`() {
        val palette = palette()
        val closedPanelChanges = AtomicInteger()
        val openPanelChanges = AtomicInteger()
        onEdt {
            val closed = JDialog().apply {
                contentPane = WandPalettePanel(palette) { closedPanelChanges.incrementAndGet() }
                pack()
            }
            closed.dispose()
            JDialog().apply { contentPane = WandPalettePanel(palette) { openPanelChanges.incrementAndGet() }; pack() }
        }

        palette.selectAction(1)

        // The panel still on screen hearing of the change shows that the closed one had its chance to
        awaitUntil { openPanelChanges.get() == 1 }
        assertEquals(0, closedPanelChanges.get())
    }
}
