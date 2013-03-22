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
package org.simbrain.world.odorworld;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Hearing;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;
import org.simbrain.world.odorworld.sensors.TileSensor;

/**
 * Panel showing an agent's sensors.
 *
 * @author Jeff Yoshimi
 * @author Lam Nguyen
 *
 */
public class SensorPanel extends JPanel {

    /** Table representing sensor. */
    private JTable table;

    /** Table model. */
    private SensorModel model;

    /**
     * Construct the sensor panel.
     *
     * @param entity the entity whose sensors should be represented.
     */
    public SensorPanel(final OdorWorldEntity entity) {

        // Set up table
        model = new SensorModel();
        table = new JTable(model);
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer())
        .setHorizontalAlignment(SwingConstants.CENTER);
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
                    menuItem.setAction(new AbstractAction("Edit Sensor...",
                            ResourceManager.getImageIcon("Properties.png")) {
                        public void actionPerformed(ActionEvent e) {
                            StandardDialog dialog = new StandardDialog();
                            dialog.setTitle("Edit Sensor");
                            if (sensor instanceof SmellSensor) {
                                SmellSensorPanel smellSensorPanel = new SmellSensorPanel(
                                        entity);
                                smellSensorPanel
                                        .fillFieldValues((SmellSensor) sensor);
                                dialog.setContentPane(smellSensorPanel);
                                dialog.pack();
                                dialog.setLocationRelativeTo(null);
                                dialog.setVisible(true);
                                if (!dialog.hasUserCancelled()) {
                                    smellSensorPanel
                                            .commitChanges((SmellSensor) sensor);
                                }
                            }
                            if (sensor instanceof TileSensor) {
                                TileSensorPanel tileSensorPanel = new TileSensorPanel(
                                        entity);
                                tileSensorPanel
                                        .fillFieldValues((TileSensor) sensor);
                                dialog.setContentPane(tileSensorPanel);
                                dialog.pack();
                                dialog.setLocationRelativeTo(null);
                                dialog.setVisible(true);
                                if (!dialog.hasUserCancelled()) {
                                    tileSensorPanel
                                            .commitChanges((TileSensor) sensor);
                                }
                            }
                            if (sensor instanceof Hearing) {
                                HearingSensorPanel hearingSensorPanel = new HearingSensorPanel(
                                        entity);
                                hearingSensorPanel.fillFieldValues((Hearing) sensor);
                                dialog.setContentPane(hearingSensorPanel);
                                dialog.pack();
                                dialog.setLocationRelativeTo(null);
                                dialog.setVisible(true);
                                if (!dialog.hasUserCancelled()) {
                                    hearingSensorPanel
                                            .commitChanges((Hearing) sensor);
                                }
                            }
                        }
                    });
                    sensorPop.add(menuItem);
                    sensorPop.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        for (Sensor sensor : entity.getSensors()) {
            model.addRow(sensor);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        JPanel buttonBar = new JPanel();
        JButton addSensor = new JButton("Add",
                ResourceManager.getImageIcon("plus.png"));
        JButton deleteSensor = new JButton("Delete",
                ResourceManager.getImageIcon("minus.png"));
        buttonBar.add(addSensor);
        buttonBar.add(deleteSensor);
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
                model.fireTableDataChanged();
            }
        });

        // Add sensor Listener
        entity.getParentWorld().addListener(new WorldListenerAdapter() {
            @Override
            public void sensorRemoved(Sensor sensor) {
                model.removeSensor(sensor);
            }
            public void sensorAdded(Sensor sensor) {
                model.addRow(sensor);
            }
        });
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, scrollPane);
        add(BorderLayout.SOUTH, buttonBar);
    }

    /**
     * Table model which represents sensors.
     */
    class SensorModel extends AbstractTableModel {

        /** Column names. */
        String[] columnNames = { "Id", "Label", "Type" };

        /** Internal list of components. */
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
        public void addRow(Sensor sensor) {
            data.add(sensor);
        }

        /**
         * Remove a row.
         *
         * @param row
         */
        public void removeRow(int row) {
            data.remove(row);
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
                return data.get(row).getId();
            case 1:
                return data.get(row).getLabel();
            case 2:
                return data.get(row).getClass().getSimpleName();
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
                data.get(row).setLabel((String) value);
                return;
            case 2:
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
                return true;
            case 2:
                return false;
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
                return String.class;
            default:
                return null;
            }
        }

    }
}
