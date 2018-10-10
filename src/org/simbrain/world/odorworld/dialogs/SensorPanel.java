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
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.threedworld.entities.EditorDialog;

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
 * Panel showing an agent's sensors.
 *
 * @author Jeff Yoshimi
 * @author Lam Nguyen
 */
public class SensorPanel extends JPanel {

    /**
     * Table representing sensor.
     */
    private JTable table;

    /**
     * Table model.
     */
    private SensorModel model;

    /**
     * The selected sensor to edit. If more than one sensor is selected in the
     * table, this is the first selected row.
     */
    private Sensor selectedSensor;

    /**
     * Construct the sensor panel.
     *
     * @param entity the entity whose sensors should be represented.
     */
    public SensorPanel(final OdorWorldEntity entity) {

        // Set up table
        model = new SensorModel();
        table = new JTable(model);
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setFocusable(false);
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(final MouseEvent e) {
                if (e.isControlDown() || (e.getButton() == 3)) {
                    final int row = table.rowAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    JPopupMenu sensorPop = new JPopupMenu();
                    JMenuItem menuItem = new JMenuItem("Edit Sensor...");
                    sensorPop.add(menuItem);
                    sensorPop.show(e.getComponent(), e.getX(), e.getY());
                    final Sensor sensor = model.getSensor(row);
                    menuItem.addMouseListener(new MouseAdapter() {
                        public void mouseReleased(MouseEvent e) {
                            editSensor(sensor);
                        }
                    });
                    sensorPop.add(menuItem);
                    sensorPop.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(final MouseEvent e) {
                if (e.getClickCount() == 2 && table.columnAtPoint(e.getPoint()) != 1) {
                    final int row = table.rowAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    final Sensor sensor = model.getSensor(row);
                    editSensor(sensor);
                }
            }
        });
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedRow() > 0) {
                    selectedSensor = model.getSensor(table.getSelectedRow());
                }
            }
        });
        for (Sensor sensor : entity.getSensors()) {
            model.addSensor(sensor);
        }

        JScrollPane scrollPane = new JScrollPane(table);

        JPanel buttonBar = new JPanel();
        JButton addSensor = new JButton("Add", ResourceManager.getImageIcon("plus.png"));
        addSensor.setToolTipText("Add sensor...");
        JButton deleteSensor = new JButton("Delete", ResourceManager.getImageIcon("minus.png"));
        deleteSensor.setToolTipText("Delete sensor...");
        JButton editSensor = new JButton("Edit", ResourceManager.getImageIcon("Properties.png"));
        editSensor.setToolTipText("Edit sensor...");
        buttonBar.add(addSensor);
        buttonBar.add(deleteSensor);
        buttonBar.add(editSensor);
        deleteSensor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = table.getSelectedRows();
                List<Sensor> toDelete = new ArrayList<Sensor>();
                for (int i = 0; i < selectedRows.length; i++) {
                    toDelete.add(model.getSensor(selectedRows[i]));
                }
                for (Sensor sensor : toDelete) {
                    if (sensor != null) {
                        entity.removeSensor(sensor);
                    }
                }
            }
        });
        addSensor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AddSensorDialog dialog = new AddSensorDialog(entity);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        });
        editSensor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editSensor(selectedSensor);
            }
        });

        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, scrollPane);
        add(BorderLayout.SOUTH, buttonBar);

        entity.addPropertyChangeListener(evt -> {
            if ("sensorAdded".equals(evt.getPropertyName())) {
                model.addSensor((Sensor) evt.getNewValue());
            } else if ("sensorRemoved".equals(evt.getPropertyName())) {
                model.removeSensor((Sensor) evt.getNewValue());
            }
        });

    }

    /**
     * Edit a sensor.
     */
    private void editSensor(Sensor sensor) {
        // Cheap null fix.  Panel is null when no item is selected on opening.
        // Should disable the edit button in this case.
        if (sensor == null) {
            return;
        }

        AnnotatedPropertyEditor sensorPanel = new AnnotatedPropertyEditor(sensor);
        StandardDialog dialog = sensorPanel.getDialog();
        dialog.setTitle("Edit Sensor");
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Table model which represents sensors.
     */
    class SensorModel extends AbstractTableModel {

        /**
         * Column names.
         */
        String[] columnNames = {"Id", "Label", "Type"};

        /**
         * Internal list of components.
         */
        private List<Sensor> data = new ArrayList<Sensor>();

        /**
         * Helper method to get a reference to the sensor displayed in a row.
         *
         * @param row the row index
         * @return the sensor displayed in that row.
         */
        public Sensor getSensor(int row) {
            if (row < data.size()) {
                return data.get(row);
            } else {
                return null;
            }
        }

        /**
         * Remove a sensor from the table representation.
         *
         * @param sensor the sensor to remove
         */
        public void removeSensor(Sensor sensor) {
            data.remove(sensor);
            fireTableDataChanged();
        }

        /**
         * Add a row.
         *
         * @param sensor
         */
        public void addSensor(Sensor sensor) {
            data.add(sensor);
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

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (col) {
                case 0:
                    return false;
                case 1:
                    return true;
                case 2:
                    return false;
                default:
                    return false;
            }
        }

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
