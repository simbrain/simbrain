/**
 * Checks that building the image world desktop GUI does not itself change which pipeline is current.
 * The pipeline combo box is populated programmatically, and its action listener is the same one that
 * user selections go through, so population must not be mistaken for a selection.
 */
package org.simbrain.world.imageworld

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.Workspace
import javax.swing.SwingUtilities

class ImagePipelineCollectionGuiTest {

    @Test
    fun `building the gui leaves a non default pipeline current`() {
        repeat(10) { attempt ->
            val workspace = Workspace()
            val component = ImageWorldComponent("Image world")
            workspace.addWorkspaceComponent(component)
            val pipelines = component.world.imagePipelineCollection
            val target = pipelines.pipelines[3]
            runBlocking { pipelines.setCurrentPipeline(target) }

            SwingUtilities.invokeAndWait {
                val frame = GenericJInternalFrame("Image world", true, true, true, true)
                frame.contentPane.add(workspace.componentFactory.createGuiComponent(frame, component))
            }

            // The combo box listener dispatches to Dispatchers.Default, so let any such work land.
            Thread.sleep(300)
            SwingUtilities.invokeAndWait { }

            assertSame(
                target, pipelines.currentPipeline,
                "attempt $attempt: constructing the GUI must not change the current pipeline"
            )
        }
    }
}
