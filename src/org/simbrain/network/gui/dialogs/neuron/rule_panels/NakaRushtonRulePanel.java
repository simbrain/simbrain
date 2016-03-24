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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.NakaRushtonRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.widgets.YesNoNull;

/**
 * <b>NakaRushtonNeuronPanel</b> edits a Naka Rushton neuron.
 */
public class NakaRushtonRulePanel extends AbstractNeuronRulePanel
        implements ActionListener {

    /** Steepness field. */
    private JTextField tfSteepness;

    /** Semi saturation field. */
    private JTextField tfSemiSaturation;

    /** Time constant field. */
    private JTextField tfTimeConstant;

    /** Use adaptation combo box. */
    private YesNoNull tsUseAdaptation;

    /** Adaptation time constant. */
    private JTextField tfAdaptationTime;

    /** Adaptation parameter. */
    private JTextField tfAdaptationParam;

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** A reference to the neuron update rule being edited. */
    private static final NakaRushtonRule prototypeRule = new NakaRushtonRule();

    /**
     * Creates a new Naka-Rushton neuron panel.
     */
    public NakaRushtonRulePanel() {
        super();

        tfSteepness = createTextField(
                (r) -> ((NakaRushtonRule) r).getSteepness(),
                (r, val) -> ((NakaRushtonRule) r).setSteepness((double) val));
        tfSemiSaturation = createTextField(
                (r) -> ((NakaRushtonRule) r).getSteepness(),
                (r, val) -> ((NakaRushtonRule) r).setSteepness((double) val));
        tfTimeConstant = createTextField(
                (r) -> ((NakaRushtonRule) r).getTimeConstant(),
                (r, val) -> ((NakaRushtonRule) r)
                        .setTimeConstant((double) val));
        tsUseAdaptation = createYesNoChoiceBox(
                (r) -> ((NakaRushtonRule) r).getUseAdaptation(),
                (r, val) -> ((NakaRushtonRule) r)
                        .setUseAdaptation((Boolean) val));
        tfAdaptationParam = createTextField(
                (r) -> ((NakaRushtonRule) r).getAdaptationParameter(),
                (r, val) -> ((NakaRushtonRule) r)
                        .setAdaptationParameter((double) val));
        tfAdaptationTime = createTextField(
                (r) -> ((NakaRushtonRule) r).getAdaptationTimeConstant(),
                (r, val) -> ((NakaRushtonRule) r)
                        .setAdaptationTimeConstant((double) val));

        tsUseAdaptation.addActionListener(this);

        this.add(tabbedPane);
        mainTab.addItem("Steepness", tfSteepness);
        mainTab.addItem("Semi-saturation constant", tfSemiSaturation);
        mainTab.addItem("Time constant", tfTimeConstant);
        mainTab.addItem("Add noise", getAddNoise());
        mainTab.addItem("Use Adaptation", tsUseAdaptation);
        mainTab.addItem("Adaptation parameter", tfAdaptationParam);
        mainTab.addItem("Adaptation time constant", tfAdaptationTime);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(getNoisePanel(), "Noise");
    }

    /**
     * Checks for using adaptation and enables or disables adaptation field
     * accordingly.
     */
    private void checkUsingAdaptation() {
        if (tsUseAdaptation.isSelected()) {
            tfAdaptationTime.setEnabled(true);
            tfAdaptationParam.setEnabled(true);
        } else {
            tfAdaptationTime.setEnabled(false);
            tfAdaptationParam.setEnabled(false);
        }
    }

    /**
     * Responds to actions performed.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == tsUseAdaptation) {
            checkUsingAdaptation();
        }
    }

    @Override
    protected final NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
