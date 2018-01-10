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

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.simbrain.workspace.*;
import org.simbrain.workspace.gui.couplingmanager.AttributePanel.ProducerOrConsumer;

/**
 * Displays a list of attribute types.
 */
public class AttributeTypePanel extends JPanel {

    /** AttributeType is a string-based representation of unique kinds of Attributes. */
    static class AttributeType {
        String source;
        String name;
        String type;
        boolean visibility;

        AttributeType() {}

        AttributeType(String source, String name, String type, boolean visibility) {
            this.source = source;
            this.name = name;
            this.type = type;
            this.visibility = visibility;
        }

        AttributeType(Attribute attribute) {
            source = attribute.getBaseObject().getClass().getSimpleName();
            name = attribute.getDescription();
            type = attribute.getTypeName();
            visibility = getDefaultVisibility(attribute);
        }

        private boolean getDefaultVisibility(Attribute attribute) {
            if (attribute instanceof Consumer) {
                Consumable annotation = attribute.getMethod().getAnnotation(Consumable.class);
                return annotation.defaultVisibility();
            } else {
                Producible annotation = attribute.getMethod().getAnnotation(Producible.class);
                return annotation.defaultVisibility();
            }
        }

        public String getSource() {
            return source;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public boolean isVisible() {
            return visibility;
        }

        public void setVisible(boolean value) {
            visibility = value;
        }
    }

    /** Table model which represents types of attributes. */
    static class AttributeTypeModel extends AbstractTableModel {

        /** Column names. */
        String[] columnNames = { "Source", "Name", "Type", "Visible" };

        /** Internal list of components. */
        private List<AttributeType> data = new ArrayList<AttributeType>();

        public void addRow(AttributeType type) {
            data.add(type);
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
                return data.get(row).getSource();
            case 1:
                return data.get(row).getName();
            case 2:
                return data.get(row).getType();
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

    /** uniqueAttributeTypes allows us to remember attribute visibility. */
    private static Map<String,AttributeType> uniqueAttributeTypes = new HashMap<String,AttributeType>();

    /** Returns whether the attribute type is default visible or marked visible. */
    public static boolean isAttributeTypeVisible(Attribute attribute) {
        return getUniqueType(new AttributeType(attribute)).isVisible();
    }

    /** Returns the unique AttributeType which was first added to the attribute type map. */
    private static AttributeType getUniqueType(AttributeType attributeType) {
        String key = attributeType.source + attributeType.name + attributeType.type;
        if (uniqueAttributeTypes.containsKey(key)) {
            return uniqueAttributeTypes.get(key);
        } else {
            uniqueAttributeTypes.put(key, attributeType);
            return attributeType;
        }
    }

    /** Attribute types. */
    Set<AttributeType> attributeTypesInModel = new HashSet<AttributeType>();

    /** Table representing attribute types. */
    private JTable table;

    /** Table model. */
    private AttributeTypeModel model;

    /**
     * Workspace component list panel constructor.
     */
    public AttributeTypePanel(WorkspaceComponent component, ProducerOrConsumer poc) {
        super(new BorderLayout());

        model = new AttributeTypeModel();
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
        CouplingFactory couplingFactory = component.getWorkspace().getCouplingFactory();
        if (poc == ProducerOrConsumer.Consuming) {
            setBorder(BorderFactory.createTitledBorder("Consumers"));
            for (Consumer consumer : couplingFactory.getAllConsumers(component)) {
                AttributeType attributeType = new AttributeType(consumer);
                addTypeToModel(attributeType);
            }
        } else {
            setBorder(BorderFactory.createTitledBorder("Producers"));
            for (Producer producer : couplingFactory.getAllProducers(component)) {
                AttributeType attributeType = new AttributeType(producer);
                addTypeToModel(attributeType);
            }
        }
    }

    /** Add an AttributeType to the table model. */
    private void addTypeToModel(AttributeType attributeType) {
        AttributeType uniqueType = getUniqueType(attributeType);
        if (!attributeTypesInModel.contains(uniqueType)) {
            attributeTypesInModel.add(uniqueType);
            model.addRow(uniqueType);
        }
    }

}
