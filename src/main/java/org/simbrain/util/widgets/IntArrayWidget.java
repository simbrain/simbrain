package org.simbrain.util.widgets;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * A parameter widget to edit integer arrays.
 */
public class IntArrayWidget extends JPanel {

    // TODO: Abstract between this and DoubleArrayWidget?
    // TODO: Write values as soon as cell is changed (now must click outside a cell)

    /**
     * The table component
     */
    private JTable table = new JTable();

    /**
     * The table model
     */
    private DefaultTableModel model;

    /**
     * Construct a double array widget with 10 rows.
     */
    public IntArrayWidget() {
        model = new DefaultTableModel(10, 1);
        table.setModel(model);
        table.setGridColor(Color.gray);
        table.setBorder(BorderFactory.createLineBorder(Color.black));
        add(table);
    }

    /**
     * Get values of this widget.
     * @return the values
     */
    public int[] getValues() {
        int[] vals = new int[model.getRowCount()];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = Integer.parseInt(model.getValueAt(i,0).toString());
        }
        return vals;
    }

    /**
     * Set values for this widget and update the model and visual.
     * @param values the values to set
     */
    public void setValues(int[] values) {
        if (values.length != model.getRowCount()) {
            model = new DefaultTableModel(values.length, 1);
        }
        for (int i = 0; i < values.length; i++) {
            model.setValueAt(values[i], i, 0);
        }
        table.setModel(model);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        table.setEnabled(enabled);
        if (!enabled) {
            table.setForeground(Color.gray);
        }
    }
}
