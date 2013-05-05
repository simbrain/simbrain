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
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.util.RandomSource;
import org.simbrain.util.LabelledItemPanel;

/**
 * Panel for editing collections of synapses.
 *
 * @author Jeff Yoshimi
 *
 */
public class SynapseAdjustmentPanel extends JPanel {

    // TODO: Use user preferences?

    /** Random source for randomizing. */
    private RandomSource randomizer = new RandomSource();

    /** Random source for perturbing. */
    private RandomSource perturber = new RandomSource();

    /**
     * Construct the synapse editor panel.
     *
     * @param networkPanel reference to parent
     */
    public SynapseAdjustmentPanel(final NetworkPanel networkPanel) {

        // General setup
        setLayout(new BorderLayout());

        // Initialize random panels
        final RandomPanel randomPanel = new RandomPanel(false);
        randomPanel.fillFieldValues(randomizer);
        final RandomPanel perturberPanel = new RandomPanel(false);
        perturberPanel.fillFieldValues(perturber);

        // TODO: Checkbox for excitatory or inhibitory only.
        // Useful? If so, for all?

        // Stats Panel
        // TODO:Move below to separate methods and initialize so it's filled
        // when opened
        JPanel statsPanel = new JPanel(new GridLayout(3, 1));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Synapse Stats"));
        final JLabel meanValue = new JLabel("Overall Mean:");
        final JLabel excitatoryValue = new JLabel("Excitatory Mean:");
        final JLabel inhibitoryValue = new JLabel("Inhibitory Mean:");
        statsPanel.add(meanValue);
        statsPanel.add(excitatoryValue);
        statsPanel.add(inhibitoryValue);
        add("North", statsPanel);
        networkPanel.getNetwork().addNetworkListener(new NetworkListener() {

            @Override
            public void networkChanged() {

                // Mean
                double overall = 0, positive = 0, negative = 0;
                int overallCount = 0, positiveCount = 0, negativeCount = 0;
                for (Synapse synapse : networkPanel.getSelectedModelSynapses()) {
                    overall += synapse.getStrength();
                    overallCount++;
                    if (synapse.getStrength() > 0) {
                        positive += synapse.getStrength();
                        positiveCount++;
                    } else if (synapse.getStrength() < 0) {
                        negative += synapse.getStrength();
                        negativeCount++;
                    }
                }
                meanValue.setText("Overall Mean: " + overall / overallCount);
                excitatoryValue.setText("Excitatory Mean: " + positive
                        / positiveCount);
                inhibitoryValue.setText("Inhibitory Mean: " + negative
                        / negativeCount);

            }

            @Override
            public void neuronClampToggled() {
            }

            @Override
            public void synapseClampToggled() {
            }

        });

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

        JTabbedPane bottomPanel = new JTabbedPane();
        bottomPanel.addTab("Randomizer", randomPanel);
        bottomPanel.addTab("Perturber", perturberPanel);

        this.add("South", bottomPanel);

    }

}
