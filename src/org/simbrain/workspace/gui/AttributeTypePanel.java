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
package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.PotentialAttributePanel.ProducerOrConsumer;

/**
 * Displays a list of attribute types.
 */
public class AttributeTypePanel extends JPanel  {

    /** Table representing attribute types. */
    private JTable table;

    /** Table model. */
    private AttributeTypeModel model;

    /**
     * Workspace component list panel constructor.
     *
     * @param desktop reference.
     */
    public AttributeTypePanel(final WorkspaceComponent component, final ProducerOrConsumer poc) {
        super(new BorderLayout());

        // Set up table
        model = new AttributeTypeModel();
        table = new JTable(model);
        ((DefaultTableCellRenderer) table.getTableHeader()
                .getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);
        table.setRowSelectionAllowed(false);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setFocusable(false);

        if (component != null) {
            if (poc == ProducerOrConsumer.Consuming) {
                for (AttributeType type : component.getConsumerTypes()) {
                    setBorder(BorderFactory
                            .createTitledBorder("Consumer type visibility for "
                                    + component.getName()));
                    model.addRow(type);
                }
            } else {
                for (AttributeType type : component.getProducerTypes()) {
                    setBorder(BorderFactory
                            .createTitledBorder("Producer type visibility for "
                                    + component.getName()));
                    model.addRow(type);
                }
            }
        }

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
    }

    /**
     * Table model which represents workspace components.
     */
    class AttributeTypeModel extends AbstractTableModel {

        /** Column names. */
        String[] columnNames = {"Name","Data Type",  "Visible" };

        /** Internal list of components. */
        private List<AttributeType> data = new ArrayList<AttributeType>();      

        public void addRow(AttributeType type) {
            data.add(type);
        }

        /**
         * {@inheritDoc}
         */
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * {@inheritDoc}
         */
        public String getColumnName(int col) {
            return columnNames[col];
        }

        /**
         * {@inheritDoc}
         */
        public int getRowCount() {
            return data.size();
        }

        /**
         * {@inheritDoc}
         */
        public Object getValueAt(int row, int col) {
            switch (col) {
            case 0:
                return data.get(row).getBaseDescription();
            case 1:
                return data.get(row).getDataType().getSimpleName();
            case 2:
                return data.get(row).isVisible();
            default:
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        public void setValueAt(Object value, int row, int col) {
            switch (col) {
            case 0:
                return;
            case 1:
                return;
            case 2:
                data.get(row).setVisible((Boolean) value);
                return;
            }
            this.fireTableDataChanged();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isCellEditable(int row, int col) {
            switch (col) {
            case 0:
                return false;
            case 1:
                return false;
            case 2:
                return true;
            default:
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        public Class getColumnClass(int col) {
            switch (col) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return Boolean.class;
            default:
                return null;
            }
        }

    }

}
