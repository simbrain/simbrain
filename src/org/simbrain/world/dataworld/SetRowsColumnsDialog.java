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
    private DataWorldComponent component;

    /**
     * Dialog for setting the size of the data world.
     * @param dataComponent parent frame
     */
    public SetRowsColumnsDialog(final DataWorldComponent dataComponent) {
        setTitle("Set Rows/Columns");
        this.component = dataComponent;
        fillFieldValues();

        columnsField.setColumns(4);

        panel.addItem("Number of Rows", rowsField);
        panel.addItem("Number of Columns", columnsField);
        setContentPane(panel);
        pack();
    }

    /** @see StandardDialog */
    public void closeDialogOk() {
        component.getDataModel().modifyRowsColumns(Integer.parseInt(rowsField.getText()),
                Integer.parseInt(columnsField.getText()), new Double(0));
        super.closeDialogOk();
    }

    /**
     * Fills the fields with current values.
     */
    private void fillFieldValues() {
        columnsField.setText(Integer.toString(component.getDataModel().getColumnCount()));
        rowsField.setText(Integer.toString(component.getDataModel().getRowCount()));
    }

    /** @see StandardDialog */
    public void closeDialogCancel() {
        super.closeDialogCancel();
    }
}
