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

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.FeedForward
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Window
import javax.swing.*

/**
 * Panel for creating a feed-forward layered network.
 *
 * @author Jeff Yoshimi
 */
class LayeredNetworkCreationPanel(
    initialNumLayers: Int,
    private val parent: Window
) : JPanel() {

    private val mainPanel: Box = Box.createVerticalBox()

    private val layersTF = JTextField()

    /**
     * Panel containing the variable number of layer edit rows.
     */
    private val layerPanel: Box = Box.createVerticalBox()

    private val layerList: MutableList<LayerCreationPanel> = ArrayList()

    init {
        val header = Box.createHorizontalBox()
        header.alignmentX = RIGHT_ALIGNMENT
        header.add(JLabel("Number of Layers: "))
        layersTF.text = "" + initialNumLayers
        layersTF.columns = 3
        val applyButton = JButton("Change")
        applyButton.addActionListener { initPanel(layersTF.text.toInt()) }
        header.add(layersTF)
        header.add(applyButton)
        mainPanel.add(header)
        mainPanel.add(JSeparator(SwingConstants.HORIZONTAL))

        // TODO: ScrollPane

        // Add layer panel
        mainPanel.add(layerPanel)
        initPanel(layersTF.text.toInt())
        add(mainPanel)
    }

    /**
     * Re-initialize the panels, with one layer editor per row.
     */
    private fun initPanel(numLayers: Int) {
        layerPanel.removeAll()
        layerList.clear()
        for (i in numLayers downTo 1) {
            var layer: LayerCreationPanel
            if (i == 1) {
                layer = LayerCreationPanel(DEFAULT_NEURON_TYPES, "Input Layer", 5)
                layer.setComboBox("Linear")
            } else if (i == numLayers) {
                layer = LayerCreationPanel(DEFAULT_NEURON_TYPES, "Output Layer", 5)
                layer.setComboBox("Sigmoid")
            } else {
                layer = if (numLayers == 3) {
                    LayerCreationPanel(DEFAULT_NEURON_TYPES, "Hidden Layer", 5)
                } else {
                    LayerCreationPanel(
                        DEFAULT_NEURON_TYPES,
                        "Hidden Layer " + (i - 1),
                        5
                    )
                }
                layer.setComboBox("Sigmoid")
            }
            layerList.add(layer)
            layerPanel.add(layer)
            layerPanel.add(JSeparator(SwingConstants.HORIZONTAL))
        }
        parent.pack()
        parent.setLocationRelativeTo(null)
    }

    /**
     * Create the layered network.
     *
     * @param panel network panel to create the network in.
     * @param type  what type of feed forward network to create. Current options
     * are "Backprop" and "FeedForward".
     */
    fun commit(panel: NetworkPanel, type: String?) {
        // Set topology
        val topology = IntArray(layerList.size)
        var i = layerList.size - 1
        for (layer in layerList) {
            topology[i] = layer.numNeurons
            i--
        }

        // Create network
        val net = when (type) {
            "Backprop" -> BackpropNetwork(topology, panel.network.placementManager.lastClickedLocation)
            "FeedForward" -> FeedForward(topology, panel.network.placementManager.lastClickedLocation)
            else -> FeedForward(topology, panel.network.placementManager.lastClickedLocation)
        }

        // Set neuron types
        for (j in net.layerList.indices) {
            net.layerList[j].updateRule = layerList[net.layerList.size - 1 - j].neuronType!!
        }

        // Add the new network
        panel.network.addNetworkModel(net)
        panel.repaint()
        panel.undoManager.addUndoableAction(
            undo = { net.delete() },
            redo = { panel.network.addNetworkModel(net, usePlacementManager = false, useAutoAssignedId = false)?.await() }
        )
    }

    /**
     * JPanel which contains information about one layer of a layered network.
     * Displayed as: Custom label | Number of Neurons: [TextField] Neuron Type: [ComboBox]
     */
    class LayerCreationPanel(
        private val neuronTypeMap: HashMap<String?, NeuronUpdateRule<ScalarDataHolder, MatrixDataHolder>?>,
        label: String?,
        numNeurons: Int
    ) : JPanel() {

        private val numNeuronsField = JTextField()

        private val neuronTypeComboBox: JComboBox<String?>

        init {
            numNeuronsField.columns = 2
            numNeuronsField.text = "" + numNeurons

            // Set up combo box
            neuronTypeComboBox = JComboBox(neuronTypeMap.keys.toTypedArray<String?>())

            // Lay out all components horizontally
            val component = Box.createHorizontalBox()
            component.alignmentX = LEFT_ALIGNMENT
            val firstLabel = JLabel(label)
            firstLabel.preferredSize = Dimension(100, 10)
            component.add(firstLabel)
            component.add(JSeparator(SwingConstants.VERTICAL))
            component.add(Box.createHorizontalStrut(20))
            val numNeuronsLabel = JLabel("Number of neurons:")
            component.add(numNeuronsLabel)
            numNeuronsField.text = "5"
            component.add(numNeuronsField)
            component.add(Box.createHorizontalStrut(15))
            val typeLabel = JLabel("Neuron type:")
            component.add(typeLabel)
            component.add(neuronTypeComboBox)

            // Add the main horizontal box to the JPanel
            this.layout = FlowLayout(FlowLayout.LEFT)
            add(component)
        }

        constructor(label: String?, numNeurons: Int) : this(DEFAULT_NEURON_TYPES, label, numNeurons)

        val neuronType: NeuronUpdateRule<ScalarDataHolder, MatrixDataHolder>?
            get() = neuronTypeMap[neuronTypeComboBox.selectedItem]

        val numNeurons: Int
            get() = numNeuronsField.text.toInt()

        fun setComboBox(item: String?) {
            neuronTypeComboBox.selectedItem = item
        }
    }

    companion object {
        /**
         * Maps string values to corresponding NeuronUpdateRules for the combo-boxes
         * governing desired Neuron type for a given layer.
         */
        var DEFAULT_NEURON_TYPES: HashMap<String?, NeuronUpdateRule<*, *>?> = HashMap()

        /**
         * Default mapping of Strings to NeuronUpdateRules.
         */
        init {
            DEFAULT_NEURON_TYPES["Linear"] = LinearRule()
            val sig0 = SigmoidalRule()
            // sig0.setSquashFunctionType(SquashingFunctionEnum.LOGISTIC);
            DEFAULT_NEURON_TYPES["Sigmoid"] = sig0
            val sig1 = SigmoidalRule()
            // sig1.setSquashFunctionType(SquashingFunctionEnum.LOGISTIC);
            DEFAULT_NEURON_TYPES["Sigmoid"] = sig1
        }
    }
}
