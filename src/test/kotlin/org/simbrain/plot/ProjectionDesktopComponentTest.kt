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
import org.simbrain.util.withEdtHeld
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

        val tasks = countEdtTasks(settleMs = 200) {
            withEdtHeld { repeat(300) { component.addPoint(DoubleArray(4) { random.nextDouble() }) } }
        }
        UiWork.awaitIdle()

        // Normally about 10. The count covers the whole JVM, so unrelated work can land in the window; posting per
        // point would cost at least 300.
        assertTrue(tasks < 100, "300 points posted $tasks EDT tasks")
        assertEquals(component.projector.dataset.kdTree.size, desktop.pointList.size)
    }
}
