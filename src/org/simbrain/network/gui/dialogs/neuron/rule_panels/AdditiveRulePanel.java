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
import org.simbrain.network.neuron_update_rules.AdditiveRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.randomizer.gui.RandomizerPanel;
import org.simbrain.util.widgets.YesNoNull;

/**
 * <b>AdditiveNeuronPanel</b>. TODO: No implementation... why?
 */
public class AdditiveRulePanel extends AbstractNeuronRulePanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private RandomizerPanel randTab = new RandomizerPanel();

    /** Add noise combo box. */
    private YesNoNull isAddNoise = new YesNoNull();

    /** A reference to the neuron update rule being edited. */
    private static final AdditiveRule prototypeRule = new AdditiveRule();

    /**
     * Creates an instance of this panel.
     */
    public AdditiveRulePanel() {
        super();
        this.add(tabbedPane);
        JTextField tfLambda = createTextField(
                (r) -> ((AdditiveRule) r).getLambda(),
                (r, val) -> ((AdditiveRule) r).setLambda((double) val));
        JTextField tfResistance = createTextField(
                (r) -> ((AdditiveRule) r).getResistance(),
                (r, val) -> ((AdditiveRule) r).setResistance((double) val));
        mainTab.addItem("Lambda", tfLambda);
        mainTab.addItem("Resistance", tfResistance);
        mainTab.addItem("Add noise", getAddNoise());
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    @Override
    protected final AdditiveRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
