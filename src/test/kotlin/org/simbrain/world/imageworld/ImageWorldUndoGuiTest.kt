/**
 * Undo and redo through the assembled image world window: mouse events on the panel a user draws on, and the same
 * actions the menu and toolbar show.
 */
package org.simbrain.world.imageworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.CmdOrCtrl
import org.simbrain.util.Shift
import org.simbrain.util.awaitUntil
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.onEdt
import org.simbrain.workspace.Workspace
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.AbstractButton
import javax.swing.JComponent
import javax.swing.JMenu
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

class ImageWorldUndoGuiTest {

    private class Window(val component: ImageWorldComponent, val frame: GenericJInternalFrame) {
        val album get() = component.world.imageAlbum
        val panel: ImageWorldPanel = frame.contentPane.descendants().filterIsInstance<ImageWorldPanel>().single()

        fun send(id: Int, x: Int, y: Int, button: Int = MouseEvent.BUTTON1) {
            val mask = if (button == MouseEvent.BUTTON3) InputEvent.BUTTON3_DOWN_MASK else InputEvent.BUTTON1_DOWN_MASK
            val reportedButton = if (id == MouseEvent.MOUSE_DRAGGED) MouseEvent.NOBUTTON else button
            SwingUtilities.invokeAndWait {
                panel.dispatchEvent(MouseEvent(panel, id, 0L, mask, x, y, 1, false, reportedButton))
            }
        }

        fun stroke(fromX: Int, toX: Int, y: Int) {
            send(MouseEvent.MOUSE_PRESSED, fromX, y)
            (fromX + 1..toX).forEach { send(MouseEvent.MOUSE_DRAGGED, it, y) }
            send(MouseEvent.MOUSE_RELEASED, toX, y)
        }

        fun click(action: javax.swing.Action) =
            SwingUtilities.invokeAndWait { action.actionPerformed(ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "")) }
    }

    private fun openWindow(): Window = runBlocking {
        val workspace = Workspace()
        val component = ImageWorldComponent("Image world")
        workspace.addWorkspaceComponent(component)
        component.world.resetImageAlbum(10, 10)
        component.world.penColor = Color.RED
        lateinit var window: Window
        SwingUtilities.invokeAndWait {
            val frame = GenericJInternalFrame("Image world", true, true, true, true)
            frame.contentPane.add(workspace.componentFactory.createGuiComponent(frame, component))
            window = Window(component, frame)
            window.panel.setSize(10, 10)
        }
        window
    }

    @Test
    fun `the window shows the panel that owns its menu actions`() {
        val window = openWindow()
        val editMenu = (0 until window.frame.jMenuBar.menuCount).map { window.frame.jMenuBar.getMenu(it) as JMenu }
            .single { it.text == "Edit" }
        val undoItem = editMenu.menuComponents.filterIsInstance<AbstractButton>().single { it.text == "Undo" }
        assertSame(window.panel.undoAction, undoItem.action)
    }

    @Test
    fun `the undo keys are bound on the panel in the window`() {
        val window = openWindow()
        val panel = window.panel
        fun actionFor(keyStroke: KeyStroke) =
            panel.actionMap[panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)[keyStroke]]
        val cmdOrCtrl = CmdOrCtrl.keyCode
        assertSame(panel.undoAction, actionFor(KeyStroke.getKeyStroke('Z'.code, cmdOrCtrl)))
        assertSame(panel.redoAction, actionFor(KeyStroke.getKeyStroke('Y'.code, cmdOrCtrl)))
        assertSame(panel.redoAction, actionFor(KeyStroke.getKeyStroke('Z'.code, cmdOrCtrl or Shift.keyCode)))
    }

    @Test
    fun `a brush stroke is one undo step`() {
        val window = openWindow()
        window.stroke(2, 6, 5)
        assertEquals(Color.RED.rgb, window.album.currentImage.getRGB(4, 5))
        awaitUntil { onEdt { window.panel.undoAction.isEnabled } }

        window.click(window.panel.undoAction)

        awaitUntil { onEdt { window.panel.redoAction.isEnabled } }
        (2..6).forEach { assertEquals(Color.BLACK.rgb, window.album.currentImage.getRGB(it, 5)) }
        assertFalse(onEdt { window.panel.undoAction.isEnabled })

        window.click(window.panel.redoAction)

        awaitUntil { onEdt { window.panel.undoAction.isEnabled } }
        (2..6).forEach { assertEquals(Color.RED.rgb, window.album.currentImage.getRGB(it, 5)) }
    }

    @Test
    fun `undo and redo start out disabled`() {
        val window = openWindow()
        assertFalse(onEdt { window.panel.undoAction.isEnabled })
        assertFalse(onEdt { window.panel.redoAction.isEnabled })
    }

    @Test
    fun `a stroke drawn with the right button can be undone`() {
        val window = openWindow()
        window.send(MouseEvent.MOUSE_PRESSED, 2, 5, MouseEvent.BUTTON3)
        window.send(MouseEvent.MOUSE_DRAGGED, 3, 5, MouseEvent.BUTTON3)
        window.send(MouseEvent.MOUSE_RELEASED, 3, 5, MouseEvent.BUTTON3)
        assertEquals(Color.RED.rgb, window.album.currentImage.getRGB(3, 5))

        awaitUntil { onEdt { window.panel.undoAction.isEnabled } }
        window.click(window.panel.undoAction)

        awaitUntil { window.album.currentImage.getRGB(3, 5) == Color.BLACK.rgb }
    }

    @Test
    fun `undoing mid stroke includes the stroke so far`() {
        val window = openWindow()
        window.send(MouseEvent.MOUSE_PRESSED, 2, 5)
        window.send(MouseEvent.MOUSE_DRAGGED, 3, 5)

        window.click(window.panel.undoAction)

        awaitUntil { window.album.currentImage.getRGB(3, 5) == Color.BLACK.rgb }
    }

    @Test
    fun `the toolbar fill can be undone without being asked first`() {
        val window = openWindow()
        val fill = onEdt {
            window.frame.contentPane.descendants().filterIsInstance<AbstractButton>()
                .single { it.action?.getValue(javax.swing.Action.NAME) == "Fill" }
        }

        // A confirmation dialog would block here on the event thread
        SwingUtilities.invokeAndWait { fill.doClick() }
        awaitUntil { window.album.currentImage.getRGB(0, 0) == Color.RED.rgb }

        awaitUntil { onEdt { window.panel.undoAction.isEnabled } }
        window.click(window.panel.undoAction)
        awaitUntil { window.album.currentImage.getRGB(0, 0) == Color.BLACK.rgb }
    }

    @Test
    fun `a snapshot can be undone`() {
        val window = openWindow()
        window.click(window.panel.takeSnapshotAction)
        awaitUntil { window.album.numFrames == 2 }

        awaitUntil { onEdt { window.panel.undoAction.isEnabled } }
        window.click(window.panel.undoAction)

        awaitUntil { window.album.numFrames == 1 }
    }

    @Test
    fun `a coupling write disables undo`() {
        val window = openWindow()
        window.stroke(2, 6, 5)
        awaitUntil { onEdt { window.panel.undoAction.isEnabled } }

        window.album.setBrightness(DoubleArray(100) { 0.5 })

        awaitUntil { !onEdt { window.panel.undoAction.isEnabled } }
    }

    private companion object {
        fun Component.descendants(): List<Component> =
            listOf(this) + ((this as? Container)?.components?.flatMap { it.descendants() } ?: listOf())
    }
}
