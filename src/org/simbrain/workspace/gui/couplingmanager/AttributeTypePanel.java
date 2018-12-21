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
import java.lang.reflect.Method;
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
        model.setWorkspaceComponent(component);
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
        if (poc == ProducerOrConsumer.Consuming) {
            setBorder(BorderFactory.createTitledBorder("Consumers"));
            component.getAttributeMethods(Consumable.class)
                    .forEach(a -> model.addRow(a));
        } else {
            setBorder(BorderFactory.createTitledBorder("Producers"));
            component.getAttributeMethods(Producible.class)
                    .forEach(a -> model.addRow(a));
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
        private List<Method> data = new ArrayList<>();

        private WorkspaceComponent workspaceComponent;

        public void addRow(Method attribute) {
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
                return data.get(row).getDeclaringClass().getSimpleName();
            case 1:
                return data.get(row).getName(); // TODO: currently using raw method name. Consider user friendly name.
            case 2:
                if (data.get(row).getParameterCount() > 0) {    // if attribute type is Consumable
                    return data.get(row).getParameterTypes()[0].getSimpleName();
                } else {
                    return data.get(row).getReturnType().getSimpleName();
                }
            case 3:
                return workspaceComponent.getAttributeTypeVisibilityMap().get(data.get(row));
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
                workspaceComponent.getAttributeTypeVisibilityMap().put(data.get(row), (Boolean) value);
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

        public void setWorkspaceComponent(WorkspaceComponent workspaceComponent) {
            this.workspaceComponent = workspaceComponent;
        }
    }

}
