/** Exercises what counts as unsaved work in a workspace, which decides when the save prompt appears. */
package org.simbrain.workspace

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.UserEdits
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.util.table.SimbrainJTable
import org.simbrain.world.odorworld.OdorWorldComponent
import java.io.File
import javax.swing.SwingUtilities

class WorkspaceChangeTrackingTest {

    private class Built(val workspace: Workspace, val network: Network)

    /**
     * Build a small network through a simulation, the way the Simulations menu does. The last model's add is not
     * awaited, as many simulations do.
     */
    private suspend fun buildSim(): Built {
        lateinit var built: Built
        newSim {
            val network = addNetworkComponent("Network").network
            network.addNetworkModel(Neuron())
            network.addNetworkModelAsync(Neuron())
            built = Built(workspace, network)
        }.run()
        return built
    }

    private fun tempZip() = File.createTempFile("change_tracking", ".zip").apply { deleteOnExit() }

    @Test
    fun `a freshly built simulation has nothing to save`() = runBlocking {
        repeat(20) {
            val workspace = buildSim().workspace
            delay(20)
            assertFalse(workspace.hasUnsavedWork())
            assertFalse(workspace.changesExist())
        }
    }

    @Test
    fun `moving a window can be saved but is not unsaved work`() = runBlocking {
        val workspace = buildSim().workspace
        workspace.markLayoutChanged()
        assertTrue(workspace.changesExist())
        assertFalse(workspace.hasUnsavedWork())
    }

    @Test
    fun `running a simulation is unsaved work`() = runBlocking {
        val workspace = buildSim().workspace
        workspace.iterateSuspend(1)
        assertTrue(workspace.hasUnsavedWork())
    }

    @Test
    fun `adding a model after the build is unsaved work`() = runBlocking {
        val built = buildSim()
        built.network.addNetworkModel(Neuron())
        assertTrue(built.workspace.hasUnsavedWork())
    }

    @Test
    fun `adding a coupling after the build is unsaved work`() = runBlocking {
        val built = buildSim()
        val source = Neuron()
        val target = Neuron()
        built.network.addNetworkModels(source, target)
        built.workspace.markClean()
        built.workspace.couplingManager.createCoupling(source, target)
        assertTrue(built.workspace.hasUnsavedWork())
    }

    @Test
    fun `adding a component after the build is unsaved work`() = runBlocking {
        val workspace = buildSim().workspace
        workspace.addWorkspaceComponent(NetworkComponent("Another"))
        assertTrue(workspace.hasUnsavedWork())
    }

    @Test
    fun `saving clears unsaved work and running afterwards brings it back`() = runBlocking {
        val workspace = buildSim().workspace
        workspace.iterateSuspend(1)
        workspace.save(tempZip(), headless = true)
        assertFalse(workspace.hasUnsavedWork())
        workspace.iterateSuspend(1)
        assertTrue(workspace.hasUnsavedWork())
    }

    @Test
    fun `an opened workspace has nothing to save even when it was saved mid run`() = runBlocking {
        val file = tempZip()
        buildSim().workspace.apply {
            iterateSuspend(3)
            save(file, headless = true)
        }
        val reopened = Workspace()
        reopened.openWorkspace(file)
        assertFalse(reopened.hasUnsavedWork())
    }

    @Test
    fun `resetting time keeps an earlier run as unsaved work`() = runBlocking {
        val workspace = buildSim().workspace
        workspace.iterateSuspend(5)
        workspace.resetTime()
        assertTrue(workspace.hasUnsavedWork())
    }

    @Test
    fun `resetting time without running is not unsaved work`() = runBlocking {
        val workspace = buildSim().workspace
        workspace.resetTime()
        assertFalse(workspace.hasUnsavedWork())
    }

    @Test
    fun `deleting a model from an opened network is unsaved work`() = runBlocking {
        val file = tempZip()
        buildSim().workspace.save(file, headless = true)
        val reopened = Workspace()
        reopened.openWorkspace(file)
        val network = reopened.componentList.filterIsInstance<NetworkComponent>().single().network
        network.freeNeurons.first().delete()
        withTimeout(2000) {
            while (!reopened.hasUnsavedWork()) delay(10)
        }
    }

    @Test
    fun `setting the odor world grid size to its current value is not an edit`() = runBlocking {
        val workspace = Workspace()
        val odorWorld = OdorWorldComponent("World")
        workspace.addWorkspaceComponent(odorWorld)
        workspace.markClean()
        odorWorld.world.gridCellSizeInTiles = odorWorld.world.gridCellSizeInTiles
        assertFalse(workspace.hasUnsavedWork())
        odorWorld.world.gridCellSizeInTiles = odorWorld.world.gridCellSizeInTiles + 1
        assertTrue(workspace.hasUnsavedWork())
    }

    @Test
    fun `a table cell edit that keeps the value is not an edit`() {
        lateinit var table: SimbrainJTable
        SwingUtilities.invokeAndWait { table = SimbrainJTable(BasicDataFrame(2, 2)) }
        val unchanged = countReportsDuring { SwingUtilities.invokeAndWait { table.setValueAt(0.0, 0, 0) } }
        val changed = countReportsDuring { SwingUtilities.invokeAndWait { table.setValueAt(5.0, 0, 0) } }
        assertTrue(unchanged == 0)
        assertTrue(changed == 1)
    }

    @Test
    fun `a cleared workspace has nothing to save`() = runBlocking {
        val workspace = buildSim().workspace
        workspace.iterateSuspend(1)
        workspace.clearWorkspace()
        assertFalse(workspace.changesExist())
    }

    class EditedObject : EditableObject {
        var value by GuiEditable(initValue = 1.0)
    }

    private fun countReportsDuring(block: () -> Unit): Int {
        var reports = 0
        val previous = UserEdits.onEdit
        UserEdits.onEdit = { reports++ }
        try {
            block()
        } finally {
            UserEdits.onEdit = previous
        }
        return reports
    }

    @Test
    fun `committing a dialog without changing anything is not an edit`() {
        val editor = AnnotatedPropertyEditor(EditedObject())
        assertTrue(countReportsDuring { editor.commitChanges() } == 0)
    }

    @Test
    fun `committing a changed value reports an edit`() {
        val edited = EditedObject()
        val editor = AnnotatedPropertyEditor(edited)
        // The widget still holds the old value, so the commit writes a different value over this one
        edited.value = 2.0
        assertTrue(countReportsDuring { editor.commitChanges() } == 1)
    }

    @Test
    fun `committing a changed preference is not an edit`() {
        val edited = EditedObject()
        val editor = AnnotatedPropertyEditor(edited)
        edited.value = 2.0
        assertTrue(countReportsDuring { editor.commitChanges(reportEdits = false) } == 0)
    }
}
