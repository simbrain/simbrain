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
 * Dialog for setting rows and columns.
 */
public class SetRowsColumnsDialog extends StandardDialog {

    /** Number of columns. */
    private JTextField columnsField = new JTextField();

    /** Number of rows. */
    private JTextField rowsField = new JTextField();

    /** Panel to add items. */
    private LabelledItemPanel panel = new LabelledItemPanel();

    /** Data world frame. */
    private DataWorldComponent component;

    /**
     * Dialog for setting the size of the data world.
     * @param dataComponent parent frame
     */
    public SetRowsColumnsDialog(final DataWorldComponent dataComponent) {
        setTitle("Set Rows/Columns");
        this.component = dataComponent;
        fillFieldValues();

        columnsField.setColumns(4);

        panel.addItem("Number of Rows", rowsField);
        panel.addItem("Number of Columns", columnsField);
        setContentPane(panel);
        pack();
    }

    /** @see StandardDialog */
    public void closeDialogOk() {
        component.getDataModel().modifyRowsColumns(Integer.parseInt(rowsField.getText()),
                Integer.parseInt(columnsField.getText()), new Double(0));
        super.closeDialogOk();
    }

    /**
     * Fills the fields with current values.
     */
    private void fillFieldValues() {
        columnsField.setText(Integer.toString(component.getDataModel().getColumnCount()));
        rowsField.setText(Integer.toString(component.getDataModel().getRowCount()));
    }

    /** @see StandardDialog */
    public void closeDialogCancel() {
        super.closeDialogCancel();
    }
}
