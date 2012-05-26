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

import javax.swing.JTextField;

import org.simbrain.network.core.Network;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.neuron_update_rules.LogisticRule;


/**
 * <b>LogisticNeuronPanel</b>.
 */
public class LogisticRulePanel extends AbstractNeuronPanel {

    /** Growth rate field. */
    private JTextField tfGrowthRate = new JTextField();

    /**
     * Creates an instance of this panel.
     */
    public LogisticRulePanel(Network network) {
        super(network);
        addItem("Growth rate", tfGrowthRate);
        this.addBottomText("<html>Note 1: This is not a sigmoidal logistic function. <p>"
                + "For that, set update rule to sigmoidal.<p> "
                + " Note 2: for chaos, try growth rates between 3.6 and 4</html>");
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        LogisticRule neuronRef = (LogisticRule) ruleList.get(0);

        tfGrowthRate.setText(Double.toString(neuronRef.getGrowthRate()));

        //Handle consistency of multiple selections
        if (!NetworkUtils.isConsistent(ruleList, LogisticRule.class, "getGrowthRate")) {
            tfGrowthRate.setText(NULL_STRING);
        }
    }

    /**
     * Populate fields with default data.
     */
    public void fillDefaultValues() {
        LogisticRule neuronRef = new LogisticRule();
        tfGrowthRate.setText(Double.toString(neuronRef.getGrowthRate()));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < ruleList.size(); i++) {
            LogisticRule neuronRef = (LogisticRule) ruleList.get(i);

            if (!tfGrowthRate.getText().equals(NULL_STRING)) {
                neuronRef.setGrowthRate(Double.parseDouble(tfGrowthRate.getText()));
            }
        }
    }
}
