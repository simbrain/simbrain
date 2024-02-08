/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs

import org.simbrain.network.connections.RadialProbabilistic
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.plot.histogram.HistogramModel
import org.simbrain.plot.histogram.HistogramPanel
import org.simbrain.util.LabelledItemPanel
import org.simbrain.util.complement
import org.simbrain.util.displayInDialog
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformIntegerDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import java.util.*
import javax.swing.*

/**
 * Panel for editing collections of synapses. Randomizing, perturbing, scaling, and pruning.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class SynapseAdjustmentPanel(
    var synapses: List<Synapse>,
    var allRandomizer: ProbabilityDistribution = UniformRealDistribution(-1.0, 1.0),
    var excitatoryRandomizer: ProbabilityDistribution = UniformRealDistribution(0.0, 1.0),
    var inhibitoryRandomizer: ProbabilityDistribution = UniformRealDistribution(-1.0, 0.0),
    val onApply: () -> Unit = {}
) : JPanel() {

    // TODO: Some of the logical operations here could be moved to utility classes or Network.kt
    // and called from here

    /**
     * A collection of the selected synaptic weights, such that the first row
     * represents excitatory weights and the 2nd row represents inhibitory
     * weights. All inhibitory weights are stored as their absolute value. Note
     * that this array is only used internally, to display stats and the
     * histogram.
     */
    // TODO: Simplify by replacing with excitatoryArray and inhibitoryArray.
    private val weights = arrayOf(doubleArrayOf(), doubleArrayOf())

    private val allPanel = AnnotatedPropertyEditor(objectWrapper("All Randomizer", allRandomizer))
    private val excitatoryPanel = AnnotatedPropertyEditor(objectWrapper("Excitatory Randomizer", excitatoryRandomizer))
    private val inhibitoryPanel = AnnotatedPropertyEditor(objectWrapper("Inhibitory Randomizer", inhibitoryRandomizer))

    private var chooseRandomizerPanel = JPanel()
    private val randomizeButton = JButton("Apply")

    private val perturber: ProbabilityDistribution = UniformRealDistribution(-0.1, .01)
    private val perturberPanel = AnnotatedPropertyEditor(objectWrapper("Perturber", perturber))
    private val perturbButton = JButton("Apply")

    /**
     * A combo box for selecting which kind of synapses should have their stats
     * displayed and/or what kind of display.
     */
    private val synTypeSelector = JComboBox(SynapseView.values())

    /**
     * Calculates some basic statistics about the private final StatisticsBlock
     */
    var statCalculator = StatisticsBlock()

    /**
     * A histogram plotting the strength of synapses over given intervals (bins)
     * against their frequency.
     */
    private val histogramPanel = HistogramPanel(HistogramModel())

    /**
     * A panel displaying basic statistics about the synapses, including: number
     * of synapses, number of inhibitory and excitatory synapses, and mean,
     * median, and standard deviation of the strengths of selected type of
     * synapses.
     */
    private val statsPanel = JPanel()

    private val meanLabel = JLabel()
    private val medianLabel = JLabel()
    private val sdLabel = JLabel()
    private val numSynsLabel = JLabel()
    private val numExSynsLabel = JLabel()
    private val numInSynsLabel = JLabel()

    init {

        // Extract weight values in usable form by internal methods
        updateWeightArrays(synapses)

        histogramPanel.setxAxisName("Synapse Strength")
        histogramPanel.setyAxisName("# of Synapses")

        // TODO: use update
        // (inhibitoryPanel.widgets.first().component as ObjectTypeEditor).dropDown.addActionListener {
        //     inhibitoryRandomizer.probabilityDistribution.useInhibitoryParams()
        //     inhibitoryPanel.fillFieldValues()
        // }

        layout = GridBagLayout()
        val synTypePanel = JPanel().apply {
            border = BorderFactory.createTitledBorder("Synapse" + " Stats")
            layout = GridLayout(3, 2)
            add(numSynsLabel)
            add(meanLabel)
            add(numExSynsLabel)
            add(medianLabel)
            add(numInSynsLabel)
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
            gridwidth = HistogramPanel.GRID_WIDTH - 1
            gridheight = 1
        }
        this.add(synTypePanel, gbc)
        gbc.apply {
            gridwidth = 1
            anchor = GridBagConstraints.CENTER
            gridx = HistogramPanel.GRID_WIDTH - 1
        }
        this.add(synTypeSelector, gbc)
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
        gbc.gridy += HistogramPanel.GRID_HEIGHT
        gbc.gridheight = 1
        val bottomPanel = JTabbedPane()
        val randTab = JPanel()
        val perturbTab = JPanel()
        val prunerTab = JPanel()
        val scalerTab = JPanel()
        randTab.layout = GridBagLayout()
        perturbTab.layout = GridBagLayout()
        val c = GridBagConstraints().apply {
            gridwidth = 2
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 0.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER
        }
        randTab.add(chooseRandomizerPanel, c)
        // TODO: Use ObjecTypeEditor.createEditor to set label to "perturber"
        perturbTab.add(perturberPanel, c)
        scalerTab.add(SynapseScalerPanel(), c)
        prunerTab.add(PrunerPanel(), c)
        c.apply {
            gridwidth = 1
            gridx = 1
            gridy = 1
            weightx = 0.0
            weighty = 0.0
            fill = GridBagConstraints.NONE
            anchor = GridBagConstraints.NORTHEAST
            insets = Insets(5, 0, 5, 10)
        }
        randTab.add(randomizeButton, c)
        perturbTab.add(perturbButton, c)
        bottomPanel.apply {
            addTab("Randomizer", randTab)
            addTab("Perturber", perturbTab)
            addTab("Pruner", prunerTab)
            addTab("Scaler", scalerTab)
        }
        this.add(bottomPanel, gbc)

        addActionListeners()
        initRandomizerPanel()
    }

    /**
     * Adds all the action listeners to the panel. Currently includes listeners
     * for: The perturb button, randomize button, and the synapse kind selector
     * combo box.
     */
    fun addActionListeners() {
        randomizeButton.addActionListener {
            val view = synTypeSelector.selectedItem as SynapseView
            when (view) {
                SynapseView.ALL, SynapseView.OVERLAY -> {
                    allPanel.commitChanges()
                }
                SynapseView.INHIBITORY -> inhibitoryPanel.commitChanges()
                SynapseView.EXCITATORY -> excitatoryPanel.commitChanges()
            }

            synapses.filter { s -> view.synapseIsAdjustable(s) }.forEach { s ->
                when (view) {
                    SynapseView.ALL, SynapseView.OVERLAY -> s.forceSetStrength(allRandomizer.sampleDouble())
                    SynapseView.EXCITATORY -> s.forceSetStrength(excitatoryRandomizer.sampleDouble())
                    SynapseView.INHIBITORY -> s.forceSetStrength(inhibitoryRandomizer.sampleDouble())
                }
            }
            // Update the histogram, stats panel, etc
            fullUpdate()
        }
        // Show stats and histogram only for selected type(s)...
        synTypeSelector.addActionListener {
            initRandomizerPanel()
        }
        perturbButton.addActionListener {
            perturberPanel.commitChanges()
            val view = synTypeSelector.selectedItem as SynapseView
            for (synapse in synapses) {
                if (view.synapseIsAdjustable(synapse)) {
                    synapse.forceSetStrength(synapse.strength + perturber.sampleDouble())
                }
            }
            fullUpdate()
        }
    }

    private fun initRandomizerPanel() {
        fullUpdate()
        when (synTypeSelector.selectedItem as SynapseView) {
            SynapseView.ALL, SynapseView.OVERLAY -> {
                chooseRandomizerPanel.removeAll()
                chooseRandomizerPanel.add(allPanel)
            }
            SynapseView.INHIBITORY -> {
                chooseRandomizerPanel.removeAll()
                chooseRandomizerPanel.add(inhibitoryPanel)
            }
            SynapseView.EXCITATORY -> {
                chooseRandomizerPanel.removeAll()
                chooseRandomizerPanel.add(excitatoryPanel)
            }
        }
    }

    /**
     * Extracts weight values and organizes them by synapse type (inhibitory or
     * excitatory). Inhibitory values are represented by their absolute value.
     */
    private fun updateWeightArrays(synapses: List<Synapse>) {
        var exWeights = 0
        var inWeights = 0

        // Inefficient but necessary due to lack of support for collections of
        // primitive types.
        for (s in synapses) {
            val w = s.strength
            if (w > 0) {
                exWeights++
            } else {
                inWeights++
            }
        }
        weights[0] = DoubleArray(exWeights)
        weights[1] = DoubleArray(inWeights)
        exWeights = 0
        inWeights = 0
        for (s in synapses) {
            val w = s.strength
            if (w > 0) {
                weights[0][exWeights++] = w
            } else {
                weights[1][inWeights++] = w
            }
        }

    }

    /**
     * Updates weight arrays and then updates all graphics.
     */
    fun fullUpdate() {
        updateWeightArrays(synapses)
        updateHistogram()
        updateStats()
        if (parent != null) {
            parent.revalidate()
            parent.repaint()
        }
        onApply()
    }

    /**
     * Updates the histogram based on the selected synapses and selected
     * options. Can plot combined excitatory and absolute inhibitory, overlaid
     * excitatory/absolute inhibitory, only excitatory, or only inhibitory.
     * Histogram must be initialized prior to invocation. Red is used to
     * represent excitatory values, blue is used for inhibitory.
     */
    private fun updateHistogram() {
        val data: MutableList<DoubleArray> = ArrayList()
        val names: MutableList<String> = ArrayList()
        when (synTypeSelector.selectedItem as SynapseView) {
            SynapseView.ALL -> {
                // Send the histogram the excitatory and absolute inhibitory
                // synapse values as separate data series.
                val hist1 = weights[0]
                val hist2 = weights[1]
                // The names of both series
                names.add(SynapseView.EXCITATORY.toString())
                names.add(SynapseView.INHIBITORY.toString())
                data.add(hist1)
                data.add(hist2)
            }
            SynapseView.OVERLAY -> {
                // Send the histogram the excitatory and absolute inhibitory
                // synapse values as separate data series.
                val hist1 = weights[0]
                val hist2 = DoubleArray(weights[1].size)
                var i = 0
                val n = hist2.size
                while (i < n) {
                    hist2[i] = Math.abs(weights[1][i])
                    i++
                }
                // The names of both series
                names.add(SynapseView.EXCITATORY.toString())
                names.add(SynapseView.INHIBITORY.toString())
                data.add(hist1)
                data.add(hist2)
            }
            SynapseView.EXCITATORY -> {
                // Send the histogram only excitatory weights as a single series
                val hist = weights[0]
                // Name the series
                names.add(SynapseView.EXCITATORY.toString())
                data.add(hist)
            }
            SynapseView.INHIBITORY -> {
                // Send the histogram only inhibitory weights as a single series
                val hist = weights[1]
                // Name the series
                names.add(SynapseView.INHIBITORY.toString())
                data.add(hist)
            }
        }

        // Send the histogram the new data and re-draw it.
        histogramPanel.model.resetData(data, names)
        histogramPanel.model.setSeriesColor(SynapseView.ALL.toString(), HistogramPanel.getDefault_Pallet()[0])
        histogramPanel.model.setSeriesColor(SynapseView.EXCITATORY.toString(), HistogramPanel.getDefault_Pallet()[0])
        histogramPanel.model.setSeriesColor(SynapseView.INHIBITORY.toString(), HistogramPanel.getDefault_Pallet()[1])
        histogramPanel.reRender()
    }

    /**
     * Updates the values in the stats panel (number of synapses, excitatory
     * synapses, inhibitory synapses, and mean, median and standard deviation of
     * selected synapses. Extract data should be used prior to this.
     */
    fun updateStats() {
        statCalculator.calcStats()
        meanLabel.text = "Mean: " + SimbrainMath.roundDouble(statCalculator.mean, 5)
        medianLabel.text = "Median: " + SimbrainMath.roundDouble(statCalculator.median, 5)
        sdLabel.text = "Std. Dev: " + SimbrainMath.roundDouble(statCalculator.stdDev, 5)
        val tot = weights[0].size + weights[1].size
        numSynsLabel.text = "Synapses: " + Integer.toString(tot)
        numExSynsLabel.text = "Excitatory : " + Integer.toString(weights[0].size)
        numInSynsLabel.text = "Inhibitory: " + Integer.toString(weights[1].size)
        statsPanel.revalidate()
        statsPanel.repaint()
    }

    /**
     * @author Zoë
     */
    inner class StatisticsBlock {
        var mean = 0.0
            private set
        var median = 0.0
            private set
        var stdDev = 0.0
            private set

        /**
         * Gets the basic statistics: mean, median, and standard deviation of
         * the synapse weights based on which group of synapses is selected.
         *
         * @return an An array where the first element is the mean, the 2nd
         * element is the median, and the 3rd element is the standard
         * deviation.
         */
        fun calcStats() {
            var data: DoubleArray? = null
            var tot = 0
            val type = synTypeSelector.selectedItem as SynapseView
            var runningVal = 0.0
            if (weights[0].size == 0 && weights[1].size == 0) {
                return
            }

            // Determine selected type(s) and collect data accordingly...
            if (type == SynapseView.ALL) {
                tot = weights[0].size + weights[1].size
                data = DoubleArray(tot)
                var c = 0
                for (i in 0..1) {
                    var j = 0
                    val m = weights[i].size
                    while (j < m) {
                        val `val` = weights[i][j]
                        runningVal += `val`
                        data[c] = `val`
                        c++
                        j++
                    }
                }
            } else if (type == SynapseView.OVERLAY) {
                tot = weights[0].size + weights[1].size
                data = DoubleArray(tot)
                var c = 0
                for (i in 0..1) {
                    var j = 0
                    val m = weights[i].size
                    while (j < m) {
                        val `val` = Math.abs(weights[i][j])
                        runningVal += `val`
                        data[c] = `val`
                        c++
                        j++
                    }
                }
            } else if (type == SynapseView.EXCITATORY && weights[0].size != 0) {
                tot = weights[0].size
                data = DoubleArray(tot)
                for (j in 0 until tot) {
                    val `val` = Math.abs(weights[0][j])
                    runningVal += `val`
                    data[j] = `val`
                }
            } else if (type == SynapseView.INHIBITORY && weights[1].size != 0) {
                tot = weights[1].size
                data = DoubleArray(tot)
                for (j in 0 until tot) {
                    val `val` = weights[1][j]
                    runningVal += `val`
                    data[j] = `val`
                }
            }
            if (data != null) {
                mean = runningVal / tot
                Arrays.sort(data)
                median = if (tot % 2 == 0) {
                    (data[tot / 2] + data[tot / 2 - 1]) / 2
                } else {
                    data[Math.floor((tot / 2).toDouble()).toInt()]
                }
                runningVal = 0.0
                for (i in 0 until tot) {
                    runningVal += Math.pow(mean - data[i], 2.0)
                }
                runningVal /= tot
                stdDev = Math.sqrt(runningVal)
            }
        }
    }

    /**
     * Panel for scaling synapses.
     */
    inner class SynapseScalerPanel() : LabelledItemPanel() {
        /**
         * Percentage to increase or decrease indicated synapses.
         */
        private val tfIncreaseDecrease = JTextField(".1")
        private val increaseButton = JButton("Increase")
        private val decreaseButton = JButton("Decrease")

        init {
            addItem("Percent to change", tfIncreaseDecrease)
            addItem("Increase", increaseButton)
            increaseButton.addActionListener {
                val amount = tfIncreaseDecrease.text.toDouble()
                val view = synTypeSelector.selectedItem as SynapseView
                for (synapse in synapses) {
                    if (view.synapseIsAdjustable(synapse)) {
                        synapse.forceSetStrength(synapse.strength + synapse.strength * amount)
                    }
                }
                fullUpdate()
            }
            addItem("Decrease", decreaseButton)
            decreaseButton.addActionListener {
                val amount = tfIncreaseDecrease.text.toDouble()
                val view = synTypeSelector.selectedItem as SynapseView
                for (synapse in synapses) {
                    if (view.synapseIsAdjustable(synapse)) {
                        synapse.forceSetStrength(synapse.strength - synapse.strength * amount)
                    }
                }
                fullUpdate()
            }
        }
    }

    /**
     * Panel for pruning synapses. If synapse strength above absolute value of the threshold,
     * prune the synapse when the prune button is pressed.
     */
    inner class PrunerPanel() : LabelledItemPanel() {

        private val tfThreshold = JTextField(".1")

        init {
            val pruneButton = JButton("Prune")
            addItem("Prune", pruneButton)
            addItem("Threshold", tfThreshold)
            pruneButton.addActionListener {
                val threshold = tfThreshold.text.toDouble()
                val view = synTypeSelector.selectedItem as SynapseView
                val toDelete = synapses.filter { view.synapseIsAdjustable(it) }
                    .filter { Math.abs(it.strength) < threshold }

                // Update the internal panel and data
                synapses = (toDelete complement synapses).rightComp.toList()
                updateWeightArrays(synapses)
                fullUpdate()

                // Delete from the network itself
                toDelete.forEach { it.delete() }
            }
        }
    }

    enum class SynapseView {
        ALL {
            override fun toString(): String {
                return "All"
            }

            override fun synapseIsAdjustable(s: Synapse): Boolean {
                return true
            }
        },
        OVERLAY {
            override fun toString(): String {
                return "Overlay"
            }

            override fun synapseIsAdjustable(s: Synapse): Boolean {
                return true
            }
        },
        EXCITATORY {
            override fun toString(): String {
                return "Excitatory"
            }

            override fun synapseIsAdjustable(s: Synapse): Boolean {
                return s.strength >= 0
            }
        },
        INHIBITORY {
            override fun toString(): String {
                return "Inhibitory"
            }

            override fun synapseIsAdjustable(s: Synapse): Boolean {
                return s.strength < 0
            }
        };

        abstract fun synapseIsAdjustable(s: Synapse): Boolean
    }
}

