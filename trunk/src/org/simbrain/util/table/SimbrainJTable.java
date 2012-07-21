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
     * Custom column headings. Only used if set, otherwise default column
     * headings (1...n) used.
     */
    private List<String> columnHeadings;

    /** Point selected. */
    private Point selectedPoint;

    /** Grid Color. */
    private Color gridColor = Color.LIGHT_GRAY;

    /** Underlying Java table model. */
    private TableModel tableModel;

    /** Whether to display column headings. */
    private boolean displayColumnHeadings = true;

    /** Whether to display the default popup menu. */
    private boolean displayPopUpMenu = true;

    /**
     * Whether the table's data has changed or not, since the last save. Used
     * externally.
     */
    private boolean hasChangedSinceLastSave = false;

    /** Flags for popup menus. */
    private boolean showInsertRowPopupMenu = true;
    private boolean showInsertColumnPopupMenu = true;
    private boolean showDeleteRowPopupMenu = true;
    private boolean showDeleteColumnPopupMenu = true;
    private boolean showEditInPopupMenu = true;
    private boolean showRandomizeInPopupMenu = true;
    private boolean showNormalizeInPopupMenu = true;
    private boolean showFillInPopupMenu = true;
    private boolean showCSVInPopupMenu = true;

    /**
     * Construct the table with specified number of rows and columns.
     *
     * @param rows number of rows of data
     * @param cols number of columns of data
     */
    public SimbrainJTable(int rows, int cols) {
        data = new NumericTable(rows, cols);
        initJTable();
    }

    /**
     * Creates a new instance of the data world.
     *
     * @param dataModel
     */
    public SimbrainJTable(SimbrainDataTable dataModel) {
        data = dataModel;
        // get data values and use to set?
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

        // Disable specific popupmenus for text tables
        if (data instanceof TextTable) {
            showRandomizeInPopupMenu = false;
            showNormalizeInPopupMenu = false;
            showFillInPopupMenu = false;
            showCSVInPopupMenu = false;
        }

        hasChangedSinceLastSave = false;
    }

    /**
     * Disable popup menus that allow table structure to changed.
     */
    public void disableTableModificationMenus() {
        showInsertRowPopupMenu = false;
        showInsertColumnPopupMenu = false;
        showDeleteRowPopupMenu = false;
        showDeleteColumnPopupMenu = false;
        showEditInPopupMenu = false;
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
            if (isRightClick && displayPopUpMenu) {
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

            if (showInsertRowPopupMenu) {
                ret.add(TableActionManager.getInsertRowAction(this));
            }
            if (showInsertColumnPopupMenu) {
                if (getSelectedColumn() >= 0) {
                    ret.add(TableActionManager.getInsertColumnAction(this));
                }
            }
            if (showDeleteRowPopupMenu) {
                ret.add(TableActionManager.getDeleteRowAction(this));
            }
            if (showDeleteColumnPopupMenu) {
                if (getSelectedColumn() != 0) {
                    ret.add(TableActionManager.getDeleteColumnAction(this));
                }
            }
        }
        if (showEditInPopupMenu) {
            JMenuItem editItem = getMenuEdit();
            if (editItem != null) {
                ret.add(editItem);
            }
        }
        if (showRandomizeInPopupMenu) {
            JMenuItem randomizeItem = getMenuRandomize();
            if (randomizeItem != null) {
                ret.add(randomizeItem);
            }
        }
        if (showNormalizeInPopupMenu) {
            JMenuItem normalizeItem = getMenuNormalize();
            if (normalizeItem != null) {
                ret.add(normalizeItem);
            }
        }
        if (showFillInPopupMenu) {
            JMenuItem fillItem = getMenuFill();
            if (fillItem != null) {
                ret.add(fillItem);
            }
        }
        if (showCSVInPopupMenu) {
            JMenuItem csvItem = getMenuCSV();
            if (csvItem != null) {
                ret.add(csvItem);
            }
        }
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
     * Return a toolbar with buttons for editing the rows of a table.
     *
     * @return the edit toolbar
     */
    public JToolBar getToolbarEditRows() {
        if (getData() instanceof MutableTable) {
            JToolBar toolbar = new JToolBar();
            toolbar.add(TableActionManager.getInsertRowAction(this));
            toolbar.add(TableActionManager.getDeleteRowAction(this));
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
            return menu;
        }
        return null;
    }

    /**
     * Select current row.
     */
    public void updateRowSelection() {
        if (getData() instanceof IterableRowsTable) {
            // TODO: If I don't call this, the line below does not work. Not
            // sure why.
            selectAll();
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
    TableModel getTableModel() {
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
                        label.setText("" + (row + 1));
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
                hasChangedSinceLastSave = true;
            }

            /**
             * {@inheritDoc}
             */
            public void columnRemoved(int column) {
                fireTableStructureChanged();
                fireTableDataChanged();
                hasChangedSinceLastSave = true;
            }

            /**
             * {@inheritDoc}
             */
            public void cellDataChanged(int row, int column) {
                fireTableCellUpdated(row, column);
                hasChangedSinceLastSave = true;
            }

            /**
             * {@inheritDoc}
             */
            public void rowAdded(int row) {
                fireTableRowsInserted(row, row);
                hasChangedSinceLastSave = true;
            }

            /**
             * {@inheritDoc}
             */
            public void rowRemoved(int row) {
                fireTableRowsDeleted(row, row);
                hasChangedSinceLastSave = true;
            }

            /**
             * {@inheritDoc}
             */
            public void tableStructureChanged() {
                fireTableStructureChanged();
                hasChangedSinceLastSave = true;
            }

            /**
             * {@inheritDoc}
             */
            public void tableDataChanged() {
                fireTableDataChanged();
                hasChangedSinceLastSave = true;
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
            if (data instanceof NumericTable) {
                return Double.class;
            } else if (data instanceof TextTable) {
                if (columnIndex > 0) {
                    return String.class;
                } else {
                    return Double.class;
                }
            }
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
                data.setValue(row, column - 1, value);
            }
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (!displayColumnHeadings) {
                return null;
            }
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

    /**
     * @return the displayColumnHeadings
     */
    public boolean isDisplayColumnHeadings() {
        return displayColumnHeadings;
    }

    /**
     * TODO: This only works for setting from true to false. Setting from false
     * back to true does not work.
     *
     * @param displayColumnHeadings the displayColumnHeadings to set
     */
    public void setDisplayColumnHeadings(boolean displayColumnHeadings) {
        this.displayColumnHeadings = displayColumnHeadings;
        if (displayColumnHeadings == false) {
            this.setTableHeader(null); // dubious method but worked
        }
    }

    /**
     * @return the displayPopUpMenu
     */
    public boolean isDisplayPopUpMenu() {
        return displayPopUpMenu;
    }

    /**
     * @param displayPopUpMenu the displayPopUpMenu to set
     */
    public void setDisplayPopUpMenu(boolean displayPopUpMenu) {
        this.displayPopUpMenu = displayPopUpMenu;
    }

    /**
     * @return the hasChanged
     */
    public boolean hasChanged() {
        return hasChangedSinceLastSave;
    }

    /**
     * @param hasChanged the hasChanged to set
     */
    public void setHasChangedSinceLastSave(boolean hasChanged) {
        this.hasChangedSinceLastSave = hasChanged;
    }

    /**
     * @param showInsertRowPopupMenu the showInsertRowPopupMenu to set
     */
    public void setShowInsertRowPopupMenu(boolean showInsertRowPopupMenu) {
        this.showInsertRowPopupMenu = showInsertRowPopupMenu;
    }

    /**
     * @param showInsertColumnPopupMenu the showInsertColumnPopupMenu to set
     */
    public void setShowInsertColumnPopupMenu(boolean showInsertColumnPopupMenu) {
        this.showInsertColumnPopupMenu = showInsertColumnPopupMenu;
    }

    /**
     * @param showDeleteRowPopupMenu the showDeleteRowPopupMenu to set
     */
    public void setShowDeleteRowPopupMenu(boolean showDeleteRowPopupMenu) {
        this.showDeleteRowPopupMenu = showDeleteRowPopupMenu;
    }

    /**
     * @param showDeleteColumnPopupMenu the showDeleteColumnPopupMenu to set
     */
    public void setShowDeleteColumnPopupMenu(boolean showDeleteColumnPopupMenu) {
        this.showDeleteColumnPopupMenu = showDeleteColumnPopupMenu;
    }

    /**
     * @param showEditInPopupMenu the showEditInPopupMenu to set
     */
    public void setShowEditInPopupMenu(boolean showEditInPopupMenu) {
        this.showEditInPopupMenu = showEditInPopupMenu;
    }

    /**
     * @param showRandomizeInPopupMenu the showRandomizeInPopupMenu to set
     */
    public void setShowRandomizeInPopupMenu(boolean showRandomizeInPopupMenu) {
        this.showRandomizeInPopupMenu = showRandomizeInPopupMenu;
    }

    /**
     * @param showNormalizeInPopupMenu the showNormalizeInPopupMenu to set
     */
    public void setShowNormalizeInPopupMenu(boolean showNormalizeInPopupMenu) {
        this.showNormalizeInPopupMenu = showNormalizeInPopupMenu;
    }

    /**
     * @param showFillInPopupMenu the showFillInPopupMenu to set
     */
    public void setShowFillInPopupMenu(boolean showFillInPopupMenu) {
        this.showFillInPopupMenu = showFillInPopupMenu;
    }

    /**
     * @param showCSVInPopupMenu the showCSVInPopupMenu to set
     */
    public void setShowCSVInPopupMenu(boolean showCSVInPopupMenu) {
        this.showCSVInPopupMenu = showCSVInPopupMenu;
    }

}
