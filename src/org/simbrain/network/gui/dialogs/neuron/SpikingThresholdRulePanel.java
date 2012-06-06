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
package org.simbrain.network.gui.dialogs.neuron;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Network;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>ProbabilisticSpikingNeuronPanel</b>.
 */
public class SpikingThresholdRulePanel extends AbstractNeuronPanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Time step field. */
    private JTextField tfThreshold = new JTextField();

    /**
     * Creates a new instance of the probabilistic spiking neuron panel.
     *
     * @param net Network
     */
    public SpikingThresholdRulePanel(Network network) {
        super(network);
        addItem("Threshold", tfThreshold);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        SpikingThresholdRule neuronRef = (SpikingThresholdRule) ruleList.get(0);

        tfThreshold.setText(Double.toString(neuronRef.getThreshold()));

        // Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(ruleList, SpikingThresholdRule.class,
                "getThreshold")) {
            tfThreshold.setText(NULL_STRING);
        }
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        SpikingThresholdRule neuronRef = new SpikingThresholdRule();
        tfThreshold.setText(Double.toString(neuronRef.getThreshold()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        for (int i = 0; i < ruleList.size(); i++) {
            SpikingThresholdRule neuronRef = (SpikingThresholdRule) ruleList
                    .get(i);

            if (!tfThreshold.getText().equals(NULL_STRING)) {
                neuronRef
                        .setThreshold(Double.parseDouble(tfThreshold.getText()));
            }
        }
    }
}
