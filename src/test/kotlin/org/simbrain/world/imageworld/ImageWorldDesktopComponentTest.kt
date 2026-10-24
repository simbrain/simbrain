/**
 * The image world view redraws a changed image on a later frame rather than in an awaited handler, so a source that
 * renders a new image every iteration never waits for the EDT.
 */
package org.simbrain.world.imageworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.finishesWhileEdtIsBlocked
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.Workspace
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities

class ImageWorldDesktopComponentTest {

    @Test
    fun `an image update does not wait for the edt`() = runBlocking {
        val workspace = Workspace()
        val component = ImageWorldComponent("Image world")
        workspace.addWorkspaceComponent(component)
        SwingUtilities.invokeAndWait {
            val frame = GenericJInternalFrame("Image world", true, true, true, true)
            frame.contentPane.add(workspace.componentFactory.createGuiComponent(frame, component))
        }
        val album = component.world.imageAlbum

        assertTrue(finishesWhileEdtIsBlocked {
            repeat(5) { album.addImage(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)) }
        })
    }
}
