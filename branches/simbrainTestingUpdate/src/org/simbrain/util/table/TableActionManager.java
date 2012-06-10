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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

/**
 * Contains actions for use in SimbrainJTables.
 *
 * @author jyoshimi
 */
public class TableActionManager {

    /** Ddefault directory where tables are stored. */
    private static String CSV_DIRECTORY = "."
            + System.getProperty("file.separator") + "simulations"
            + System.getProperty("file.separator") + "tables";

    /**
     * Action for opening from comma separate value file.
     *
     * @param table table to load data in to
     * @return the action
     */
    public static Action getOpenCSVAction(final NumericTable table) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Open.png"));
                putValue(NAME, "Open (.csv)");
                putValue(SHORT_DESCRIPTION, "Open table from .csv");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                SFileChooser chooser = new SFileChooser(CSV_DIRECTORY,
                        "comma-separated-values (csv)", "csv");
                File theFile = chooser.showOpenDialog();
                if (theFile != null) {
                    table.readData(theFile);
                }
            }

        };
    }

    /**
     * Action for saving to comma separated value file.
     *
     * @param table table to load data in to
     * @return the action
     */
    public static Action getSaveCSVAction(final NumericTable table) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Save.png"));
                putValue(NAME, "Export (.csv)");
                putValue(SHORT_DESCRIPTION, "Save table as .csv");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                SFileChooser chooser = new SFileChooser(CSV_DIRECTORY,
                        "comma-separated-values (csv)", "csv");
                File theFile = chooser.showSaveDialog();
                if (theFile != null) {
                    Utils.writeMatrix(table.asStringArray(), theFile);
                }
            }

        };
    }

    /**
     * Action for randomizing table.
     *
     * @param table table to randomize
     * @return the action
     */
    public static Action getRandomizeAction(final NumericTable table) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Rand.png"));
                putValue(NAME, "Randomize");
                putValue(SHORT_DESCRIPTION, "Randomize Table");
                KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_R,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                putValue(ACCELERATOR_KEY, keyStroke);
            }

            /**
             * {@ineritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                table.randomize();
            }

        };
    }

    /**
     * Action for normalizing the currently selected column in a table.
     *
     * @param jtable table to normalize
     * @return the action
     */
    public static Action getNormalizeColumnAction(final NumericTable table,
            final int columnIndex) {
        return new AbstractAction() {

            // Initialize
            {
                // putValue(SMALL_ICON,
                // ResourceManager.getImageIcon("Rand.png"));
                putValue(NAME, "Normalize column");
                putValue(SHORT_DESCRIPTION, "Normalize column");
            }

            /**
             * {@ineritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                table.normalizeColumn(columnIndex);
            }

        };
    }

    /**
     * Action for normalizing a table.
     *
     * @param jtable table to normalize
     * @return the action
     */
    public static Action getNormalizeAction(final NumericTable table) {
        return new AbstractAction() {

            // Initialize
            {
                // putValue(SMALL_ICON,
                // ResourceManager.getImageIcon("Rand.png"));
                putValue(NAME, "Normalize table");
                putValue(SHORT_DESCRIPTION, "Normalize table");
                KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_N,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                putValue(ACCELERATOR_KEY, keyStroke);
            }

            /**
             * {@ineritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                table.normalizeTable();
            }

        };
    }

    /**
     * Action for setting table bounds.
     *
     * @param table table to adjust bounds on
     * @return the action
     */
    public static Action getSetTableBoundsAction(final NumericTable table) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.gif"));
                putValue(NAME, "Randomization bounds");
                putValue(SHORT_DESCRIPTION, "Set randomization bounds");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                StandardDialog dialog = new StandardDialog();
                JPanel pane = new JPanel();
                JTextField lower = new JTextField();
                JTextField upper = new JTextField();
                lower.setText(Integer.toString(table.getLowerBound()));
                lower.setColumns(3);
                upper.setText(Integer.toString(table.getUpperBound()));
                upper.setColumns(3);
                pane.add(new JLabel("Lower Bound"));
                pane.add(lower);
                pane.add(new JLabel("Upper Bound"));
                pane.add(upper);

                dialog.setContentPane(pane);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                if (!dialog.hasUserCancelled()) {
                    table.setLowerBound(Integer.parseInt(lower.getText()));
                    table.setUpperBound(Integer.parseInt(upper.getText()));
                }
            }

        };
    }

    /**
     * Action for setting table structure.
     *
     * @param table table to change structure of
     * @return the action
     */
    public static Action getChangeTableStructureAction(
            final SimbrainJTable table) {
        return new AbstractAction() {

            // Initialize
            {
                // TODO: Throw exception if jtable.getData() is not mutable
                // putValue(SMALL_ICON,
                // ResourceManager.getImageIcon("Prefs.gif"));
                putValue(NAME, "Reset table");
                putValue(SHORT_DESCRIPTION,
                        "Set number of rows and columns (cells are zeroed out)");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                StandardDialog dialog = new StandardDialog();
                JPanel pane = new JPanel();
                JTextField rows = new JTextField();
                JTextField columns = new JTextField();
                rows.setText(Integer.toString(table.getRowCount()));
                rows.setColumns(3);
                columns.setText(Integer.toString(table.getColumnCount()));
                columns.setColumns(3);
                pane.add(new JLabel("Rows"));
                pane.add(rows);
                pane.add(new JLabel("Columns"));
                pane.add(columns);

                dialog.setContentPane(pane);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                if (!dialog.hasUserCancelled()) {
                    ((MutableTable) table.getData()).reset(
                            Integer.parseInt(rows.getText()),
                            Integer.parseInt(columns.getText()));
                }
            }

        };
    }

    /**
     * Action for changing the number of rows and columns in the table.
     *
     * @param table table to change structure of
     * @return the action
     */
    public static Action changeRowsColumns(final SimbrainJTable table) {
        return new AbstractAction() {

            // Initialize
            {
                // putValue(SMALL_ICON,
                // ResourceManager.getImageIcon("Prefs.gif"));
                putValue(NAME, "Set rows / columns");
                putValue(SHORT_DESCRIPTION,
                        "Set number of rows and columns (cells are zeroed out)");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                StandardDialog dialog = new StandardDialog();
                JPanel pane = new JPanel();
                JTextField rows = new JTextField();
                JTextField columns = new JTextField();
                rows.setText(Integer.toString(table.getRowCount()));
                rows.setColumns(3);
                columns.setText(Integer.toString(table.getColumnCount()));
                columns.setColumns(3);
                pane.add(new JLabel("Rows"));
                pane.add(rows);
                pane.add(new JLabel("Columns"));
                pane.add(columns);

                dialog.setContentPane(pane);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                if (!dialog.hasUserCancelled()) {
                    ((MutableTable) table.getData()).modifyRowsColumns(
                            Integer.parseInt(rows.getText()),
                            Integer.parseInt(columns.getText()), 0);
                }
            }

        };
    }

    /**
     * Action for inserting a row in to a jtable.
     *
     * @param jtable table to insert row into
     * @return the action
     */
    public static Action getInsertRowAction(final SimbrainJTable table) {
        return new AbstractAction() {

            // Initialize
            {
                // TODO: Throw exception if jtable.getData() is not mutable
                putValue(SMALL_ICON,
                        ResourceManager.getImageIcon("AddTableRow.png"));
                putValue(NAME, "Insert row");
                putValue(SHORT_DESCRIPTION, "Insert row (above)");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                if (table.getSelectedRow() != -1) {
                    ((MutableTable) table.getData()).insertRow(
                            table.getSelectedRow(), new Double(0));
                }
            }

        };
    }

    /**
     * Action for inserting a column in to a jtable. Assumes the table is
     * mutable.
     *
     * @param jtable table to insert column into
     * @return the action
     */
    public static Action getInsertColumnAction(final SimbrainJTable jtable) {
        return new AbstractAction() {

            // Initialize
            {
                // TODO: Throw exception if jtable.getData() is not mutable
                putValue(SMALL_ICON,
                        ResourceManager.getImageIcon("AddTableColumn.png"));
                putValue(NAME, "Insert column");
                putValue(SHORT_DESCRIPTION, "Insert column (to right)");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                if (jtable.getSelectedColumn() != -1) {
                    ((MutableTable) jtable.getData()).insertColumn(
                            jtable.getSelectedColumn(), new Double(0));
                }
            }

        };
    }

    /**
     * Action for deleting a row from to a jtable.
     *
     * @param jtable table to delete a row from
     * @return the action
     */
    public static Action getDeleteRowAction(final SimbrainJTable jtable) {
        return new AbstractAction() {

            // Initialize
            {
                // TODO: Throw exception if jtable.getData() is not mutable
                putValue(SMALL_ICON,
                        ResourceManager.getImageIcon("DeleteRowTable.png"));
                putValue(NAME, "Delete row");
                putValue(SHORT_DESCRIPTION, "Delete row");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                // TODO: For this and others, if not use a sensible value (for
                // not selected case)
                // selection = getSelectedRow or else it =
                if (jtable.getSelectedRow() != -1) {
                    ((MutableTable) jtable.getData()).removeRow(jtable
                            .getSelectedRow());
                }
            }

        };
    }

    /**
     * Action for deleting a column from a jtable.
     *
     * @param jtable table to delete column from
     * @return the action
     */
    public static Action getDeleteColumnAction(final SimbrainJTable jtable) {
        return new AbstractAction() {

            // Initialize
            {
                // TODO: Throw exception if jtable.getData() is not mutable
                putValue(SMALL_ICON,
                        ResourceManager.getImageIcon("DeleteColumnTable.png"));
                putValue(NAME, "Delete column");
                putValue(SHORT_DESCRIPTION, "Delete column");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                if (jtable.getSelectedColumn() != -1) {
                    ((MutableTable) jtable.getData()).removeColumn(jtable
                            .getSelectedColumn() - 1);
                }
            }

        };
    }

    /**
     * Action for adding rows to a table.
     *
     * @param table table to add rows to
     * @return the action
     */
    public static Action getAddRowsAction(final MutableTable table) {
        return new AbstractAction() {

            // Initialize
            {
                // putValue(SMALL_ICON,
                // ResourceManager.getImageIcon("Eraser.png"));
                putValue(NAME, "Add rows");
                putValue(SHORT_DESCRIPTION, "Add rows");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                String numRows = JOptionPane.showInputDialog(null,
                        "Number of rows to add:", "5");
                table.addRows(Integer.parseInt(numRows), 0);
            }

        };
    }

    /**
     * Action for adding columns to a jtable.
     *
     * @param table table to insert column into
     * @return the action
     */
    public static Action getAddColumnsAction(final MutableTable table) {
        return new AbstractAction() {

            // Initialize
            {
                // putValue(SMALL_ICON,
                // ResourceManager.getImageIcon("Eraser.png"));
                putValue(NAME, "Add columns");
                putValue(SHORT_DESCRIPTION, "Add columns");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                String numCols = JOptionPane.showInputDialog(null,
                        "Number of columns to add:", "5");
                table.addColumns(Integer.parseInt(numCols), 0);
            }

        };
    }

    /**
     * Action for zeroing out cells of a table.
     *
     * @param table table to zero out
     * @return the action
     */
    public static Action getZeroFillAction(final NumericTable table) {
        return new AbstractAction() {

            // Initialize
            {
                // putValue(SMALL_ICON,
                // ResourceManager.getImageIcon("Eraser.png"));
                putValue(NAME, "Zero fill table");
                putValue(SHORT_DESCRIPTION, "Zero fill table");
                KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                putValue(ACCELERATOR_KEY, keyStroke);
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                table.fill(0);
            }

        };
    }

    /**
     * Action for filling a table with specific values.
     *
     * @param table table to fill
     * @return the action
     */
    public static Action getFillAction(final NumericTable table) {
        return new AbstractAction() {

            // Initialize
            {
                // putValue(SMALL_ICON,
                // ResourceManager.getImageIcon("Eraser.png"));
                putValue(NAME, "Fill table...");
                putValue(SHORT_DESCRIPTION, "Fill table with specified value");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                String val = JOptionPane.showInputDialog(null,
                        "Value:", "0");
                table.fill(Double.parseDouble(val));
            }

        };
    }

}
