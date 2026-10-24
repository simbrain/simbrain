/**
 * The projection view redraws from the dataset once per frame rather than once per added point: the chart still shows
 * every point, and a burst of points costs the EDT a few tasks.
 */
package org.simbrain.plot

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.projection.ProjectionDesktopComponent
import org.simbrain.util.UiWork
import org.simbrain.util.countEdtTasks
import org.simbrain.util.genericframe.GenericJInternalFrame
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities
import kotlin.random.Random

class ProjectionDesktopComponentTest {

    private fun desktopFor(component: ProjectionComponent): ProjectionDesktopComponent {
        lateinit var desktop: ProjectionDesktopComponent
        SwingUtilities.invokeAndWait {
            desktop = ProjectionDesktopComponent(GenericJInternalFrame("Projection", true, true, true, true), component)
        }
        return desktop
    }

    @Test
    fun `the chart shows every added point after the redraw`() = runBlocking {
        val component = ProjectionComponent("Projection")
        val desktop = desktopFor(component)
        val random = Random(1)

        repeat(50) { component.addPoint(DoubleArray(4) { random.nextDouble() }) }
        UiWork.awaitIdle()

        assertEquals(component.projector.dataset.kdTree.size, desktop.pointList.size)
    }

    @Test
    fun `a burst of points costs a few edt tasks, not several each`() = runBlocking {
        val component = ProjectionComponent("Projection")
        val desktop = desktopFor(component)
        UiWork.awaitIdle()
        val random = Random(2)

        val tasks = countEdtTasks {
            val release = CountDownLatch(1)
            val held = CountDownLatch(1)
            SwingUtilities.invokeLater {
                held.countDown()
                release.await()
            }
            held.await()
            repeat(300) { component.addPoint(DoubleArray(4) { random.nextDouble() }) }
            release.countDown()
            // Sleep rather than poll, so only the work the points caused is counted
            Thread.sleep(200)
        }
        UiWork.awaitIdle()

        assertTrue(tasks < 10, "300 points posted $tasks EDT tasks")
        assertEquals(component.projector.dataset.kdTree.size, desktop.pointList.size)
    }
}
