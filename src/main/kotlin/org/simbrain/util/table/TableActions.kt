package org.simbrain.util.table

import org.simbrain.util.ResourceManager
import org.simbrain.util.SFileChooser
import org.simbrain.util.StandardDialog
import org.simbrain.util.Utils
import smile.plot.swing.BoxPlot
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.*

/**
 * Default directory where tables are stored.
 */
private val CSV_DIRECTORY = "." + Utils.FS + "simulations" + Utils.FS + "tables"

/**
 * Action for opening from comma separate value file.
 *
 * @param table              table to load data in to
 * @param allowRowChanges    whether to allow number of rows to change
 * @param allowColumnChanges whether to allow number of columns to change
 * @return the action
 */
fun getOpenCSVAction(table: NumericTable, allowRowChanges: Boolean, allowColumnChanges: Boolean): Action {
    return object : AbstractAction() {
        /**
         * {@inheritDoc}
         */
        override fun actionPerformed(arg0: ActionEvent) {
            val chooser = SFileChooser(CSV_DIRECTORY, "comma-separated-values (csv)", "csv")
            val theFile = chooser.showOpenDialog()
            if (theFile != null) {
                try {
                    table.readData(theFile, allowRowChanges, allowColumnChanges)
                } catch (e: TableDataException) {
                    JOptionPane.showOptionDialog(
                        null,
                        e.message,
                        "Warning",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        null,
                        null
                    )
                }
            }
        }

        // Initialize
        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Open.png"))
            putValue(NAME, "Import (.csv)")
            putValue(SHORT_DESCRIPTION, "Import table from .csv")
        }
    }
}

/**
 * Action for opening text table from comma separated value file.
 *
 * @param table              table to load data in to
 * @param allowRowChanges    whether to allow number of rows to change
 * @param allowColumnChanges whether to allow number of columns to change
 * @return the action
 */
fun getOpenCSVAction(table: TextTable, allowRowChanges: Boolean, allowColumnChanges: Boolean): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            val chooser = SFileChooser(CSV_DIRECTORY, "comma-separated-values (csv)", "csv")
            val theFile = chooser.showOpenDialog()
            if (theFile != null) {
                try {
                    table.readData(theFile, allowRowChanges, allowColumnChanges)
                } catch (e: TableDataException) {
                    JOptionPane.showOptionDialog(
                        null,
                        e.message,
                        "Warning",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        null,
                        null
                    )
                }
            }
        }

        // Initialize
        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Open.png"))
            putValue(NAME, "Import (.csv)")
            putValue(SHORT_DESCRIPTION, "Import table from .csv")
        }
    }
}

/**
 * Action for saving to comma separated value file.
 *
 * @param table table to load data in to
 * @return the action
 */
fun getSaveCSVAction(table: SimbrainDataTable<*>): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            val chooser = SFileChooser(CSV_DIRECTORY, "comma-separated-values (csv)", "csv")
            val theFile = chooser.showSaveDialog()
            if (theFile != null) {
                Utils.writeMatrix(table.asStringArray(), theFile)
            }
        }

        // Initialize
        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Save.png"))
            putValue(NAME, "Export (.csv)")
            putValue(SHORT_DESCRIPTION, "Save table as .csv")
        }
    }
}

fun SimbrainJTable.getRandomizeAction(): Action {
    return object : AbstractAction() {

        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Rand.png"))
            putValue(NAME, "Randomize")
            putValue(SHORT_DESCRIPTION, "Randomize selected cells")
            putValue(ACCELERATOR_KEY, KeyEvent.META_DOWN_MASK + KeyEvent.VK_R)
        }

        override fun actionPerformed(arg0: ActionEvent) {
            randomize()
        }
    }
}


// TODO: Temp
fun getShowPlotAction(model: DataFrameWrapper): Action {
    return object : AbstractAction() {

        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/PieChart.png"))
            putValue(NAME, "Show plots")
            putValue(SHORT_DESCRIPTION, "Create histograms and other plots...")
        }

        override fun actionPerformed(arg0: ActionEvent) {
            val canvas = BoxPlot.of(model.df.doubleVector(0).toDoubleArray()).canvas();
            canvas.window()
        }
    }
}


/**
 * Action for normalizing selected parts of a table.
 *
 * @param table table to normalize
 * @return the action
 */
fun getNormalizeAction(table: SimbrainJTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            table.normalize()
        }

        // Initialize
        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/histogram.png"))
            putValue(NAME, "Normalize Column(s)")
            putValue(SHORT_DESCRIPTION, "Normalize Selected Columns")
            val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().menuShortcutKeyMask)
            // putValue(ACCELERATOR_KEY, org.simbrain.util.table.keyStroke)
        }
    }
}

