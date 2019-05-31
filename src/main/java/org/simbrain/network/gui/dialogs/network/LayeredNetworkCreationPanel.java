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
package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.subnetworks.FeedForward;
import org.simbrain.util.math.SquashingFunctionEnum;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Panel for creating a feed-forward layered network.
 *
 * @author Jeff Yoshimi
 */

public class LayeredNetworkCreationPanel extends JPanel {

    /**
     * Parent window.
     */
    private Window parent;

    /**
     * Main display panel.
     */
    private Box mainPanel = Box.createVerticalBox();

    /**
     * Text field for number of layers in network.
     */
    private JTextField layersTF = new JTextField();

    /**
     * Panel containing the variable number of layer edit rows.
     */
    private Box layerPanel = Box.createVerticalBox();

    /**
     * List of swing components that describe the layers of the network.
     */
    private List<LayerCreationPanel> layerList = new ArrayList<LayerCreationPanel>();

    /**
     * Maps string values to corresponding NeuronUpdateRules for the combo-boxes
     * governing desired Neuron type for a given layer.
     */
    public static HashMap<String, NeuronUpdateRule> DEFAULT_NEURON_TYPES = new HashMap<String, NeuronUpdateRule>();

    /**
     * Default mapping of Strings to NeuronUpdateRules.
     */
    static {
        DEFAULT_NEURON_TYPES.put("Linear", new LinearRule());
        SigmoidalRule sig0 = new SigmoidalRule();
        sig0.setSquashFunctionType(SquashingFunctionEnum.LOGISTIC);
        DEFAULT_NEURON_TYPES.put("Logistic", sig0);
        SigmoidalRule sig1 = new SigmoidalRule();
        sig1.setSquashFunctionType(SquashingFunctionEnum.LOGISTIC);
        DEFAULT_NEURON_TYPES.put("Logistic", sig1);
    }

