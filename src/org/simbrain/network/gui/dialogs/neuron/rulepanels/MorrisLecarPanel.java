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
package org.simbrain.network.gui.dialogs.neuron.rulepanels;

import org.simbrain.network.neuron_update_rules.MorrisLecarRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;

import javax.swing.JTabbedPane;

public class MorrisLecarPanel extends AnnotatedPropertyEditor {

    /** Organize the giant cauldron of params for this rule. */
    private JTabbedPane tabbedPane;

    public MorrisLecarPanel(MorrisLecarRule rule) {
        super(rule);
    }

    //TODO: Not done yet.  For help consulting this
    // https://github.com/simbrain/simbrain/blob/master/src/org/simbrain/network/gui/dialogs/neuron/rule_panels/MorrisLecarRulePanel.java

    @Override
    protected void initPanel() {
        super.initPanel();
        this.removeAll();
        tabbedPane = new JTabbedPane();
        this.add(tabbedPane);

        LabelledItemPanel cellPanel = new LabelledItemPanel();
        cellPanel.addItem("Capacitance (µF/cm²):", getWidget("Capacitance (µF/cm²)").component);
        cellPanel.addItem("Voltage const. 1", getWidget("Voltage const. 1").component);
        cellPanel.addItem("Voltage const. 2", getWidget("Voltage const. 2").component);
        cellPanel.addItem("Threshold (mV)",  getWidget("Threshold (mV)").component);
        cellPanel.addItem("Background current (nA)", getWidget("Background current (nA)").component);
        cellPanel.addItem("Add noise", getWidget("Add noise").component);

        LabelledItemPanel ionPanel = new LabelledItemPanel();
        ionPanel.addItem("Ca²⁺ Conductance (µS/cm²)", getWidget("Ca²⁺ Conductance (µS/cm²)").component);
        ionPanel.addItem("K⁺ Conductance (µS/cm²)", getWidget("K⁺ Conductance (µS/cm²)").component);
        ionPanel.addItem("Leak Conductance (µS/cm²)", getWidget("Leak Conductance (µS/cm²)").component);
        ionPanel.addItem("Ca²⁺ Equilibrium (mV)", getWidget("Ca²⁺ Equilibrium (mV)").component);
        ionPanel.addItem("K⁺ Equilibrium (mV)", getWidget("K⁺ Equilibrium (mV)").component);
        ionPanel.addItem("Leak Equilibrium (mV)", getWidget("Leak Equilibrium (mV)").component);

        LabelledItemPanel potas = new LabelledItemPanel();
        potas.addItem("K⁺  Const. 1", getWidget("K⁺  Const. 1").component);
        potas.addItem("K⁺  Const. 2", getWidget("K⁺  Const. 2").component);
        potas.addItem("K⁺ φ", getWidget("K⁺ φ").component);

        tabbedPane.add(cellPanel, "Membrane Properties");
        tabbedPane.add(ionPanel, "Ion Properties");
        tabbedPane.add(potas, "K\u207A consts.");
    }
}
