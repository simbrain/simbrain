package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.event.ActionEvent
import javax.swing.JMenu
import javax.swing.SwingUtilities

class OdorWorldMenusTest {

    private val component = OdorWorldComponent("Odor world")
    private val desktopComponent: OdorWorldDesktopComponent
    private val panel get() = desktopComponent.worldPanel
    private val world get() = component.world

    init {
        SimbrainDesktop.workspace.clearWorkspace()
        SimbrainDesktop.workspace.addWorkspaceComponent(component)
        lateinit var created: OdorWorldDesktopComponent
        SwingUtilities.invokeAndWait {
            created = OdorWorldDesktopComponent(GenericJInternalFrame("Odor world", true, true, true, true), component)
        }
        desktopComponent = created
    }

    private fun JMenu.itemLabels() = (0 until itemCount).mapNotNull { getItem(it)?.text }

    private fun awaitOnEdt(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2000
        while (System.currentTimeMillis() < deadline) {
            var result = false
            SwingUtilities.invokeAndWait { result = condition() }
            if (result) return
            Thread.sleep(20)
        }
        SwingUtilities.invokeAndWait { assertTrue(condition()) }
    }

    @Test
    fun `menu bar follows the network layout`() {
        val menuBar = desktopComponent.parentFrame.jMenuBar
        val names = (0 until menuBar.menuCount).map { menuBar.getMenu(it).text }
        assertEquals(listOf("File", "Edit", "Insert", "View", "Help"), names)
    }

    @Test
    fun `insert menu holds the actions that add objects to the world`() {
        assertEquals(listOf("Add agent", "Add entity"), panel.insertMenu.itemLabels())
    }

    @Test
    fun `edit menu groups selection and tile map editing`() {
        val labels = panel.editMenu.itemLabels()
        assertTrue(labels.containsAll(listOf("Select all", "Delete selected entities", "Edit entity...")))
        assertTrue(labels.containsAll(listOf("Add tile...", "Fill layer...", "Choose layer", "Edit layers...", "Clear tile map...")))
        assertFalse(labels.contains("Add entity"))
    }

    @Test
    fun `view menu exposes zoom and trails`() {
        val labels = panel.viewMenu.itemLabels()
        assertEquals(listOf("Zoom in", "Zoom out", "Reset zoom", "Auto-zoom", "Show trails", "Toolbar"), labels)
    }

    @Test
    fun `manual zoom turns auto-zoom off and toggling it back on fits the world`() {
        SwingUtilities.invokeAndWait { panel.canvas.setSize(400, 300) }
        assertTrue(panel.autoZoom)
        SwingUtilities.invokeAndWait { panel.odorWorldActions.zoomInAction.actionPerformed(ActionEvent(panel, ActionEvent.ACTION_PERFORMED, null)) }
        awaitOnEdt { !panel.autoZoom }
        SwingUtilities.invokeAndWait { panel.odorWorldActions.toggleAutoZoomAction.actionPerformed(ActionEvent(panel, ActionEvent.ACTION_PERFORMED, null)) }
        awaitOnEdt { panel.autoZoom }
        awaitOnEdt {
            val view = panel.canvas.camera.viewBounds
            view.width >= world.width - 0.5 || view.height >= world.height - 0.5
        }
    }

    @Test
    fun `menus toolbar and popups share one action instance`() {
        val actions = panel.odorWorldActions
        val toolbarActions = panel.mainToolBar.components.filterIsInstance<javax.swing.AbstractButton>().map { it.action }
        assertTrue(toolbarActions.contains(actions.addAgentAction))
        assertTrue(toolbarActions.contains(actions.deleteSelectedAction))
        val insertActions = (0 until panel.insertMenu.itemCount).map { panel.insertMenu.getItem(it).action }
        assertEquals(listOf(actions.addAgentAction, actions.addEntityAction), insertActions)
        val popupActions = panel.getContextMenu().components.filterIsInstance<javax.swing.JMenuItem>().map { it.action }
        assertTrue(popupActions.contains(actions.addEntityAction))
    }

    @Test
    fun `delete and edit are enabled only with a selected entity`() {
        val actions = panel.odorWorldActions
        val entity = runBlocking { world.addEntity() }
        awaitOnEdt { actions.deleteSelectedAction.isEnabled && actions.editEntityAction.isEnabled }

        SwingUtilities.invokeAndWait { panel.clearSelection() }
        awaitOnEdt { !actions.deleteSelectedAction.isEnabled && !actions.editEntityAction.isEnabled }

        SwingUtilities.invokeAndWait { panel.selectionManager.add(panel.getEntityNode(entity)) }
        awaitOnEdt { actions.deleteSelectedAction.isEnabled && actions.editEntityAction.isEnabled }
    }

    @Test
    fun `select all is enabled only when the world has entities`() {
        val actions = panel.odorWorldActions
        world.entityList.toList().forEach { it.delete() }
        awaitOnEdt { !actions.selectAllAction.isEnabled }

        runBlocking { world.addEntity() }
        awaitOnEdt { actions.selectAllAction.isEnabled }
    }
}