/**
 * Action for setting table bounds.
 *
 * @param table table to adjust bounds on
 * @return the action
 */
fun getSetTableBoundsAction(table: NumericTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            val dialog = StandardDialog()
            val pane = JPanel()
            val lower = JTextField()
            val upper = JTextField()
            lower.text = Integer.toString(table.lowerBound)
            lower.columns = 3
            upper.text = Integer.toString(table.upperBound)
            upper.columns = 3
            pane.add(JLabel("Lower Bound"))
            pane.add(lower)
            pane.add(JLabel("Upper Bound"))
            pane.add(upper)
            dialog.contentPane = pane
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
            if (!dialog.hasUserCancelled()) {
                table.lowerBound = lower.text.toInt()
                table.upperBound = upper.text.toInt()
            }
        }

        // Initialize
        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Prefs.gif"))
            putValue(NAME, "Randomization bounds")
            putValue(SHORT_DESCRIPTION, "Set randomization bounds")
        }
    }
}

/**
 * Action for setting table structure.
 *
 * @param table table to change structure of
 * @return the action
 */
fun getChangeTableStructureAction(table: SimbrainJTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            val dialog = StandardDialog()
            val pane = JPanel()
            val rows = JTextField()
            val columns = JTextField()
            rows.text = Integer.toString(table.rowCount)
            rows.columns = 3
            columns.text = Integer.toString(table.columnCount)
            columns.columns = 3
            pane.add(JLabel("Rows"))
            pane.add(rows)
            pane.add(JLabel("Columns"))
            pane.add(columns)
            dialog.contentPane = pane
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
            if (!dialog.hasUserCancelled()) {
                (table.data as MutableTable<*>).reset(rows.text.toInt(), columns.text.toInt())
            }
        }

        // Initialize
        init {
            // TODO: Throw exception if jtable.getData() is not mutable
            // putValue(SMALL_ICON,
            // ResourceManager.getImageIcon("Prefs.gif"));
            putValue(NAME, "Reset table")
            putValue(SHORT_DESCRIPTION, "Set number of rows and columns (cells are zeroed out)")
        }
    }
}

/**
 * Action for changing the number of rows and columns in the table.
 *
 * @param table table to change structure of
 * @return the action
 */
fun changeRowsColumns(table: SimbrainJTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            val dialog = StandardDialog()
            val pane = JPanel()
            val rows = JTextField()
            val columns = JTextField()
            rows.text = Integer.toString(table.data.rowCount)
            rows.columns = 3
            columns.text = Integer.toString(table.data.logicalColumnCount)
            columns.columns = 3
            pane.add(JLabel("Rows"))
            pane.add(rows)
            pane.add(JLabel("Columns"))
            pane.add(columns)
            dialog.contentPane = pane
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
            if (!dialog.hasUserCancelled()) {
                (table.data as MutableTable<Double>).modifyRowsColumns(rows.text.toInt(), columns.text.toInt(), 0.0)
            }
        }

        // Initialize
        init {
            // putValue(SMALL_ICON,
            // ResourceManager.getImageIcon("Prefs.gif"));
            putValue(NAME, "Set rows / columns")
            putValue(SHORT_DESCRIPTION, "Set number of rows and columns (cells are zeroed out)")
        }
    }
}

/**
 * Action for inserting a row in to a jtable.
 *
 * @param table jtable to insert row into.
 * @return the action
 */
fun getInsertRowAction(table: SimbrainJTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            if (table.selectedRow != -1) {
                if (table.data is MutableTable<*>) {
                    (table.data as MutableTable<*>).insertRow(table.selectedRow)
                }
            }
        }

        // Initialize
        init {
            // TODO: Throw exception if jtable.getData() is not mutable
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/AddTableRow.png"))
            putValue(NAME, "Insert row")
            putValue(SHORT_DESCRIPTION, "Insert row (above)")
        }
    }
}

/**
 * Action for inserting a column in to a jtable. Assumes the table is
 * mutable.
 *
 * @param jtable table to insert column into
 * @return the action
 */
fun getInsertColumnAction(jtable: SimbrainJTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            if (jtable.selectedColumn != -1) {
                (jtable.data as MutableTable<*>).insertColumn(jtable.selectedColumn)
            }
        }

        // Initialize
        init {
            // TODO: Throw exception if jtable.getData() is not mutable
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/AddTableColumn.png"))
            putValue(NAME, "Insert column")
            putValue(SHORT_DESCRIPTION, "Insert column (to right)")
        }
    }
}

