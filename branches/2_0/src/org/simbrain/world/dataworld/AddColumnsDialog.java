package org.simbrain.world.dataworld;

import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

public class AddColumnsDialog extends StandardDialog {

    /** Number of columns to be added. */
    private JTextField columnsField = new JTextField();

    /** Panel to add items. */
    private LabelledItemPanel panel = new LabelledItemPanel();

    /** Data world frame. */
    private DataWorldFrame worldFrame;

    /**
     * Dialog for adding columns to data world.
     * @param worldFrame parent frame
     */
    public AddColumnsDialog(final DataWorldFrame worldFrame) {
        setTitle("Add Columns");
        this.worldFrame = worldFrame;
        fillFieldValues();

        columnsField.setColumns(4);

        panel.addItem("Number of Columns", columnsField);
        setContentPane(panel);
        
    }

    /** @see StandardDialog */
    public void closeDialogOk() {
        int numColumns = Integer.parseInt(columnsField.getText());
        for (int i = 0; i < numColumns; ++i) {
            worldFrame.getWorld().getModel().addColumn(Integer.toString(worldFrame.getWorld().getModel().getColumnCount()));
            worldFrame.getWorld().getModel().zeroFillNew();
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
        columnsField.setText("0");
    }
}
