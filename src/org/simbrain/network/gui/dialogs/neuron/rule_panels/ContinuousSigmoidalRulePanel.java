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
import org.simbrain.network.neuron_update_rules.ContinuousSigmoidalRule;

/**
 *
 * @author Zach Tosi
 *
 */
@SuppressWarnings("serial")
public class ContinuousSigmoidalRulePanel extends AbstractSigmoidalRulePanel {

    /** Time constant field. */
    private JTextField tfTimeConstant;

    /** Text field for leak constant. */
    private JTextField tfLeakConstant;

    /** A reference to the neuron rule being edited. */
    private static ContinuousSigmoidalRule prototypeRule = new ContinuousSigmoidalRule();

    /**
     * Creates the continuous sigmoidal rule panel, but does not initialize the
     * listeners responsible for altering the panel in response to the selected
     * squashing function.
     */
    public ContinuousSigmoidalRulePanel() {
        super();
        this.add(tabbedPane);
        tfTimeConstant = createTextField(
                (r) -> ((ContinuousSigmoidalRule) r).getTimeConstant(),
                (r, val) -> ((ContinuousSigmoidalRule) r)
                        .setTimeConstant((double) val));
        tfLeakConstant = createTextField(
                (r) -> ((ContinuousSigmoidalRule) r).getLeakConstant(),
                (r, val) -> ((ContinuousSigmoidalRule) r)
                        .setLeakConstant((double) val));
        mainTab.addItem("Implementation", cbImplementation);
        mainTab.addItem("Time Constant", tfTimeConstant);
        mainTab.addItem("Leak Constant", tfLeakConstant);
        mainTab.addItem("Bias", tfBias);
        mainTab.addItem("Slope", tfSlope);
        mainTab.addItem("Add Noise", getAddNoise());
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(getNoisePanel(), "Noise");
    }

    @Override
    protected NeuronUpdateRule getPrototypeRule() {
        return prototypeRule;
    }

}