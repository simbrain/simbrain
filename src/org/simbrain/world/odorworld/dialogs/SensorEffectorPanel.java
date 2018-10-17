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
package org.simbrain.world.odorworld.dialogs;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.PeripheralAttribute;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel showing an agent's sensors or effectors.
 *
 * @author Jeff Yoshimi
 * @author Lam Nguyen
 */
public class SensorEffectorPanel extends JPanel {

    /**
     * Table representing sensors / effectors.
     */
    private JTable table;

    /**
     * Table model.
     */
    private AttributeModel model;

    /**
     * Whether this is a sensor or effector panel.
     */
    public enum PanelType {Sensor, Effector};

    /**
     * Initial sensor or effector to edit, if any.  Also used to determine the type
     * for this panel.
     */
    private PanelType type;

    /**
     * Currently selected attribute
     */
    private PeripheralAttribute selectedAttribute;

    /**
     * Parent entity.
     */
    private final OdorWorldEntity parentEntity;

    /**
     * Parent window.
     */
    private final Window parentWindow;

    /**
     * Construct the SensorEffectorPanel.
     *
     * @param type
     */
    public SensorEffectorPanel(OdorWorldEntity parentEntity, final PanelType type, final Window parentWindow) {

        this.parentEntity = parentEntity;
        this.type = type;
        this.parentWindow = parentWindow;

        model = new AttributeModel();
        table = new JTable(model);
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setFocusable(false);

        // Context menu
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(final MouseEvent e) {
                if (e.isControlDown() || (e.getButton() == 3)) {
                    final int row = table.rowAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem menuItem = new JMenuItem("Edit...");
                    popupMenu.add(menuItem);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    final PeripheralAttribute attribute = model.getAttribute(row);
                    menuItem.addMouseListener(new MouseAdapter() {
                        public void mouseReleased(MouseEvent e) {
                            editAttribute(attribute);
                        }
                    });
                    popupMenu.add(menuItem);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(final MouseEvent e) {
                if (e.getClickCount() == 2 && table.columnAtPoint(e.getPoint()) != 1) {
                    final int row = table.rowAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    final PeripheralAttribute sensor = model.getAttribute(row);
                    editAttribute(sensor);
                }
            }
        });

        // Set selected item
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedRow() >= 0) {
                    selectedAttribute = model.getAttribute(table.getSelectedRow());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        JPanel buttonBar = new JPanel();

        // Add attribute
        JButton addAttribute = new JButton("Add", ResourceManager.getImageIcon("plus.png"));
        addAttribute.setToolTipText("Add...");
        buttonBar.add(addAttribute);
        addAttribute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StandardDialog dialog;
                if (type == PanelType.Sensor) {
                    dialog = new AddSensorDialog(parentEntity);
                } else {
                    dialog = new AddEffectorDialog(parentEntity);
                }
                // Putting these on the side since I can't figure out a way to get this in front of its parent dialog
                // after trying everything (toFront, alwaysOnTop, etc).
                SwingUtilities.invokeLater(() -> {
                    dialog.setLocation(parentWindow.getX() - dialog.getWidth(), parentWindow.getY());
                });
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        // Delete attribute
        JButton deleteAttribute = new JButton("Delete", ResourceManager.getImageIcon("minus.png"));
        deleteAttribute.setToolTipText("Delete...");
        buttonBar.add(deleteAttribute);
        deleteAttribute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = table.getSelectedRows();
                List<PeripheralAttribute> toDelete = new ArrayList();
                for (int i = 0; i < selectedRows.length; i++) {
                    toDelete.add(model.getAttribute(selectedRows[i]));
                }
                for (PeripheralAttribute attribute : toDelete) {
                    if (attribute != null) {
                        if (attribute instanceof Sensor) {
                            parentEntity.removeSensor((Sensor) attribute);

                        } else {
                            parentEntity.removeEffector((Effector) attribute);
                        }
                    }
                }
            }
        });

        // Edit attribute
        JButton editAttribute = new JButton("Edit", ResourceManager.getImageIcon("Properties.png"));
        editAttribute.setToolTipText("Edit...");
        buttonBar.add(editAttribute);
        editAttribute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editAttribute(selectedAttribute);
            }
        });

        // Final GUI setup
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, scrollPane);
        add(BorderLayout.SOUTH, buttonBar);

        // Populate table
        if (type == PanelType.Sensor) {
            for (Sensor sensor : parentEntity.getSensors()) {
                model.addAttribute(sensor);
            }
        } else {
            for (Effector effector: parentEntity.getEffectors()) {
                model.addAttribute(effector);
            }
        }

        // Set up event listeners
        if (type == PanelType.Sensor) {
            parentEntity.addPropertyChangeListener(evt -> {
                if ("sensorAdded".equals(evt.getPropertyName())) {
                    model.addAttribute((Sensor) evt.getNewValue());
                } else if ("sensorRemoved".equals(evt.getPropertyName())) {
                    model.removeAttribute((Sensor) evt.getNewValue());
                }
            });
        } else {
            parentEntity.addPropertyChangeListener(evt -> {
                if ("effectorAdded".equals(evt.getPropertyName())) {
                    model.addAttribute((Effector) evt.getNewValue());
                } else if ("effectorRemoved".equals(evt.getPropertyName())) {
                    model.removeAttribute((Effector) evt.getNewValue());
                }
            });
        }

    }

    /**
     * Edit an attribute.
     */
    private void editAttribute(PeripheralAttribute attribute) {
        // Panel is null when no item is selected on opening.
        // TODO: Disable the edit button in this case.
        if (attribute == null) {
            return;
        }

        AnnotatedPropertyEditor attributePanel = new AnnotatedPropertyEditor((EditableObject) attribute);
        StandardDialog dialog = attributePanel.getDialog();
        SwingUtilities.invokeLater(() -> {
            dialog.setLocation(parentWindow.getX() - dialog.getWidth(), parentWindow.getY());
        });
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * Table model which represents sensors and effectors.
     */
    class AttributeModel extends AbstractTableModel {

        /**
         * Column names.
         */
        String[] columnNames = {"Id", "Label", "Type"};

        /**
         * Internal list of components.
         */
        private List<PeripheralAttribute> data = new ArrayList();

        /**
         * Helper method to get a reference to the attribute displayed in a row.
         *
         * @param row the row index
         * @return the attribute displayed in that row.
         */
        public PeripheralAttribute getAttribute(int row) {
            if (row < data.size()) {
                return data.get(row);
            } else {
                return null;
            }
        }

        /**
         * Remove an attribute from the table representation.
         *
         * @param attribute the attribute to remove
         */
        public void removeAttribute(PeripheralAttribute attribute) {
            data.remove(attribute);
            fireTableDataChanged();
        }

        /**
         * Add an attribute
         *
         * @param attribute the attribute to add
         */
        public void addAttribute(PeripheralAttribute attribute) {
            data.add(attribute);
            model.fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            switch (col) {
            case 0:
                return data.get(row).getId();
            case 1:
                return data.get(row).getLabel();
            case 2:
                return data.get(row).getTypeDescription();
            default:
                return null;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            switch (col) {
            case 0:
                return;
            case 1:
                data.get(row).setLabel((String) value);
                return;
            case 2:
                return;
            }
            this.fireTableDataChanged();
        }

//        @Override
//        public boolean isCellEditable(int row, int col) {
//            switch (col) {
//                case 0:
//                    return false;
//                case 1:
//                    return true;
//                case 2:
//                    return false;
//                default:
//                    return false;
//            }
//        }

        @Override
        public Class getColumnClass(int col) {
            switch (col) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            default:
                return null;
            }
        }

    }
}
