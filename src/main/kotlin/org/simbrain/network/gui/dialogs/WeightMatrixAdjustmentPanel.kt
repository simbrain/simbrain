package org.simbrain.network.gui.dialogs

import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.WeightInitializationStrategy
import org.simbrain.plot.histogram.HistogramModel
import org.simbrain.plot.histogram.HistogramPanel
import org.simbrain.util.Theme
import org.simbrain.util.createApplyPanel
import org.simbrain.util.flatten
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import javax.swing.JLabel
import javax.swing.JPanel

class WeightMatrixAdjustmentPanel(
    val weightMatrix: WeightMatrix
) : JPanel() {

    private val weights: DoubleArray
        get() = weightMatrix.weights.flatten()

    private val histogramPanel = HistogramPanel(HistogramModel())

    private val statsPanel = JPanel()

    private val meanLabel = JLabel()
    private val medianLabel = JLabel()
    private val sdLabel = JLabel()
    private val shapeLabel = JLabel()
    private val numExcLabel = JLabel()
    private val numInhLabel = JLabel()

    private val initializationStrategy = objectWrapper("Weight Initialization", org.simbrain.network.trainers.Randomize() as WeightInitializationStrategy)
    private val strategyEditor = AnnotatedPropertyEditor(initializationStrategy)
    private val strategyPanel = strategyEditor.createApplyPanel {
        commitChanges()
        initializationStrategy.editingObject.initializeWeights(weightMatrix)
        updateDisplay()
    }

    init {
        histogramPanel.setxAxisName("Weight Value")
        histogramPanel.setyAxisName("# of Weights")

        layout = GridBagLayout()
        val statsInfoPanel = JPanel().apply {
            border = Theme.sectionBorder("Weight Matrix Stats")
            layout = GridLayout(3, 2)
            add(shapeLabel)
            add(meanLabel)
            add(numExcLabel)
            add(medianLabel)
            add(numInhLabel)
            add(sdLabel)
        }
        val gbc = GridBagConstraints().apply {
            weightx = 1.0
            weighty = 0.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(5, 5, 5, 5)
            anchor = GridBagConstraints.NORTHWEST
            gridx = 0
            gridy = 0
            gridwidth = HistogramPanel.GRID_WIDTH
            gridheight = 1
        }
        this.add(statsInfoPanel, gbc)
        gbc.apply {
            weighty = 1.0
            fill = GridBagConstraints.BOTH
            anchor = GridBagConstraints.NORTHWEST
            gridwidth = HistogramPanel.GRID_WIDTH
            gridheight = HistogramPanel.GRID_HEIGHT
            gridy = 1
            gridx = 0
        }
        this.add(histogramPanel, gbc)

        gbc.apply {
            weighty = 0.0
            fill = GridBagConstraints.HORIZONTAL
            gridwidth = HistogramPanel.GRID_WIDTH
            gridheight = 1
            gridy = 1 + HistogramPanel.GRID_HEIGHT
        }
        this.add(strategyPanel, gbc)

        updateDisplay()
    }

    private fun updateDisplay() {
        updateHistogram()
        updateStats()
        if (parent != null) {
            parent.revalidate()
            parent.repaint()
        }
    }

    private fun updateHistogram() {
        val data = mutableListOf<DoubleArray>()
        val names = mutableListOf<String>()

        val positiveWeights = weights.filter { it >= 0 }.toDoubleArray()
        val negativeWeights = weights.filter { it < 0 }.toDoubleArray()

        if (positiveWeights.isNotEmpty()) {
            data.add(positiveWeights)
            names.add("Positive")
        }
        if (negativeWeights.isNotEmpty()) {
            data.add(negativeWeights)
            names.add("Negative")
        }

        histogramPanel.model.resetData(data, names)
        histogramPanel.model.setSeriesColor("Positive", HistogramPanel.getDefault_Pallet()[0])
        histogramPanel.model.setSeriesColor("Negative", HistogramPanel.getDefault_Pallet()[1])
        histogramPanel.reRender()
    }

    private fun updateStats() {
        val allWeights = weights
        val excitatoryCount = allWeights.count { it > 0 }
        val inhibitoryCount = allWeights.count { it < 0 }

        val mean = if (allWeights.isNotEmpty()) allWeights.average() else 0.0
        val median = if (allWeights.isNotEmpty()) {
            val sorted = allWeights.sorted()
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2] + sorted[sorted.size / 2 - 1]) / 2
            } else {
                sorted[sorted.size / 2]
            }
        } else 0.0
        val stdDev = if (allWeights.isNotEmpty()) {
            val variance = allWeights.map { (it - mean) * (it - mean) }.average()
            Math.sqrt(variance)
        } else 0.0

        shapeLabel.text = "Shape: ${weightMatrix.sizeString}"
        meanLabel.text = "Mean: ${SimbrainMath.roundDouble(mean, 5)}"
        medianLabel.text = "Median: ${SimbrainMath.roundDouble(median, 5)}"
        sdLabel.text = "Std. Dev: ${SimbrainMath.roundDouble(stdDev, 5)}"
        numExcLabel.text = "Excitatory: $excitatoryCount"
        numInhLabel.text = "Inhibitory: $inhibitoryCount"
        statsPanel.revalidate()
        statsPanel.repaint()
    }
}
