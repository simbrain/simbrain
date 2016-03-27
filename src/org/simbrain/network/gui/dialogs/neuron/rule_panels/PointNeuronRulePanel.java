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

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.PointNeuronRule;
import org.simbrain.network.neuron_update_rules.PointNeuronRule.OutputFunction;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>PointNeuronPanel</b> TODO:Excitatory
 * normalization was removed because it was both unused and had no accessor
 * methods in PointNeuronRule.
 * 
 * TODO: Not yet wired up in the new style (since it's not visible in the GUI right now)
 */
public class PointNeuronRulePanel extends AbstractNeuronRulePanel {

    /** Excitatory Reversal field. */
    private JTextField tfER = new JTextField();

    /** Inhibitory Reversal field. */
    private JTextField tfIR = new JTextField();

    /** Leak Reversal field. */
    private JTextField tfLR = new JTextField();

    /** Leak Conductance field. */
    private JTextField tfLC = new JTextField();

    /** Output function. */
    private JComboBox cbOutputFunction = new JComboBox(new OutputFunction[] {
            OutputFunction.DISCRETE_SPIKING, OutputFunction.LINEAR,
            OutputFunction.NOISY_RATE_CODE, OutputFunction.NONE,
            OutputFunction.RATE_CODE, });

    /** Threshold for output function. */
    private JTextField tfThreshold = new JTextField();

    /** Gain for output function. */
    private JTextField tfGain = new JTextField();

    /** Bias for excitatory inputs. */
    private JTextField tfBias = new JTextField();

    /**
     * Time averaging for excitatory inputs. TODO: Rename to "Time Constant"?
     * Inconsistency between name in the rule panel and name in the rule model
     * class.
     */
    private JTextField tfTimeAveraging = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Inputs tab. */
    private LabelledItemPanel inputsTab = new LabelledItemPanel();

    /** Output Function tab. */
    private LabelledItemPanel outputFunctionTab = new LabelledItemPanel();

    /** A reference to the neuron update rule being edited. */
    private static final PointNeuronRule prototypeRule = new PointNeuronRule();

    /**
     * Creates an instance of this panel.
     */
    public PointNeuronRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.addItem("Excitatory Reversal", tfER);
        mainTab.addItem("Inhibitory Reversal", tfIR);
        mainTab.addItem("Leak Reversal", tfLR);
        mainTab.addItem("Leak Conductance", tfLC);
        outputFunctionTab.addItem("Output Function", cbOutputFunction);
        outputFunctionTab.addItem("Threshold", tfThreshold);
        outputFunctionTab.addItem("Gain", tfGain);
        inputsTab.addItem("Net Time Constant", tfTimeAveraging);
        inputsTab.addItem("Bias", tfBias);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(inputsTab, "Inputs");
        tabbedPane.add(outputFunctionTab, "Output Function");
    }

    @Override
    protected final NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
