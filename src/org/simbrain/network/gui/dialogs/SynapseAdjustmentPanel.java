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
package org.simbrain.network.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.listeners.NetworkAdapter;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainMath;
import org.simbrain.util.randomizer.Randomizer;

/**
 * Panel for editing collections of synapses.
 *
 * @author Jeff Yoshimi
 */
public class SynapseAdjustmentPanel extends JPanel {

    // TODO: Use user preferences.

    /** Random source for randomizing. */
    final private Randomizer randomizer = new Randomizer();

    /** Random source for perturbing. */
    final private Randomizer perturber = new Randomizer();

    /** Display number of synapses being adjusted. */
    final private JLabel numSynapses = new JLabel();

    /** Display number of excitatory synapses being adjusted. */
    final private JLabel numExcitatory = new JLabel();

    /** Display number of inhibitory synapses being adjusted. */
    final private JLabel numInhibitory = new JLabel();

    /** Display mean value of the synapses. */
    final private JLabel meanValue = new JLabel();

    /** Display mean excitatory value of the synapses. */
    final private JLabel excitatoryValue = new JLabel();

    /** Display mean inhibitory value of the synapses. */
    final private JLabel inhibitoryValue = new JLabel();

    /**
     * Construct the synapse editor panel.
     *
     * @param networkPanel reference to parent
     */
    public SynapseAdjustmentPanel(final NetworkPanel networkPanel) {

        // General setup
        setLayout(new BorderLayout());

        // Initialize random panels
        final RandomPanelNetwork randomPanel = new RandomPanelNetwork(false);
        randomPanel.fillFieldValues(randomizer);
        final RandomPanelNetwork perturberPanel = new RandomPanelNetwork(false);
        perturberPanel.fillFieldValues(perturber);

        // TODO: Checkbox for excitatory or inhibitory only.
        // Useful? If so, for all?

        // Stats Panel
        JPanel statsPanel = new JPanel(new GridLayout(3, 2));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Synapse Stats"));
        statsPanel.add(numSynapses);
        statsPanel.add(meanValue);
        statsPanel.add(numExcitatory);
        statsPanel.add(excitatoryValue);
        statsPanel.add(numInhibitory);
        statsPanel.add(inhibitoryValue);
        add("North", statsPanel);
        networkPanel.getNetwork().addNetworkListener(new NetworkAdapter() {
            @Override
            public void networkChanged() {
                updateStatsPanel(networkPanel);
            }
        });
        updateStatsPanel(networkPanel);

        // Center Panel
        LabelledItemPanel mainPanel = new LabelledItemPanel();
        add("Center", mainPanel);

        // Randomize
        JButton randomizeButton = new JButton("Randomize");
        mainPanel.addItem("Randomize", randomizeButton);
        randomizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                randomPanel.commitRandom(randomizer);
                for (Synapse synapse : networkPanel.getSelectedModelSynapses()) {
                    synapse.setStrength(randomizer.getRandom());
                }
                networkPanel.getNetwork().fireNetworkChanged();
            }
        });

        // Perturb
        JButton perturbButton = new JButton("Perturb");
        mainPanel.addItem("Perturb", perturbButton);
        perturbButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                randomPanel.commitRandom(perturber);
                for (Synapse synapse : networkPanel.getSelectedModelSynapses()) {
                    synapse.setStrength(synapse.getStrength()
                            + perturber.getRandom());
                }
                networkPanel.getNetwork().fireNetworkChanged();
            }
        });

        // Prune
        JButton pruneButton = new JButton("Prune");
        mainPanel.addItem("Prune", pruneButton);
        final JTextField tfThreshold = new JTextField(".1");
        mainPanel.addItem("Threshold", tfThreshold);
        pruneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double threshold = Double.parseDouble(tfThreshold.getText());
                for (Synapse synapse : networkPanel.getSelectedModelSynapses()) {
                    if (Math.abs(synapse.getStrength()) < threshold) {
                        networkPanel.getNetwork().removeSynapse(synapse);
                    }
                }
                networkPanel.getNetwork().fireNetworkChanged();
            }
        });

        // Decrease / Increase
        final JTextField tfIncreaseDecrease= new JTextField(".1");
        JButton increaseButton = new JButton("Increase");
        mainPanel.addItem("Increase", increaseButton);
        increaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double amount = Double.parseDouble(tfIncreaseDecrease.getText());
                for (Synapse synapse : networkPanel.getSelectedModelSynapses()) {
                    synapse.setStrength(synapse.getStrength()
                            + synapse.getStrength() * amount);
                }
                networkPanel.getNetwork().fireNetworkChanged();
            }
        });
        JButton decreaseButton = new JButton("Decrease");
        mainPanel.addItem("Decrease", decreaseButton);
        decreaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double amount = Double.parseDouble(tfIncreaseDecrease.getText());
                for (Synapse synapse : networkPanel.getSelectedModelSynapses()) {
                    synapse.setStrength(synapse.getStrength()
                            - synapse.getStrength() * amount);
                }
                networkPanel.getNetwork().fireNetworkChanged();
            }
        });
        mainPanel.addItem("Increase / Decrease percent", tfIncreaseDecrease);

        JTabbedPane bottomPanel = new JTabbedPane();
        bottomPanel.addTab("Randomizer", randomPanel);
        bottomPanel.addTab("Perturber", perturberPanel);

        this.add("South", bottomPanel);

    }

    /**
     * Update the synapse statistics panel.
     *
     * @param networkPanel reference to the network panel whose selected
     *            synapses are being adjusted.
     */
    private void updateStatsPanel(NetworkPanel networkPanel) {

        // Mean
        double overall = 0, positive = 0, negative = 0;
        int positiveCount = 0, negativeCount = 0;
        for (Synapse synapse : networkPanel.getSelectedModelSynapses()) {
            overall += synapse.getStrength();
            if (synapse.getStrength() > 0) {
                positive += synapse.getStrength();
                positiveCount++;
            } else if (synapse.getStrength() < 0) {
                negative += synapse.getStrength();
                negativeCount++;
            }
        }
        numSynapses.setText("Synapses: " + (positiveCount + negativeCount));
        numExcitatory.setText("Excitatory: " + positiveCount);
        numInhibitory.setText("Inhibitory: " + negativeCount);
        meanValue.setText("Overall Mean: "
                + SimbrainMath.roundDouble(overall
                        / (positiveCount + negativeCount), 4));
        excitatoryValue.setText("Excitatory Mean: "
                + SimbrainMath.roundDouble(positive / positiveCount, 4));
        inhibitoryValue.setText("Inhibitory Mean: "
                + SimbrainMath.roundDouble(negative / negativeCount, 4));
    }

}
