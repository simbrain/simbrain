package org.simbrain.util.uisnapshot

import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.projection.ProjectionDesktopComponent
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.projection.DecayColoringManager
import org.simbrain.util.projection.Projector
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.random.Random

/**
 * The projection plot in its desktop frame with a spread of resting points, a few decaying recently-visited
 * points, and the hot current point, so point-versus-plot-well contrast can be checked under light and dark.
 */
class ProjectionPlotSnapshot : UiSnapshotDef {
    override val name = "projection_plot"

    override fun build(): Component {
        // Building a desktop component initializes SimbrainDesktop, which installs the user's saved theme
        // over the harness's; initialize it first and re-apply the requested theme so the render is faithful.
        val requestedTheme = if (UIManager.getBoolean("laf.dark")) "dark" else "light"
        SimbrainDesktop.workspace
        setupTheme(requestedTheme)
        val projector = Projector(5).apply {
            tolerance = 0.0
            coloringManager = DecayColoringManager().apply { stepsToBase = 6 }
        }
        val component = ProjectionComponent("Projection plot", projector)
        val random = Random(7)
        repeat(24) {
            component.addPoint(DoubleArray(5) { random.nextDouble(-20.0, 20.0) })
        }
        lateinit var host: JFrame
        lateinit var desktopComponent: ProjectionDesktopComponent
        SwingUtilities.invokeAndWait {
            val frame = GenericJInternalFrame("Projection plot of neuronarray_1 activations", true, true, true, true)
            desktopComponent = ProjectionDesktopComponent(frame, component)
            frame.contentPane.add(desktopComponent)
            frame.setBounds(16, 16, 620, 560)
            frame.isVisible = true
            val desktop = JDesktopPane().apply { preferredSize = Dimension(652, 592) }
            desktop.add(frame)
            host = JFrame().apply { contentPane = desktop }
            host.pack()
        }
        // Points reach the chart series through a coroutine; wait for them so the snapshot is not empty.
        val deadline = System.currentTimeMillis() + 5000
        while (desktopComponent.chart.xyPlot.dataset.getItemCount(0) < 24 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        return host
    }
}
