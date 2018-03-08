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
import org.simbrain.util.propertyeditor2.EditableObject;
import javax.swing.JTabbedPane;

import javax.swing.*;
import java.util.List;

public class MorrisLecarPanel extends AnnotatedPropertyEditor {

    /** Organize the giant cauldron of params for this rule. */
    private JTabbedPane tabbedPane;

    public MorrisLecarPanel(MorrisLecarRule rule) {
        super(rule);
    }

    //TODO: Not done yet.  For help consulting this
    // https://github.com/simbrain/simbrain/blob/master/src/org/simbrain/network/gui/dialogs/neuron/rule_panels/MorrisLecarRulePanel.java

    @Override
    protected void initWidgets() {
        super.initWidgets();
        this.removeAll();
        tabbedPane = new JTabbedPane();
        this.add(tabbedPane);

        LabelledItemPanel cellPanel = new LabelledItemPanel();
        cellPanel.add(new JLabel("Capacitance (µF/cm²):"));
        cellPanel.add(getWidget("Capacitance (µF/cm²)").component);

        LabelledItemPanel ionPanel = new LabelledItemPanel();

        LabelledItemPanel potas = new LabelledItemPanel();

        tabbedPane.add(cellPanel, "Membrane Properties");
        tabbedPane.add(ionPanel, "Ion Properties");
        tabbedPane.add(potas, "K\u207A consts.");
    }
}