    /**
     * Constructs a labeled item panel dialog for the creation of a simple
     * recurrent network.
     *
     * @param initialNumLayers the initial number of layers
     * @param parent           the parent dialog
     */
    public LayeredNetworkCreationPanel(final int initialNumLayers, final Window parent) {
        this.parent = parent;

        // Set up header
        Box header = Box.createHorizontalBox();
        header.setAlignmentX(RIGHT_ALIGNMENT);
        header.add(new JLabel("Number of Layers: "));
        layersTF.setText("" + initialNumLayers);
        layersTF.setColumns(3);
        JButton applyButton = new JButton("Change");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initPanel(Integer.parseInt(layersTF.getText()));
            }
        });
        header.add(layersTF);
        header.add(applyButton);
        mainPanel.add(header);
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        // TODO: ScrollPane

        // Add layer panel
        mainPanel.add(layerPanel);
        initPanel(Integer.parseInt(layersTF.getText()));
        add(mainPanel);

    }

    /**
     * Re-initialize the panels, with one layer editor per row.
     *
     * @param numLayers number of layers
     */
    private void initPanel(int numLayers) {
        layerPanel.removeAll();
        layerList.clear();
        for (int i = numLayers; i > 0; i--) {
            LayerCreationPanel layer;
            if (i == 1) {
                layer = new LayerCreationPanel(DEFAULT_NEURON_TYPES, "Input Layer", 5);
                layer.setComboBox("Linear");
            } else if (i == numLayers) {
                layer = new LayerCreationPanel(DEFAULT_NEURON_TYPES, "Output Layer", 5);

            } else {
                if (numLayers == 3) {
                    layer = new LayerCreationPanel(DEFAULT_NEURON_TYPES, "Hidden Layer", 5);
                } else {
                    layer = new LayerCreationPanel(DEFAULT_NEURON_TYPES, "Hidden Layer " + (i - 1), 5);
                }
                layer.setComboBox("Logistic");
            }
            layerList.add(layer);
            layerPanel.add(layer);
            layerPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        }
        parent.pack();
        parent.setLocationRelativeTo(null);
    }

    /**
     * Create the layered network.
     *
     * @param panel network panel to create the network in.
     * @param type  what type of feed forward network to create. Current options
     *              are "Backprop" and "FeedForward".
     */
    public void commit(final NetworkPanel panel, final String type) {
        // Set topology
        int[] topology = new int[layerList.size()];
        int i = layerList.size() - 1;
        for (LayerCreationPanel layer : layerList) {
            topology[i] = layer.getNumNeurons();
            i--;
        }

        // Create network
        FeedForward net;
        switch (type) {
            case "Backprop":
                net = new BackpropNetwork(panel.getNetwork(), topology, panel.getWhereToAdd());
                break;
            case "FeedForward":
                net = new FeedForward(panel.getNetwork(), topology, panel.getWhereToAdd());
                break;
            default:
                net = new FeedForward(panel.getNetwork(), topology, panel.getWhereToAdd());
                break;
        }

        // Set neuron types
        i = layerList.size() - 1;
        for (LayerCreationPanel layer : layerList) {
            net.getNeuronGroup(i).setNeuronType(layer.getNeuronType());
            i--;
        }

        // Add the new network
        panel.getNetwork().addGroup(net);
        panel.repaint();
    }

    /**
     * JPanel which contains information about one layer of a layered network.
     * Displayed as
     * <p>
     * Custom label | Number of Neurons: [TextField] Neuron Type: [ComboBox]
     * <p>
     */
    public static class LayerCreationPanel extends JPanel {

        /**
         * Number of neurons in this layer.
         */
        private final JTextField numNeuronsField;

        /**
         * Update rule for this layer.
         */
        private final JComboBox<String> neuronTypeComboBox;

        /**
         * Mapping from string descriptions of rules to neuron update rules.
         */
        private final HashMap<String, NeuronUpdateRule> neuronTypeMap;

        /**
         * Construct a layer creation panel.
         *
         * @param neuronTypeMap the neuron types to allow
         * @param label         the label for the text field
         * @param numNeurons    initial number of neurons
         */
        public LayerCreationPanel(final HashMap<String, NeuronUpdateRule> neuronTypeMap, final String label, final int numNeurons) {

            this.neuronTypeMap = neuronTypeMap;

            // Set up text field
            numNeuronsField = new JTextField();
            numNeuronsField.setColumns(2);
            numNeuronsField.setText("" + numNeurons);

            // Set up combo box
            neuronTypeComboBox = new JComboBox<String>(neuronTypeMap.keySet().toArray(new String[neuronTypeMap.size()]));

            // Lay out all components horizontally
            Box component = Box.createHorizontalBox();
            component.setAlignmentX(Box.LEFT_ALIGNMENT);
            JLabel firstLabel = new JLabel(label);
            firstLabel.setPreferredSize(new Dimension(100, 10));
            component.add(firstLabel);
            component.add(new JSeparator(SwingConstants.VERTICAL));
            component.add(Box.createHorizontalStrut(20));
            JLabel numNeuronsLabel = new JLabel("Number of neurons:");
            component.add(numNeuronsLabel);
            numNeuronsField.setText("5");
            component.add(numNeuronsField);
            component.add(Box.createHorizontalStrut(15));
            JLabel typeLabel = new JLabel("Neuron type:");
            component.add(typeLabel);
            component.add(neuronTypeComboBox);

            // Add the main horizontal box to the JPanel
            this.setLayout(new FlowLayout(FlowLayout.LEFT));
            add(component);
        }

        /**
         * Construct a layer creation panel using default neuron types.
         *
         * @param label      the label for the text field
         * @param numNeurons initial number of neurons
         */
        public LayerCreationPanel(String label, int numNeurons) {
            this(DEFAULT_NEURON_TYPES, label, numNeurons);
        }

        /**
         * Return the current selected neuron type.
         *
         * @return current neuron update rule.
         */
        public NeuronUpdateRule getNeuronType() {
            return neuronTypeMap.get(neuronTypeComboBox.getSelectedItem());
        }

        /**
         * Return the number of neurons currently in this text field.
         *
         * @return number of neurons current entered in the text field
         */
        public int getNumNeurons() {
            return Integer.parseInt(numNeuronsField.getText());
        }

        /**
         * Set selected item of the combo box.
         *
         * @param item the string selection to use
         */
        public void setComboBox(String item) {
            neuronTypeComboBox.setSelectedItem(item);
        }

    }

}
