package org.simbrain.util.uisnapshot

import org.jfree.chart.ui.Layer
import org.simbrain.custom_sims.simulations.ml.ClassifierChoice
import org.simbrain.custom_sims.simulations.ml.DecisionBoundarySettings
import org.simbrain.plot.projection.DecisionRegionAnnotation
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.projection.ProjectionDesktopComponent
import org.simbrain.util.classifierColor
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.projection.AuxDataColoringManager
import org.simbrain.util.projection.CoordinateProjection
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.projection.Projector
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * Projection plots shaded by the decision regions of each classifier in the decision boundary simulation, trained on
 * the moons dataset, so region orientation, alignment with the points, and shading contrast can be checked.
 */
class DecisionRegionSnapshot : UiSnapshotDef {
    override val name = "decision_regions"

    override fun build(): Component {
        val requestedTheme = if (UIManager.getBoolean("laf.dark")) "dark" else "light"
        SimbrainDesktop.workspace
        setupTheme(requestedTheme)
        val settings = DecisionBoundarySettings()
        val desktopComponents = mutableListOf<ProjectionDesktopComponent>()
        val desktop = JDesktopPane().apply { preferredSize = Dimension(2 * 460 + 24, 2 * 420 + 24) }
        ClassifierChoice.entries.forEachIndexed { i, choice ->
            settings.classifier = choice
            val classifier = settings.createTrainedClassifier(settings.createDataset())
            val projector = Projector(2).apply {
                tolerance = 0.0
                coloringManager = AuxDataColoringManager()
                projectionMethod = CoordinateProjection()
            }
            val component = ProjectionComponent(choice.toString(), projector)
            classifier.trainingData.inputs.zip(classifier.trainingData.targets).forEach { (input, target) ->
                component.addPoint(DataPoint(input.toDoubleArray(), aux = classifierColor(target)))
            }
            SwingUtilities.invokeAndWait {
                val frame = GenericJInternalFrame(choice.toString(), true, true, true, true)
                val desktopComponent = ProjectionDesktopComponent(frame, component)
                desktopComponent.chart.xyPlot.renderer.addAnnotation(
                    DecisionRegionAnnotation(
                        scope = SimbrainDesktop.workspace,
                        modelKey = { classifier.model },
                        colors = List(2) { classifierColor(it) }
                    ) { point -> classifier.model?.let { classifier.classIndex(it.predict(point)) } },
                    Layer.BACKGROUND
                )
                frame.contentPane.add(desktopComponent)
                frame.setBounds(8 + (i % 2) * 468, 8 + (i / 2) * 428, 460, 420)
                frame.isVisible = true
                desktop.add(frame)
                desktopComponents.add(desktopComponent)
            }
        }
        lateinit var host: JFrame
        SwingUtilities.invokeAndWait {
            host = JFrame().apply { contentPane = desktop }
            host.pack()
        }
        val deadline = System.currentTimeMillis() + 5000
        while (desktopComponents.any { it.chart.xyPlot.dataset.getItemCount(0) < 160 } &&
            System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        // Paint once so each annotation starts computing its regions, then give the background jobs time to finish.
        SwingUtilities.invokeAndWait {
            desktopComponents.forEach { it.chart.createBufferedImage(400, 360) }
        }
        Thread.sleep(2000)
        return host
    }
}
