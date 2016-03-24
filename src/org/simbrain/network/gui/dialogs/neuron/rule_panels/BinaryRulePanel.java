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

import javax.swing.BoxLayout;
import javax.swing.JTextField;

import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>BinaryNeuronPanel</b> creates a dialog for setting preferences of binary
 * neurons.
 */
public class BinaryRulePanel extends AbstractNeuronRulePanel {

    /** Main tab for neuron preferences. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** A reference to the neuron rule being edited. */
    private static final BinaryRule prototypeRule = new BinaryRule();

    /**
     * Creates binary neuron preferences panel.
     */
    public BinaryRulePanel() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JTextField biasField = createTextField(
                (r) -> ((BinaryRule) r).getBias(),
                (r, val) -> ((BinaryRule) r).setBias((double) val));
        JTextField lowerBoundField = createTextField(
                (r) -> ((BinaryRule) r).getLowerBound(),
                (r, val) -> ((BinaryRule) r).setLowerBound((double) val));

        JTextField upperBoundField = createTextField(
                (r) -> ((BinaryRule) r).getUpperBound(),
                (r, val) -> ((BinaryRule) r).setUpperBound((double) val));

        JTextField thresholdField = createTextField(
                (r) -> ((BinaryRule) r).getThreshold(),
                (r, val) -> ((BinaryRule) r).setThreshold((double) val));

        mainTab.addItem("Threshold", thresholdField);
        mainTab.addItem("On Value", upperBoundField);
        mainTab.addItem("Off Value", lowerBoundField);
        mainTab.addItem("Bias", biasField);
        mainTab.setAlignmentX(CENTER_ALIGNMENT);
        this.add(mainTab);
    }

    @Override
    protected final BinaryRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