fun createSynapseAdjustmentPanel(
    synapses: List<Synapse>,
    all: ProbabilityDistribution = UniformRealDistribution(-1.0, 1.0),
    excitatoryRandomizer: ProbabilityDistribution = UniformRealDistribution(0.0, 1.0),
    inhibitoryRandomizer: ProbabilityDistribution = UniformRealDistribution(-1.0, 0.0)
): SynapseAdjustmentPanel? {
    val sap = SynapseAdjustmentPanel(synapses, all, excitatoryRandomizer, inhibitoryRandomizer)
    if (synapses.isEmpty()) {
        JOptionPane.showMessageDialog(
            null, "No synapses to display", "Warning",
            JOptionPane.WARNING_MESSAGE
        );
        return null
    }
    return sap
}

fun createSynapseAdjustmentPanel(synapses: List<Synapse>): SynapseAdjustmentPanel? {
    return createSynapseAdjustmentPanel(synapses)
}

/**
 * Convenience methods to set parameters for inhibitory methods in a prob. dist
 */
fun ProbabilityDistribution.useInhibitoryParams() {
    when(this) {
        is UniformRealDistribution -> {
            ceil = 0.0
            floor = -1.0
        }
        is NormalDistribution ->   mean = -1.0
        is UniformIntegerDistribution -> {
            ceil = 0
            floor = -1
        }
    }
}

fun main() {
    val net = Network()
    val neurons = List(20) { Neuron() }
    // val neurons = mutableListOf<Neuron>() // To test empty list case
    val conn = RadialProbabilistic()
    val syns = conn.connectNeurons(neurons, neurons).also { net.addNetworkModelsAsync(it) }
    createSynapseAdjustmentPanel(syns)?.displayInDialog()
}