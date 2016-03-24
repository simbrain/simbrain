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
import org.simbrain.network.gui.dialogs.neuron.NoiseGeneratorPanel;
import org.simbrain.network.neuron_update_rules.IzhikevichRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.widgets.YesNoNull;

/**
 * <b>IzhikevichNeuronPanel</b> edits an Izhekevich neuron.
 */
public class IzhikevichRulePanel extends AbstractNeuronRulePanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** A reference to the neuron update rule being edited. */
    private static final IzhikevichRule prototypeRule = new IzhikevichRule();

    /**
     * Creates an instance of this panel.
     */
    public IzhikevichRulePanel() {
        super();
        this.add(tabbedPane);
        JTextField tfA = createTextField((r) -> ((IzhikevichRule) r).getA(),
                (r, val) -> ((IzhikevichRule) r).setA((double) val));
        JTextField tfB = createTextField((r) -> ((IzhikevichRule) r).getB(),
                (r, val) -> ((IzhikevichRule) r).setB((double) val));
        JTextField tfC = createTextField((r) -> ((IzhikevichRule) r).getC(),
                (r, val) -> ((IzhikevichRule) r).setC((double) val));
        JTextField tfD = createTextField((r) -> ((IzhikevichRule) r).getD(),
                (r, val) -> ((IzhikevichRule) r).setD((double) val));
        JTextField tfIBg = createTextField(
                (r) -> ((IzhikevichRule) r).getiBg(),
                (r, val) -> ((IzhikevichRule) r).setiBg((double) val));
        mainTab.addItem("A", tfA);
        mainTab.addItem("B", tfB);
        mainTab.addItem("C", tfC);
        mainTab.addItem("D", tfD);
        mainTab.addItem("Ibg", tfIBg);
        mainTab.addItem("Add noise", getAddNoise());
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(getNoisePanel(), "Noise");
        this.addBottomText("<html>For a list of useful parameter settings<p>"
                + "press the \"Help\" Button.</html>");
    }

    @Override
    protected final IzhikevichRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
