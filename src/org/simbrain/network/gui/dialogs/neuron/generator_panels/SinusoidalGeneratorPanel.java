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
package org.simbrain.network.gui.dialogs.neuron.generator_panels;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.activity_generators.SinusoidalRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>SinusoidalGeneratorPanel</b> edits a sinusoidal actiity generator.
 */
public class SinusoidalGeneratorPanel extends AbstractNeuronRulePanel {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Tabbed panel. */
    private JTabbedPane tabbedPanel = new JTabbedPane();

    /** A reference to the neuron rule being edited. */
    private SinusoidalRule prototypeRule = new SinusoidalRule();

    /**
     * Creates an instance of this panel.
     *
     */
    public SinusoidalGeneratorPanel() {
        super();
        this.add(tabbedPanel);
        JTextField tfPhase = createTextField(
                (r) -> ((SinusoidalRule) r).getPhase(),
                (r, val) -> ((SinusoidalRule) r).setPhase((double) val));
        JTextField tfFrequency = createTextField(
                (r) -> ((SinusoidalRule) r).getFrequency(),
                (r, val) -> ((SinusoidalRule) r).setFrequency((double) val));
        mainPanel.addItem("Phase", tfPhase);
        mainPanel.addItem("Frequency", tfFrequency);
        mainPanel.addItem("Add noise", getAddNoise());
        tabbedPanel.add(mainPanel, "Main");
        tabbedPanel.add(getNoisePanel(), "Noise");
    }

    @Override
    public final NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }


}
