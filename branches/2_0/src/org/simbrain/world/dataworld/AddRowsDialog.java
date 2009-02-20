package org.simbrain.world.dataworld;

import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

public class AddRowsDialog extends StandardDialog {

    /** Number of rows to be added. */
    private JTextField rowsField = new JTextField();

    /** Panel to add items. */
    private LabelledItemPanel panel = new LabelledItemPanel();

    /** Data world frame. */
    private DataWorldFrame worldFrame;

    /**
     * Dialog for adding rows to the data world.
     * @param worldFrame parent frame
     */
    public AddRowsDialog(final DataWorldFrame worldFrame) {
        setTitle("Add Rows");
        this.worldFrame = worldFrame;
        fillFieldValues();

        rowsField.setColumns(4);

        panel.addItem("Number of Rows", rowsField);
        setContentPane(panel);
        
    }

    /** @see StandardDialog */
    public void closeDialogOk() {
        int numRows = Integer.parseInt(rowsField.getText());
        for (int i = 0; i < numRows; ++i) {
            worldFrame.getWorld().getModel().addRow(worldFrame.getWorld().getModel().newRow());
        }
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
