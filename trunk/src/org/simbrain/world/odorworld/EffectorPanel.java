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
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;

/**
 * Panel showing an agent's effectors.
 *
 * TODO: Possibly abstract and integrate with sensor panel.
 *
 * @author Lam Nguyen
 * @author Jeff Yoshimi
 */
public class EffectorPanel extends JPanel {

    /** Table representing Effector. */
    private JTable table;

    /** Table model. */
    private EffectorModel model;

    public EffectorPanel(final OdorWorldEntity entity) {

        // Set up table
        model = new EffectorModel();
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
                    JPopupMenu effectorPop = new JPopupMenu();
                    JMenuItem menuItem = new JMenuItem("Edit Effector...");
                    effectorPop.add(menuItem);
                    effectorPop.show(e.getComponent(), e.getX(), e.getY());
                    final Effector effector = model.getEffector(row);
                    menuItem.setAction(new AbstractAction("Edit Effector...",
                            ResourceManager.getImageIcon("Properties.png")) {
                        public void actionPerformed(ActionEvent e) {
                            StandardDialog dialog = new StandardDialog();
                            if (effector instanceof Turning) {
                                TurningEffectorPanel turningEffectorPanel = new TurningEffectorPanel(entity);
                                turningEffectorPanel.fillFieldValues((Turning) effector);
                                dialog.setContentPane(turningEffectorPanel);
                                dialog.pack();
                                dialog.setLocationRelativeTo(null);
                                dialog.setVisible(true);
                                if (!dialog.hasUserCancelled()) {
                                    turningEffectorPanel.commitChanges((Turning) effector);
                                }
                            }
                            if (effector instanceof StraightMovement) {
                                StraightEffectorPanel straightEffectorPanel = new StraightEffectorPanel(entity);
                                straightEffectorPanel.fillFieldValues((StraightMovement) effector);
                                dialog.setContentPane(straightEffectorPanel);
                                dialog.pack();
                                dialog.setLocationRelativeTo(null);
                                dialog.setVisible(true);
                                if (!dialog.hasUserCancelled()) {
                                    straightEffectorPanel.commitChanges((StraightMovement) effector);
                                }
                            }
                        }

                    });
                    effectorPop.add(menuItem);
                    effectorPop.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        for (Effector effector : entity.getEffectors()) {
            model.addRow(effector);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        JPanel buttonBar = new JPanel();
        JButton addEffector = new JButton(
                ResourceManager.getImageIcon("plus.png"));
        JButton deleteEffector = new JButton(
                ResourceManager.getImageIcon("minus.png"));
        buttonBar.add(addEffector);
        buttonBar.add(deleteEffector);
        deleteEffector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent del) {
                int[] selectedRows = table.getSelectedRows();
                List<Effector> toDelete = new ArrayList<Effector>();
                for (int i = 0; i < selectedRows.length; i++) {
                    toDelete.add(model.getEffector(selectedRows[i]));
                }
                for (Effector effector: toDelete) {
                    if (effector != null) {
                        entity.removeEffector(effector);
                    }
                }
            }
        });
        addEffector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AddEffectorDialog dialog = new AddEffectorDialog(entity);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                model.commitChanges();
                model.fireTableDataChanged();
            }
        });
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, scrollPane);
        add(BorderLayout.SOUTH, buttonBar);

        entity.getParentWorld().addListener(new WorldListenerAdapter() {
            @Override
            public void effectorRemoved(Effector effector) {
                model.removeEffector(effector);
            }

            @Override
            public void effectorAdded(Effector effector) {
                model.addRow(effector);
            }
        });
    }

    /**
     * Table model which represents Effectors
     */
    class EffectorModel extends AbstractTableModel {

        /** Column names. */
        String[] columnNames = { "Id", "Label", "Type" };

        /** Internal list of components. */
        private List<Effector> data = new ArrayList<Effector>();

        /**
         * Helper method to get a reference to the effector displayed in a row.
         *
         * @param row the row index
         * @return the effector displayed in that row.
         */
        public Effector getEffector(int row) {
            if (row < data.size()) {
                return data.get(row);
            } else {
                return null;
            }
        }

        public void commitChanges() {

        }
        /**
         * Remove an effector from the table representation.
         *
         * @param effector the effector to remove
         */
        public void removeEffector(Effector effector) {
            data.remove(effector);
            fireTableDataChanged();
        }

        /**
         * Add a row
         *
         * @param Effector
         */
        public void addRow(Effector effector) {
            data.add(effector);
        }

        /**
         * Remove a row
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
