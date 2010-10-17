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
package org.simbrain.util.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.swingx.JXTable;

/**
 * <b>SimbrainJTable</b> is a version of a JTable which wraps a Simbrain data
 * table object. Also provides sets of menus and buttons that can be used by
 * components which use this table.
 *
 * @author jyoshimi
 */
public class SimbrainJTable extends JXTable {

    private static final long serialVersionUID = 1L;

    /** Underlying data. */
    private SimbrainTableModel tableModel;

    /** Point selected. */
    private Point selectedPoint;

    /** Grid Color. */
    private Color gridColor = Color.LIGHT_GRAY;

    /** Data model. */
    private SimbrainDataTable data;

    /**
     * Creates a new instance of the data world.
     *
     * @param dataModel
     */
    public SimbrainJTable(SimbrainDataTable dataModel) {

        data = dataModel;
        this.setModel(new SimbrainTableModel());
        addKeyListener(keyListener);
        addMouseListener(mouseListener);
        setColumnSelectionAllowed(true);
        setRolloverEnabled(true);
        setRowSelectionAllowed(true);
        setGridColor(gridColor);
        updateRowSelection();

        // First column displays row numbers
        this.setDefaultRenderer(Double.class, new  CustomCellRenderer());

        // Sorting is not helpful in datatable contexts (possibly add an option to put it back in)
        this.setSortable(false);

        // Below initially forces first column to specific width; but has other side effects
        //setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        //getColumnModel().getColumn(0).setPreferredWidth(30);

    }

    /**
     * Returns the currently selected column.
     *
     * @return the currently selected column
     */
    public int getSelectedColumn() {
        if (selectedPoint == null) {
            return this.getColumnCount() - 1;
        } else {
            return columnAtPoint(selectedPoint);
        }
    }

    /**
     * Returns the currently selected row.
     *
     * @return the currently selected row
     */
    public int getSelectedRow() {
        return rowAtPoint(selectedPoint);
    }

    /**
     * @return the data
     */
    public SimbrainDataTable getData() {
        return data;
    }

    /**
     * Listener for mouse events.
     */
    private MouseListener mouseListener = new MouseAdapter() {

        /**
         * {@inheritDoc}
         */
        public void mousePressed(final MouseEvent e) {
            selectedPoint = e.getPoint();
            data.setCurrentRow(getSelectedRow());
            // TODO: should use isPopupTrigger, see e.g. ContextMenuEventHandler
            boolean isRightClick = (e.isControlDown() || (e.getButton() == 3));
            if (isRightClick) {
                JPopupMenu menu = buildPopupMenu();
                menu.show(SimbrainJTable.this, (int) selectedPoint.getX(),
                        (int) selectedPoint.getY());
            }
        }
    };

    /**
     * Build the context menu for the table.
     *
     * @return The context menu.
     */
    private JPopupMenu buildPopupMenu() {

        JPopupMenu ret = new JPopupMenu();
        ret.add(TableActionManager.getInsertRowAction(this));
        if (getSelectedColumn() >= 0) {
            ret.add(TableActionManager.getInsertColumnAction(this));
        }
        ret.add(TableActionManager.getDeleteRowAction(this));
        if (getSelectedColumn() != 0) {
            ret.add(TableActionManager.getDeleteColumnAction(this));
        }
        ret.addSeparator();
        ret.add(getMenuEdit());
        ret.addSeparator();
        ret.add(getMenuRandomize());
        ret.addSeparator();
        ret.add(getMenuNormalize());
        ret.addSeparator();
        ret.add(getMenuFill());
        ret.addSeparator();
        ret.add(getMenuCSV());

        // JMenu producerMenu = new ProducingAttributeMenu(
        // "Receive coupling from", component.getWorkspace(), component
        // .getConsumingAttributes().get(getSelectedColumn()));
        // ret.add(producerMenu);
        // JMenu consumerMenu = new ConsumingAttributeMenu("Send coupling to",
        // component.getWorkspace(), component.getProducingAttributes()
        // .get(getSelectedColumn()));
        // ret.add(consumerMenu);

        return ret;
    }

    /**
     * Return a toolbar with buttons for opening from and saving to .csv files.
     *
     * @return the csv toolbar
     */
    public JToolBar getToolbarCSV() {
        JToolBar toolbar = new JToolBar();
        toolbar.add(TableActionManager.getOpenCSVAction(getData()));
        toolbar.add(TableActionManager.getSaveCSVAction(getData()));
        return toolbar;
    }

    /**
     * Return a toolbar with buttons for randomzing the table cells.
     *
     * @return the randomization toolbar
     */
    public JToolBar getToolbarEditTable() {
        JToolBar toolbar = new JToolBar();
        toolbar.add(TableActionManager.getInsertColumnAction(this));
        toolbar.add(TableActionManager.getDeleteColumnAction(this));
        toolbar.add(TableActionManager.getInsertRowAction(this));
        toolbar.add(TableActionManager.getDeleteRowAction(this));
        return toolbar;
    }

    /**
     * Return a toolbar with buttons for randomzing the table cells.
     *
     * @return the randomization toolbar
     */
    public JToolBar getToolbarRandomize() {
        JToolBar toolbar = new JToolBar();
        toolbar.add(TableActionManager.getRandomizeAction(getData()));
        toolbar.add(TableActionManager.getSetTableBoundsAction(getData()));
        return toolbar;
    }

