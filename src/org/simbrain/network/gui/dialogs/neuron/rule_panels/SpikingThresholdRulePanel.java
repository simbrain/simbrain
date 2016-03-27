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

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.widgets.YesNoNull;

/**
 * <b>ProbabilisticSpikingNeuronPanel</b>. TODO: Deactivated until discussion
 * about "SpikingNeuronUpdateRule".
 */
public class SpikingThresholdRulePanel extends AbstractNeuronRulePanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** A reference to the neuron rule being edited. */
    private static final SpikingThresholdRule prototypeRule = new SpikingThresholdRule();

    /**
     * Creates a new instance of the probabilistic spiking neuron panel.
     */
    public SpikingThresholdRulePanel() {
        super();
        JTextField thresholdField = createTextField(
                (r) -> ((SpikingThresholdRule) r).getThreshold(),
                (r, val) -> ((SpikingThresholdRule) r)
                        .setThreshold((double) val));
        this.add(tabbedPane);
        mainTab.addItem("Threshold", thresholdField);
        mainTab.addItem("Add noise", getAddNoise());
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(getNoisePanel(), "Noise");
    }

    @Override
    protected NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }


}
