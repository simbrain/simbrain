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
import java.util.List;

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
 * <b>SimbrainJTable</b> is a version of a JXTable (itself an improved JTable
 * from SwingLabs) which provides GUI access to a Simbrain data table. Provides
 * various features that are useful in simbrain, e.g. ability to set row and
 * column headings. (Note that JTables have their own tablemodels, which are
 * used here under the hood, but can be ignored when using this class. Just
 * instantiate some subclass of SimbrainJTable and pass it to an instance of
 * this class, and the data will display).
 *
 * @author jyoshimi
 */
public class SimbrainJTable extends JXTable {

    /** The data to be displayed in the jtable. */
    private SimbrainDataTable data;

    /**
     * Row headings. Only used if set, otherwise default row headings (1...n)
     * used.
     */
    private List<String> rowHeadings;

    /**
     * Column headings. Only used if set, otherwise default column headings
     * (1...n) used.
     */
    private List<String> columnHeadings;

    /** Point selected. */
    private Point selectedPoint;

    /** Grid Color. */
    private Color gridColor = Color.LIGHT_GRAY;

    /** Underlying Java table model. */
    private TableModel tableModel;

    /**
     * Construct the table with specified number of rows and columns.
     * 
     * @param rows number of rows of data
     * @param cols number of columns of data
     */
    public SimbrainJTable(int rows, int cols) {
        data = new DefaultNumericTable(rows, cols);
        initJTable();
    }

    /**
     * Creates a new instance of the data world.
     * 
     * @param dataModel
     */
    public SimbrainJTable(SimbrainDataTable dataModel) {
        data = dataModel;
        initJTable();
    }

