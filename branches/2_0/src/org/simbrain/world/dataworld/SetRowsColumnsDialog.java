package org.simbrain.world.dataworld;

import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

public class SetRowsColumnsDialog extends StandardDialog {

    /** Number of columns. */
    private JTextField columnsField = new JTextField();

    /** Number of rows. */
    private JTextField rowsField = new JTextField();

    /** Panel to add items. */
    private LabelledItemPanel panel = new LabelledItemPanel();

    /** Data world frame. */
    private DataWorldFrame worldFrame;

    /** Current number of rows. */
    private int currentRowCount;

    /** Current number of columns. */
    private int currentColumnCount;

    /**
     * Dialog for setting the size of the data world.
     * @param worldFrame parent frame
     */
    public SetRowsColumnsDialog(final DataWorldFrame worldFrame) {
        setTitle("Set Rows/Columns");
        this.worldFrame = worldFrame;
        currentRowCount = worldFrame.getWorld().getModel().getRowCount();
        currentColumnCount = worldFrame.getWorld().getModel().getColumnCount() - 1;
        fillFieldValues();

        columnsField.setColumns(4);

        panel.addItem("Number of Rows", rowsField);
        panel.addItem("Number of Columns", columnsField);
        setContentPane(panel);
        
    }

    public void closeDialogOk() {
        int numColumns = Integer.parseInt(columnsField.getText());
        int numRows = Integer.parseInt(rowsField.getText());
        if (numColumns > currentColumnCount) {
            for (int i = 0; i < numColumns - currentColumnCount; ++i) {
                worldFrame.getWorld().getModel().addColumn(Integer.toString(worldFrame.getWorld().getModel().getColumnCount()));
                worldFrame.getWorld().getModel().zeroFillNew();
            }
        } else if (numColumns < currentColumnCount) {
            for (int i = 0; i < currentColumnCount - numColumns; ++i) {
                worldFrame.getWorld().getModel().removeColumn(worldFrame.getWorld().getModel().getColumnCount() - 1);
            }
        } 

        if (numRows > currentRowCount) {
            for (int i = 0; i < numRows - currentColumnCount; ++i) {
                worldFrame.getWorld().getModel().addRow(worldFrame.getWorld().getModel().newRow());
            }
        } else if (numRows < currentRowCount) {
            for (int i = 0; i < currentRowCount - numRows; ++i) {
                worldFrame.getWorld().getModel().removeRow(worldFrame.getWorld().getTable().getRowCount() - 1);
            }
        }
        super.closeDialogOk();
    }

    private void fillFieldValues() {
        columnsField.setText(Integer.toString(currentColumnCount));
        rowsField.setText(Integer.toString(currentRowCount));
    }

    public void closeDialogCancel() {
        super.closeDialogCancel();
    }
}
