/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simbrain.network.dialog.network.layout;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simnet.layouts.Layout;
import org.simnet.layouts.LineLayout;


/**
 * <b>LayoutPanel</b> allows the user to define the layout of a network.
 */
public class LineLayoutPanel extends AbstractLayoutPanel {

    /** Spacing field. */
    private JTextField tfSpacing = new JTextField("40");

    /** Layout style selected. */
    private JComboBox cbLayouts = new JComboBox(new String[] {"Horizontal", "Vertical"});

    /**
     * Default constructor.
     */
    public LineLayoutPanel() {
        this.addItem("Layout Style", cbLayouts);
        this.addItem("Spacing between neurons", tfSpacing);
    }

    /**
     * @return Returns the neuronLayout.
     */
    public Layout getNeuronLayout() {
        LineLayout layout = new LineLayout(Double.parseDouble(tfSpacing.getText()), cbLayouts.getSelectedIndex());
        return layout;
    }

}
