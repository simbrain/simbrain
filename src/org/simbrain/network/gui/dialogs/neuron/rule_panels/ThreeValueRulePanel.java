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
package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import javax.swing.JTextField;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.ThreeValueRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>ThreeValuedNeuronPanel</b> creates a dialog for setting preferences of
 * three valued neurons.
 */
public class ThreeValueRulePanel extends AbstractNeuronRulePanel {

    /** Threshold for this neuron. */
    private JTextField tfLowerThreshold;

    /** Upper threshold field. */
    private JTextField tfUpperThreshold;

    /** Bias for this neuron. */
    private JTextField tfBias;

    /** Lower value field. */
    private JTextField tfLowerValue;

    /** Middle value field. */
    private JTextField tfMiddleValue;

    /** Upper value field. */
    private JTextField tfUpperValue;

    /** Main tab for neuron prefernces. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** A reference to the neuron rule being edited. */
    private static final ThreeValueRule prototypeRule = new ThreeValueRule();

    //TODO: Make the panel wider. Reuse bounds?
    
    /**
     * Creates binary neuron preferences panel.
     */
    public ThreeValueRulePanel() {
        super();
        this.add(mainTab);
        tfBias = createTextField(
                (r) -> ((ThreeValueRule) r).getBias(),
                (r, val) -> ((ThreeValueRule) r).setBias((double) val));
        tfLowerThreshold = createTextField(
                (r) -> ((ThreeValueRule) r).getLowerThreshold(),
                (r, val) -> ((ThreeValueRule) r)
                        .setLowerThreshold((double) val));
        tfUpperThreshold = createTextField(
                (r) -> ((ThreeValueRule) r).getUpperThreshold(),
                (r, val) -> ((ThreeValueRule) r)
                        .setUpperThreshold((double) val));
        tfLowerValue = createTextField(
                (r) -> ((ThreeValueRule) r).getLowerValue(),
                (r, val) -> ((ThreeValueRule) r).setLowerValue((double) val));
        tfMiddleValue = createTextField(
                (r) -> ((ThreeValueRule) r).getMiddleValue(),
                (r, val) -> ((ThreeValueRule) r).setMiddleValue((double) val));
        tfUpperValue = createTextField(
                (r) -> ((ThreeValueRule) r).getUpperValue(),
                (r, val) -> ((ThreeValueRule) r).setUpperValue((double) val));
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Lower threshold", tfLowerThreshold);
        mainTab.addItem("Upper threshold", tfUpperThreshold);
        mainTab.addItem("Lower value", tfLowerValue);
        mainTab.addItem("Middle value", tfMiddleValue);
        mainTab.addItem("Upper value", tfUpperValue);
    }

    @Override
    protected final NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
