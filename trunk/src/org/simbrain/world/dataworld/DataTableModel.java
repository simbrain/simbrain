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

import javax.swing.table.AbstractTableModel;

/**
 * <b>TableModel</b> extends DefaultTableModel so that the addRow and addColumn
 * commands are available.
 */
public class DataTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    
    private final DataModel<Double> model;
    
    private final DataModel.Listener listener = new DataModel.Listener() {

        public void columnAdded(int column) {
            fireTableStructureChanged();
            fireTableDataChanged();
        }

        public void columnRemoved(int column) {
            fireTableStructureChanged();
            fireTableDataChanged();
        }

        public void dataChanged() {
            fireTableDataChanged();
        }

        public void itemChanged(int row, int column) {
            fireTableCellUpdated(row, column);
        }

        public void rowAdded(int row) {
            fireTableRowsInserted(row, row);
        }

        public void rowRemoved(int row) {
            fireTableRowsDeleted(row, row);
        }
        
    };
    
    DataTableModel(final DataModel<Double> model)
    {
        this.model = model;
        model.addListener(listener);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return Double.class;
    }

    public int getColumnCount() {
        return model.getColumnCount();
    }

    public String getColumnName(int columnIndex) {
        return "" + (columnIndex + 1);
    }

    public int getRowCount() {
        return model.getRowCount();
    }

    public Object getValueAt(int row, int column) {
        return model.get(row, column);
    }

    public boolean isCellEditable(int row, int column) {
        return true;
    }

    public void setValueAt(Object value, int row, int column) {
        model.set(row, column, (Double) value);
    }
}
