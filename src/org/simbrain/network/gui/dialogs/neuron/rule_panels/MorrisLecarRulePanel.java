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
import org.simbrain.network.gui.dialogs.neuron.NoiseGeneratorPanel;
import org.simbrain.network.neuron_update_rules.DecayRule;
import org.simbrain.network.neuron_update_rules.MorrisLecarRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.widgets.YesNoNull;

/**
 * <b>MorrisLecarRulePanel</b> edits MorrisLecar neurons.
 *
 * @author Amanda Pandey <amanda.pandey@gmail.com>
 */
public class MorrisLecarRulePanel extends AbstractNeuronRulePanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** A reference to the neuron update rule being edited. */
    private static final MorrisLecarRule prototypeRule = new MorrisLecarRule();

    /**
     * Construct the panel.
     */
    public MorrisLecarRulePanel() {
        super();
        this.add(tabbedPane);

        JTextField tfCMembrane = createTextField(
                (r) -> ((MorrisLecarRule) r).getcMembrane(),
                (r, val) -> ((MorrisLecarRule) r).setcMembrane((double) val));
        JTextField tfV_M1 = createTextField(
                (r) -> ((MorrisLecarRule) r).getV_m1(),
                (r, val) -> ((MorrisLecarRule) r).setV_m1((double) val));
        JTextField tfV_M2 = createTextField(
                (r) -> ((MorrisLecarRule) r).getV_m2(),
                (r, val) -> ((MorrisLecarRule) r).setV_m2((double) val));
        JTextField tfThreshold = createTextField(
                (r) -> ((MorrisLecarRule) r).getThreshold(),
                (r, val) -> ((MorrisLecarRule) r).setThreshold((double) val));
        JTextField tfI_Bg = createTextField(
                (r) -> ((MorrisLecarRule) r).getI_bg(),
                (r, val) -> ((MorrisLecarRule) r).setI_bg((double) val));

        LabelledItemPanel cellPanel = new LabelledItemPanel();
        cellPanel.addItem("Capacitance (\u03BCF/cm\u00B2)", tfCMembrane);
        cellPanel.addItem("Voltage const. 1", tfV_M1);
        cellPanel.addItem("Voltage const. 2", tfV_M2);
        cellPanel.addItem("Threshold (mV)", tfThreshold);
        cellPanel.addItem("Background current (nA)", tfI_Bg);
        cellPanel.addItem("Add noise: ", getAddNoise());

        JTextField tfG_Ca = createTextField(
                (r) -> ((MorrisLecarRule) r).getG_Ca(),
                (r, val) -> ((MorrisLecarRule) r).setG_Ca((double) val));
        JTextField tfG_K = createTextField(
                (r) -> ((MorrisLecarRule) r).getG_K(),
                (r, val) -> ((MorrisLecarRule) r).setG_K((double) val));
        JTextField tfG_L = createTextField(
                (r) -> ((MorrisLecarRule) r).getG_L(),
                (r, val) -> ((MorrisLecarRule) r).setG_L((double) val));
        JTextField tfVRest_Ca = createTextField(
                (r) -> ((MorrisLecarRule) r).getvRest_Ca(),
                (r, val) -> ((MorrisLecarRule) r).setvRest_Ca((double) val));
        JTextField tfvRest_k = createTextField(
                (r) -> ((MorrisLecarRule) r).getvRest_k(),
                (r, val) -> ((MorrisLecarRule) r).setvRest_k((double) val));
        JTextField tfVRest_L = createTextField(
                (r) -> ((MorrisLecarRule) r).getvRest_Ca(),
                (r, val) -> ((MorrisLecarRule) r).setvRest_Ca((double) val));
        LabelledItemPanel ionPanel = new LabelledItemPanel();
        ionPanel.addItem("Ca\u00B2\u207A conductance (\u03BCS/cm\u00B2)",
                tfG_Ca);
        ionPanel.addItem("K\u207A conductance (\u03BCS/cm\u00B2)", tfG_K);
        ionPanel.addItem("Leak conductance (\u03BCS/cm\u00B2)", tfG_L);
        ionPanel.addItem("Ca\u00B2\u207A equilibrium (mV)", tfVRest_Ca);
        ionPanel.addItem("K\u207A equilibrium (mV)", tfvRest_k);
        ionPanel.addItem("Leak equilibrium (mV)", tfVRest_L);

        JTextField tfV_W1 = createTextField(
                (r) -> ((MorrisLecarRule) r).getV_w1(),
                (r, val) -> ((MorrisLecarRule) r).setV_w1((double) val));
        JTextField tfV_W2 = createTextField(
                (r) -> ((MorrisLecarRule) r).getV_w2(),
                (r, val) -> ((MorrisLecarRule) r).setV_w2((double) val));
//        JTextField tfW_K = createTextField(
//                (r) -> ((MorrisLecarRule) r).getK(),
//                (r, val) -> ((MorrisLecarRule) r).setW_K((double) val));
        JTextField tfPhi = createTextField(
                (r) -> ((MorrisLecarRule) r).getPhi(),
                (r, val) -> ((MorrisLecarRule) r).setPhi((double) val));
        LabelledItemPanel potas = new LabelledItemPanel();
        potas.addItem("K\u207A const. 1", tfV_W1);
        potas.addItem("K\u207A const. 2", tfV_W2);
//        potas.addItem("Open K\u207A channels", tfW_K);
        potas.addItem("K\u207A \u03C6", tfPhi);

        tabbedPane.add(cellPanel, "Membrane Properties");
        tabbedPane.add(ionPanel, "Ion Properties");
        tabbedPane.add(potas, "K\u207A consts.");

        tabbedPane.add(getNoisePanel(), "Noise");
    }

    @Override
    protected final NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }
}