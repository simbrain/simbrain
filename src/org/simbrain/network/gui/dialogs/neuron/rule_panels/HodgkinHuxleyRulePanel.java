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

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.NoiseGeneratorPanel;
import org.simbrain.network.neuron_update_rules.HodgkinHuxleyRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.widgets.YesNoNull;

/**
 * <b>Hodgkin-Huxley Rule Panel</b> edits an H-H neurons.
 */
public class HodgkinHuxleyRulePanel extends AbstractNeuronRulePanel {

    // Todo
    private JTextField perNaChannels;

    private JTextField perKChannels;

    private JTextField getEna;

    private JTextField getEk;

    /** A reference to the neuron update rule being edited. */
    private static final HodgkinHuxleyRule prototypeRule = new HodgkinHuxleyRule();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab for neuron preferences. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /**
     * Creates HodgkinHuxley preferences panel.
     */
    public HodgkinHuxleyRulePanel() {
        super();
        this.add(tabbedPane);
        perNaChannels = createTextField(Float.class,
                (r) -> ((HodgkinHuxleyRule) r).getPerNaChannels(),
                (r, val) -> ((HodgkinHuxleyRule) r)
                        .setPerNaChannels((float) val));
        perKChannels = createTextField(Float.class,
                (r) -> ((HodgkinHuxleyRule) r).getPerKChannels(),
                (r, val) -> ((HodgkinHuxleyRule) r)
                        .setPerKChannels((float) val));
        getEna = createTextField(Float.class,
                (r) -> ((HodgkinHuxleyRule) r).getEna(),
                (r, val) -> ((HodgkinHuxleyRule) r).setEna((float) val));
        getEk = createTextField(Float.class,
                (r) -> ((HodgkinHuxleyRule) r).getEk(),
                (r, val) -> ((HodgkinHuxleyRule) r).setEk((float) val));

        mainTab.addItem("Sodium Channels", perNaChannels);
        mainTab.addItem("Potassium Channels", perKChannels);
        mainTab.addItem("Sodium Equilibrium", getEna);
        mainTab.addItem("Potassium Equilibrium", getEk);
        mainTab.addItem("Add noise", getAddNoise());
        tabbedPane.add(mainTab, "Main");

        tabbedPane.add(getNoisePanel(), "Noise");

    }

    @Override
    protected final NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
