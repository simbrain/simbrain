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
package org.simbrain.world.dataworld;

import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * Dialog for adding multiple columns to data component.
 */
public class AddColumnsDialog extends StandardDialog {

    /** Number of columns to be added. */
    private JTextField columnsField = new JTextField();

    /** Panel to add items. */
    private LabelledItemPanel panel = new LabelledItemPanel();

    /** Data world frame. */
    private DataWorldComponent component;

    /**
     * Dialog for adding columns to data world.
     * @param dataComponent parent component
     */
    public AddColumnsDialog(final DataWorldComponent dataComponent) {
        setTitle("Add Columns");
        this.component = dataComponent;
        fillFieldValues();

        columnsField.setColumns(4);

        panel.addItem("Number of Columns", columnsField);
        setContentPane(panel);
        pack();
        
    }

    /** @see StandardDialog */
    public void closeDialogOk() {
        component.getDataModel().addRowsColumns(0, Integer.parseInt(columnsField.getText()),
                new Double(0));
        super.closeDialogOk();
    }

    /** @see StandardDialog */
    public void closeDialogCancel() {
        super.closeDialogCancel();
    }

    /**
     * Fills the field with initial values.
     */
    private void fillFieldValues() {
        columnsField.setText("0");
    }
}
