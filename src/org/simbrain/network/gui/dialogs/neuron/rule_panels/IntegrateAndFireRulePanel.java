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

import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>IntegrateAndFireNeuronPanel</b> edits an integrate and fire neuron.
 */
public class IntegrateAndFireRulePanel extends AbstractNeuronRulePanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Time constant field. */
    private JTextField tfTimeConstant;

    /** Threshold field. */
    private JTextField tfThreshold;

    /** Reset field. */
    private JTextField tfReset;

    /** Resistance field. */
    private JTextField tfResistance;

    /** Resting potential field. */
    private JTextField tfRestingPotential;

    /** Background current field. */
    private JTextField tfBackgroundCurrent;

    /** A reference to the neuron update rule being edited. */
    private static final IntegrateAndFireRule prototypeRule = new IntegrateAndFireRule();

    /**
     * Creates a new instance of the integrate and fire neuron panel.
     */
    public IntegrateAndFireRulePanel() {
        super();
        this.add(tabbedPane);

        tfTimeConstant = createTextField(
                (r) -> ((IntegrateAndFireRule) r).getTimeConstant(),
                (r, val) -> ((IntegrateAndFireRule) r)
                        .setTimeConstant((double) val));
        tfThreshold = createTextField(
                (r) -> ((IntegrateAndFireRule) r).getThreshold(),
                (r, val) -> ((IntegrateAndFireRule) r)
                        .setThreshold((double) val));
        tfReset = createTextField(
                (r) -> ((IntegrateAndFireRule) r).getResetPotential(),
                (r, val) -> ((IntegrateAndFireRule) r)
                        .setResetPotential((double) val));
        tfResistance = createTextField(
                (r) -> ((IntegrateAndFireRule) r).getResistance(),
                (r, val) -> ((IntegrateAndFireRule) r)
                        .setResistance((double) val));
        tfRestingPotential = createTextField(
                (r) -> ((IntegrateAndFireRule) r).getRestingPotential(),
                (r, val) -> ((IntegrateAndFireRule) r)
                        .setRestingPotential((double) val));
        tfBackgroundCurrent = createTextField(
                (r) -> ((IntegrateAndFireRule) r).getBackgroundCurrent(),
                (r, val) -> ((IntegrateAndFireRule) r)
                        .setBackgroundCurrent((double) val));

        mainTab.addItem("Threshold (mV)", tfThreshold);
        mainTab.addItem("Resting potential (mV)", tfRestingPotential);
        mainTab.addItem("Reset potential (mV)", tfReset);
        mainTab.addItem("Resistance (M\u03A9)", tfResistance);
        mainTab.addItem("Background Current (nA)", tfBackgroundCurrent);
        mainTab.addItem("Time constant (ms)", tfTimeConstant);
        mainTab.addItem("Add noise", getAddNoise());
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(getNoisePanel(), "Noise");
    }

    @Override
    protected final IntegrateAndFireRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
