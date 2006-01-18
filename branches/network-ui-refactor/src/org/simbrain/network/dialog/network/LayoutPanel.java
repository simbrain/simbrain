/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog.network;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;


/**
 * <b>LayoutPanel</b> allows the user to define the layout of a network.
 */
public class LayoutPanel extends LabelledItemPanel {
    /** Line string. */
    public static final String LINE = "Line";
    /** Grid string. */
    public static final String GRID = "Grid";
    /** Layout list. */
    public static String[] layoutList = {LINE, GRID };
    /** Layouts combo box. */
    private JComboBox cbLayouts = new JComboBox(layoutList);
    /** Number of rows field. */
    private JTextField tfRows = new JTextField();
    /** Number of columns field. */
    private JTextField tfColumns = new JTextField();

    /**
     * This method is the default constructor.
     */
    public LayoutPanel() {
        this.addItem("Layout Style", cbLayouts);
    }

    /**
     * @return the current network layout.
     */
    public String getCurrentLayout() {
        return cbLayouts.getSelectedItem().toString();
    }

    /**
     * @param layout Sets the current network layout.
     */
    public void setCurrentLayout(final String layout) {
        if (layout.equalsIgnoreCase(LINE)) {
            cbLayouts.setSelectedIndex(0);
        } else if (layout.equalsIgnoreCase(GRID)) {
            cbLayouts.setSelectedIndex(1);
        }
    }

    /**
     * Initializes the layout panel with requred fields.
     */
    public void initPanel() {
        if (getCurrentLayout().equalsIgnoreCase(GRID)) {
            this.addItem("Rows", tfRows);
            this.addItem("Columns", tfColumns);
        }
    }
}
