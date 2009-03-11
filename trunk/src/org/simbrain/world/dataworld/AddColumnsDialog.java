package org.simbrain.world.dataworld;

import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * Dialog for adding multiple columns to data component.
 *
 */
public class AddColumnsDialog extends StandardDialog {

    /** Number of columns to be added. */
    private JTextField columnsField = new JTextField();

    /** Panel to add items. */
    private LabelledItemPanel panel = new LabelledItemPanel();

    /** Data world frame. */
    private DataWorldComponent component;

    /**
     * Dialog for adding columns to data world.
     * @param dataComponent parent component
     */
    public AddColumnsDialog(final DataWorldComponent dataComponent) {
        setTitle("Add Columns");
        this.component = dataComponent;
        fillFieldValues();

        columnsField.setColumns(4);

        panel.addItem("Number of Columns", columnsField);
        setContentPane(panel);
        pack();
        
    }

    /** @see StandardDialog */
    public void closeDialogOk() {
        component.getDataModel().addRowsColumns(0, Integer.parseInt(columnsField.getText()),
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
        columnsField.setText("0");
    }
}
