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
package org.simbrain.workspace.gui.couplingmanager;

import org.simbrain.workspace.*;
import org.simbrain.workspace.gui.couplingmanager.AttributePanel.ProducerOrConsumer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Panel for editing visibility of attributes.
 */
public class AttributeTypePanel extends JPanel {

    //TODO: Consider using this: https://stackoverflow.com/questions/21847411/java-swing-need-a-good-quality-developed-jtree-with-checkboxes

    /**
     * Table representing attribute types.
     */
    private JTable table;

    /**
     * Table model.
     */
    private AttributeModel model;

    /**
     * Workspace component list panel constructor.
     */
    public AttributeTypePanel(WorkspaceComponent component, ProducerOrConsumer poc) {
        super(new BorderLayout());

        model = new AttributeModel();
        table = new JTable(model);
        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setRowSelectionAllowed(false);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setFocusable(false);

        addAttributeTypesToModel(component, poc);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
    }

    private void addAttributeTypesToModel(WorkspaceComponent component, ProducerOrConsumer poc) {
        CouplingManager couplingFactory = component.getWorkspace().getCouplingManager();
        if (poc == ProducerOrConsumer.Consuming) {
            setBorder(BorderFactory.createTitledBorder("Consumers"));
            for (Consumer consumer : couplingFactory.getConsumers(component)) {
                model.addRow(consumer);
            }
        } else {
            setBorder(BorderFactory.createTitledBorder("Producers"));
            for (Producer producer : couplingFactory.getProducers(component)) {
                model.addRow(producer);
            }
        }
    }

    /**
     * Table model which represents types of attributes.
     */
    static class AttributeModel extends AbstractTableModel {

        /**
         * Column names.
         */
        String[] columnNames = {"Source", "Name", "Type", "Visible"};

        /**
         * Internal list of attributes.
         */
        private List<Attribute> data = new ArrayList<>();

        public void addRow(Attribute attribute) {
            data.add(attribute);
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public int getRowCount() {
            return data.size();
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
            case 0:
                return data.get(row).getBaseObject().getClass().getSimpleName();
            case 1:
                return data.get(row).getDescription();
            case 2:
                return data.get(row).getTypeName();
            case 3:
                return data.get(row).isVisible();
            default:
                return null;
            }
        }

        public void setValueAt(Object value, int row, int col) {
            switch (col) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                data.get(row).setVisible((boolean) value);
                fireTableDataChanged();
                break;
            }
        }

        public boolean isCellEditable(int row, int col) {
            return col == 3;
        }

        public Class getColumnClass(int col) {
            switch (col) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            case 3:
                return Boolean.class;
            default:
                return null;
            }
        }

    }

}