    /**
     * Initialize the table.
     */
    private void initJTable() {
        this.setModel(new TableModel());
        addKeyListener(keyListener);
        addMouseListener(mouseListener);
        setColumnSelectionAllowed(true);
        setRolloverEnabled(true);
        setRowSelectionAllowed(true);
        setGridColor(gridColor);
        updateRowSelection();

        // First column displays row numbers
        this.setDefaultRenderer(Double.class, new CustomCellRenderer());

        // Sorting is not helpful in datatable contexts (possibly add an option
        // to put it back in)
        this.setSortable(false);

        // Below initially forces first column to specific width; but has other
        // side effects
        // setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        getColumnModel().getColumn(0).setPreferredWidth(30);
        // TODO: Make preferred width for first column settable

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
        if (selectedPoint != null) {
            return rowAtPoint(selectedPoint);
        } else {
            return 0;
        }
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

            if (data instanceof IterableRowsTable) {
                ((IterableRowsTable) data).setCurrentRow(getSelectedRow());
            }
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
    protected JPopupMenu buildPopupMenu() {

        JPopupMenu ret = new JPopupMenu();
        if (getData() instanceof MutableTable) {
            ret.add(TableActionManager.getInsertRowAction(this));
            if (getSelectedColumn() >= 0) {
                ret.add(TableActionManager.getInsertColumnAction(this));
            }
            ret.add(TableActionManager.getDeleteRowAction(this));
            if (getSelectedColumn() != 0) {
                ret.add(TableActionManager.getDeleteColumnAction(this));
            }
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
        return ret;
    }

    /**
     * Return a toolbar with buttons for opening from and saving to .csv files.
     * 
     * @return the csv toolbar
     */
    public JToolBar getToolbarCSV() {
        if (getData() instanceof NumericTable) {
            JToolBar toolbar = new JToolBar();
            toolbar.add(TableActionManager
                    .getOpenCSVAction((NumericTable) getData()));
            toolbar.add(TableActionManager
                    .getSaveCSVAction((NumericTable) getData()));
            return toolbar;
        }
        return null;
    }

    /**
     * Return a toolbar with buttons for editing the table cells.
     * 
     * @return the edit toolbar
     */
    public JToolBar getToolbarEditTable() {
        if (getData() instanceof MutableTable) {
            JToolBar toolbar = new JToolBar();
            toolbar.add(TableActionManager.getInsertRowAction(this));
            if (getSelectedColumn() >= 0) {
                toolbar.add(TableActionManager.getInsertColumnAction(this));
            }
            toolbar.add(TableActionManager.getDeleteRowAction(this));
            if (getSelectedColumn() != 0) {
                toolbar.add(TableActionManager.getDeleteColumnAction(this));
            }
            return toolbar;
        }
        return null;
    }

    /**
     * @return the randomization toolbar
     */
    public JToolBar getToolbarRandomize() {
        if (getData() instanceof NumericTable) {
            JToolBar toolbar = new JToolBar();
            toolbar.add(TableActionManager
                    .getRandomizeAction((NumericTable) getData()));
            toolbar.add(TableActionManager
                    .getSetTableBoundsAction((NumericTable) getData()));
            return toolbar;
        }
        return null;
    }

    /**
     * Return a menu with items for opening from and saving to .csv files.
     * 
     * @return the csv menu
     */
    public JMenu getMenuCSV() {
        if (getData() instanceof NumericTable) {
            JMenu menu = new JMenu("Import / Export .csv");
            menu.add(new JMenuItem(TableActionManager
                    .getOpenCSVAction((NumericTable) getData())));
            menu.add(new JMenuItem(TableActionManager
                    .getSaveCSVAction((NumericTable) getData())));
            return menu;
        }
        return null;
    }

    /**
     * Return a menu with items for randomizing table values.
     * 
     * @return the randomize menu
     */
    public JMenu getMenuRandomize() {
        if (getData() instanceof NumericTable) {
            JMenu menu = new JMenu("Randomize");
            menu.add(TableActionManager
                    .getRandomizeAction((NumericTable) getData()));
            menu.add(TableActionManager
                    .getSetTableBoundsAction((NumericTable) getData()));
            return menu;
        }
        return null;
    }

    /**
     * Return a menu with items for normalizing the data in a table.
     * 
     * @return the normalize menu
     */
    public JMenu getMenuNormalize() {
        if (getData() instanceof NumericTable) {
            JMenu menu = new JMenu("Normalize");
            menu.add(TableActionManager.getNormalizeColumnAction(
                    (NumericTable) getData(), this.getSelectedColumn() - 1));
            menu.add(TableActionManager
                    .getNormalizeAction((NumericTable) getData()));
            return menu;
        }
        return null;
    }

    /**
     * Return a menu with items for filling table values.
     * 
     * @return the fill menu
     */
    public JMenu getMenuFill() {
        if (getData() instanceof NumericTable) {
            JMenu menu = new JMenu("Fill values");
            menu.add(new JMenuItem(TableActionManager
                    .getFillAction((NumericTable) getData())));
            menu.add(new JMenuItem(TableActionManager
                    .getZeroFillAction((NumericTable) getData())));
            return menu;
        }
        return null;
    }

    /**
     * Return a menu with items for changing the table structure.
     * 
     * @return the edit menu
     */
    public JMenu getMenuEdit() {
        JMenu menu = new JMenu("Edit");
        if (getData() instanceof MutableTable) {
            menu.add(new JMenuItem(TableActionManager.changeRowsColumns(this)));
        }
        return menu;
    }

    /**
     * Select current row.
     */
    public void updateRowSelection() {
        // TODO: If I don't call this, the line below does not work. Not sure
        // why.
        selectAll();
        if (getData() instanceof IterableRowsTable) {
            int currentRow = ((IterableRowsTable) data).getCurrentRow();
            setRowSelectionInterval(currentRow, currentRow);
        }
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
            // System.out.println("Key typed");
        }
    };

    /**
     * Returns a copy of the underlying table model.
     * 
     * @return the tableModel
     */
    public TableModel getTableModel() {
        return tableModel;
    }

    /**
     * Renderer for table. If custom row headings are used, treats first column
     * as a set of headings.
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
                if (column == 0) {
                    if (rowHeadings != null) {
                        label.setText(rowHeadings.get(row));
                    } else {
                        // First column displays the row number.
                        label.setText("" + (int) (row + 1));
                    }
                }
                return label;
            } else {
                return super.getTableCellRendererComponent(table, value,
                        selected, focused, row, column);
            }
        }
    }

    /**
     * <b>TableModel</b> extends DefaultTableModel (the standard model for
     * JTables), and passes data from the SimbrainDataTable in to it, so that it
     * can be presented in the JTable. This hides the tablemodel from the
     * client, who only has to use this class and some subclass of
     * SimbrainDataTable.
     */
    private class TableModel extends AbstractTableModel {

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
        public TableModel() {
            super();
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
                data.setValue(row, column - 1, (Double) value);
            }
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex > 0) {
                if (columnHeadings != null) {
                    return columnHeadings.get(columnIndex - 1);
                } else {
                    return "" + (columnIndex);
                }
            } else {
                return "#"; // TODO: Make this settable
            }
        }

        /**
         * {@inheritDoc}
         */
        public int getColumnCount() {
            return data.getColumnCount() + 1;
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
                // This is taken care of by the CustomCellRenderer.
                return null;
            } else {
                return data.getValue(row, column - 1);
            }
        }
    }

    /**
     * @return the rowHeadings
     */
    public List<String> getRowHeadings() {
        return rowHeadings;
    }

    /**
     * @param rowHeadings the rowHeadings to set
     */
    public void setRowHeadings(List<String> rowHeadings) {
        this.rowHeadings = rowHeadings;
    }

    /**
     * @return the columnHeadings
     */
    public List<String> getColumnHeadings() {
        return columnHeadings;
    }

    /**
     * @param columnHeadings the columnHeadings to set
     */
    public void setColumnHeadings(List<String> columnHeadings) {
        this.columnHeadings = columnHeadings;
    }

}
