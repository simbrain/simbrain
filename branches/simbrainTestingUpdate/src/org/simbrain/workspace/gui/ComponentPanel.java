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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;
import org.simbrain.workspace.updater.WorkspaceUpdaterListener;

/**
 * Displays a list of all currently open workspace components.
 *
 * TODO: Will need to make each row of the list be an object wrapping the
 * component. This is so the rows can listen for workspacecomponent changes, and
 * also so users can right click on the rows and use them as a to set parameters
 * of the represented component. Once it's fixed, the test of this will be
 * setting component properties via terminal and seeing the change reflected.
 */
public class ComponentPanel extends JPanel implements WorkspaceListener,
        WorkspaceUpdaterListener {

    /** Table representing workspace components. */
    private JTable componentTable;

    /** Table model. */
    private ComponentTableModel model;

    /** Update method label. */
    private JLabel updateLabel = new JLabel();

    /** Reference to Simbrain Desktop. */
    private SimbrainDesktop desktop;

    /**
     * Workspace component list panel constructor.
     *
     * @param desktop reference.
     */
    public ComponentPanel(final SimbrainDesktop desktop) {
        super(new BorderLayout());
        desktop.getWorkspace().addListener(this);
        desktop.getWorkspace().getUpdater().addUpdaterListener(this);
        this.desktop = desktop;

        // Set up table
        model = new ComponentTableModel();
        componentTable = new JTable(model);
        ((DefaultTableCellRenderer) componentTable.getTableHeader()
                .getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);
        componentTable.setRowSelectionAllowed(false);
        componentTable.setGridColor(Color.LIGHT_GRAY);
        componentTable.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(componentTable);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add("Center", scrollPane);

        changedUpdateController();
        panel.add("North", updateLabel);

        add(panel);
    }

    /**
     * Update the panel.
     */
    private void update() {
    }

    /**
     * {@inheritDoc}
     */
    public void componentAdded(WorkspaceComponent component) {
        model.addRow(component);
    }

    /**
     * {@inheritDoc}
     */
    public void componentRemoved(WorkspaceComponent component) {
        model.removeRow(component);
    }

    /**
     * {@inheritDoc}
     */
    public void workspaceCleared() {
        model.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void newWorkspaceOpened() {
    }

    /**
     * {@inheritDoc}
     */
    public boolean clearWorkspace() {
        update();
        return true;
    }

    /**
     * Table model which represents workspace components.
     */
    class ComponentTableModel extends AbstractTableModel {

        /** Column names. */
        String[] columnNames = { "Component", "Type", "Gui On", "Update On" };

        /** Internal list of components. */
        private ArrayList<WorkspaceComponent> data = new ArrayList<WorkspaceComponent>();

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
                return data.get(row).getName();
            case 1:
                return data.get(row).getSimpleName();
            case 2:
                return data.get(row).getGuiOn();
            case 3:
                return data.get(row).getUpdateOn();
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
                data.get(row).setGuiOn((Boolean) value);
                return;
            case 3:
                data.get(row).setUpdateOn((Boolean) value);
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
            case 3:
                return true;
            default:
                return false;
            }
        }

        /**
         * Add a new component to the list.
         */
        public void addRow(WorkspaceComponent component) {
            data.add(component);
            fireTableStructureChanged();
        }

        /**
         * Clear the component list.
         */
        public void clear() {
            data.clear();
            fireTableStructureChanged();
        }

        /**
         * Remove a component from the list.
         *
         * @param component the component to remove.
         */
        public void removeRow(WorkspaceComponent component) {
            data.remove(component);
            fireTableStructureChanged();
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
            case 3:
                return Boolean.class;
            case 4:
                return Integer.class;
            default:
                return null;
            }
        }

    }

    public void changeNumThreads() {
        // TODO Auto-generated method stub
    }

    public void changedUpdateController() {
        // TODO:
        // updateLabel.setText("Current updater: " +
        // desktop.getWorkspace().getUpdator().getCurrentUpdatorName());
    }

    public void updatedCouplings(int update) {
        // TODO Auto-generated method stub
    }

    public void updatingFinished() {
        // TODO Auto-generated method stub
    }

    public void updatingStarted() {
        // TODO Auto-generated method stub
    }

    public void workspaceUpdated() {
        // TODO Auto-generated method stub
    }

}
