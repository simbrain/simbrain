package org.simbrain.world.dataworld;

import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * Dialog for adding multiple rows to data component.
 *
 */
public class AddRowsDialog extends StandardDialog {

    /** Number of rows to be added. */
    private JTextField rowsField = new JTextField();

    /** Panel to add items. */
    private LabelledItemPanel panel = new LabelledItemPanel();

    /** Data world frame. */
    private DataWorldComponent component;

    /**
     * Dialog for adding rows to the data world.
     * @param dataComponent parent frame
     */
    public AddRowsDialog(final DataWorldComponent dataComponent) {
        setTitle("Add Rows");
        this.component = dataComponent;
        fillFieldValues();

        rowsField.setColumns(4);

        panel.addItem("Number of Rows", rowsField);
        setContentPane(panel);
        pack();
    }

    /** @see StandardDialog */
    public void closeDialogOk() {
        component.getDataModel().addRowsColumns(Integer.parseInt(rowsField.getText()), 0,
                new Double(0));
        super.closeDialogOk();
    }

    /** @see StandardDialog */
    public void closeDialogCancel() {
        super.closeDialogCancel();
    }

    /**
     * Fills the field with initial values.
     */
    private void fillFieldValues() {
        rowsField.setText("0");
    }
}