/**
 * Action for deleting a row from to a jtable.
 *
 * @param jtable table to delete a row from
 * @return the action
 */
fun getDeleteRowAction(jtable: SimbrainJTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            // TODO: Also allow multiple column selection using this method?
            val selection: MutableList<Int> = ArrayList(0)
            for (i in jtable.selectedRows.indices) {
                selection.add(jtable.selectedRows[i])
            }
            Collections.sort(selection, Collections.reverseOrder())
            if (selection.size > 0) {
                for (i in selection) {
                    (jtable.data as MutableTable<*>).removeRow(i)
                }
            }
            // Rule for selecting row after deleting a row. Needs work.
            // Should work well when button is repeatedly pressed
            if (selection.size > 0) {
                val newSelection = selection[selection.size - 1] - 1
                if (newSelection >= 0) {
                    jtable.setRowSelectionInterval(newSelection, newSelection)
                }
            }
        }

        // Initialize
        init {
            // TODO: Throw exception if jtable.getData() is not mutable
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/DeleteRowTable.png"))
            putValue(NAME, "Delete row")
            putValue(SHORT_DESCRIPTION, "Delete row")
        }
    }
}

/**
 * Action for deleting a column from a jtable.
 *
 * @param jtable table to delete column from
 * @return the action
 */
fun getDeleteColumnAction(jtable: SimbrainJTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            if (jtable.selectedColumn != -1) {
                (jtable.data as MutableTable<*>).removeColumn(jtable.selectedColumn - 1)
            }
        }

        // Initialize
        init {
            // TODO: Throw exception if jtable.getData() is not mutable
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/DeleteColumnTable.png"))
            putValue(NAME, "Delete column")
            putValue(SHORT_DESCRIPTION, "Delete column")
        }
    }
}

/**
 * Action for adding rows to a table.
 *
 * @param table table to add rows to
 * @return the action
 */
fun getAddRowsAction(table: MutableTable<*>): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            val numRows = JOptionPane.showInputDialog(null, "Number of rows to add:", "5")
            table.addRows(numRows.toInt())
        }

        // Initialize
        init {
            // putValue(SMALL_ICON,
            // ResourceManager.getImageIcon("Eraser.png"));
            putValue(NAME, "Add rows")
            putValue(SHORT_DESCRIPTION, "Add rows")
        }
    }
}

/**
 * Action for adding columns to a jtable.
 *
 * @param table table to insert column into
 * @return the action
 */
fun getAddColumnsAction(table: MutableTable<*>): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            val numCols = JOptionPane.showInputDialog(null, "Number of columns to add:", "5")
            table.addColumns(numCols.toInt())
        }

        // Initialize
        init {
            // putValue(SMALL_ICON,
            // ResourceManager.getImageIcon("Eraser.png"));
            putValue(NAME, "Add columns")
            putValue(SHORT_DESCRIPTION, "Add columns")
        }
    }
}

/**
 * Action for zeroing out cells of a table.
 *
 * @param table table to zero out
 * @return the action
 */
fun getZeroFillAction(table: SimbrainJTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            table.fill(0.0)
        }

        // Initialize
        init {
            // putValue(SMALL_ICON,
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Eraser.png"))
            putValue(NAME, "Zero fill cells")
            putValue(SHORT_DESCRIPTION, "Zero fill selected cells")
            val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().menuShortcutKeyMask)
            // putValue(ACCELERATOR_KEY, org.simbrain.util.table.keyStroke)
        }
    }
}

/**
 * Action for filling a table with specific values.
 *
 * @param table table to fill
 * @return the action
 */
fun getFillAction(table: SimbrainJTable): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            table.fill()
        }

        // Initialize
        init {
            // putValue(SMALL_ICON,
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/fill.png"))
            putValue(NAME, "Fill table cells...")
            putValue(SHORT_DESCRIPTION, "Fill table selected cells with specified value")
        }
    }
}

/**
 * Action for shuffling the rows of a table.
 *
 * @param table table whose rows should be shuffled
 * @return the action
 */
fun getShuffleAction(table: SimbrainDataTable<*>): Action {
    return object : AbstractAction() {
        override fun actionPerformed(arg0: ActionEvent) {
            table.shuffle()
        }

        // Initialize
        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Shuffle.png"))
            putValue(NAME, "Shuffle rows")
            putValue(SHORT_DESCRIPTION, "Randomize the positions of the rows")
        }
    }
}