    /**
     * Return a menu with items for opening from and saving to .csv files.
     *
     * @return the csv menu
     */
    public JMenu getMenuCSV() {
        JMenu menu = new JMenu("File (.csv)");
        menu.add(new JMenuItem(TableActionManager.getOpenCSVAction(getData())));
        menu.add(new JMenuItem(TableActionManager.getSaveCSVAction(getData())));
        return menu;
    }

    /**
     * Return a menu with items for randomizing table values.
     *
     * @return the randomize menu
     */
    public JMenu getMenuRandomize() {
        JMenu menu = new JMenu("Randomize");
        menu.add(TableActionManager.getRandomizeAction(getData()));
        menu.add(TableActionManager.getSetTableBoundsAction(getData()));
        return menu;
    }

    /**
     * Return a menu with items for normalizing the data in a table.
     *
     * @return the normalize menu
     */
    public JMenu getMenuNormalize() {
        JMenu menu = new JMenu("Normalize");
        menu.add(TableActionManager.getNormalizeColumnAction(this));
        menu.add(TableActionManager.getNormalizeAction(this));
        return menu;
    }

    /**
     * Return a menu with items for filling table values.
     *
     * @return the fill menu
     */
    public JMenu getMenuFill() {
        JMenu menu = new JMenu("Fill values");
        menu.add(new JMenuItem(TableActionManager.getFillAction(getData())));
        menu.add(new JMenuItem(TableActionManager.getZeroFillAction(getData())));
        return menu;
    }

    /**
     * Return a menu with items for changing the table structure.
     *
     * @return the edit menu
     */
    public JMenu getMenuEdit() {
        JMenu menu = new JMenu("Edit");
        menu.add(new JMenuItem(TableActionManager.changeRowsColumns(getData())));
        return menu;
    }

    /**
     * Select current row.
     */
    public void updateRowSelection() {
     // TODO: If I don't call this, the line below does not work. Not sure why.
        selectAll();
        setRowSelectionInterval(data.getCurrentRow(), data.getCurrentRow());
    }

    /**
     * @return The selected point.
     */
    public Point getSelectedPoint() {
        return selectedPoint;
    }

    /**
     * Sets the selected point.
     *
     * @param selectedPoint the selected point
     */
    public void setSelectedPoint(final Point selectedPoint) {
        this.selectedPoint = selectedPoint;
    }

    /**
     * Listener for key events. Not yet used.
     */
    private KeyListener keyListener = new KeyAdapter() {
        /**
         * Responds to key typed events.
         *
         * @param arg0 Key event
         */
        public void keyTyped(final KeyEvent arg0) {
            //System.out.println("Key typed");
        }
    };

    /**
     * Returns a copy of the underlying table model.
     *
     * @return the tableModel
     */
    public SimbrainTableModel getTableModel() {
        return tableModel;
    }

    /**
     * Renderer for table.  Paints first column differently.
     */
    private class CustomCellRenderer extends DefaultTableCellRenderer {

        /**
         * {@inheritDoc}
         */
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean selected, boolean focused, int row,
                int column) {

            if (column == 0) {
                JLabel label = new JLabel();
                label.setOpaque(true);
                label.setText("" + (int) (row + 1));
                return label;
            } else {
                return super.getTableCellRendererComponent(table, value,
                        selected, focused, row, column);
            }
        }
    }

    /**
     * <b>TableModel</b> extends DefaultTableModel so that the addRow and
     * addColumn commands are available.
     */
    private class SimbrainTableModel extends AbstractTableModel {

        /** Listener. */
        private final SimbrainTableListener listener = new SimbrainTableListener() {

            /**
             * {@inheritDoc}
             */
            public void columnAdded(int column) {
                fireTableStructureChanged();
                fireTableDataChanged();
            }

            /**
             * {@inheritDoc}
             */
            public void columnRemoved(int column) {
                fireTableStructureChanged();
                fireTableDataChanged();
            }

            /**
             * {@inheritDoc}
             */
            public void cellDataChanged(int row, int column) {
                fireTableCellUpdated(row, column);
            }

            /**
             * {@inheritDoc}
             */
            public void rowAdded(int row) {
                fireTableRowsInserted(row, row);
            }

            /**
             * {@inheritDoc}
             */
            public void rowRemoved(int row) {
                fireTableRowsDeleted(row, row);
            }

            /**
             * {@inheritDoc}
             */
            public void tableStructureChanged() {
                fireTableStructureChanged();
            }

            /**
             * {@inheritDoc}
             */
            public void tableDataChanged() {
                fireTableDataChanged();
            }

        };

        /**
         * Construct the table model.
         *
         * @param model reference to underlying data.
         */
        public SimbrainTableModel() {
            data.addListener(listener);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return Double.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (column == 0) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column > 0) {
                data.setValue(row, column-1, (Double) value);
            }
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex > 0) {
                return "" + (columnIndex);
            } else {
                return "#";
            }
        }

        /**
         * {@inheritDoc}
         */
        public int getColumnCount() {
            return data.getColumnCount()+1;
        }

        /**
         * {@inheritDoc}
         */
        public int getRowCount() {
            return data.getRowCount();
        }

        /**
         * {@inheritDoc}
         */
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return (row + 1); // First column displays the row number.
            } else {
                return data.get(row, column - 1);
            }
        }

    }


}
