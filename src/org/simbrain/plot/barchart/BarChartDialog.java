package org.simbrain.plot.barchart;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.jfree.chart.JFreeChart;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * Displays a dialog for setting of available plot parameters.
 *
 */
public class BarChartDialog extends StandardDialog implements ActionListener {

    /** Reference to calling component. */
    private JFreeChart chart;

    /** Auto Range check box. */
    private JCheckBox autoRange = new JCheckBox();

    /** Text field for setting the maximum range. */
    private JTextField maxRangeField = new JTextField();

    /** Text field for setting the minimum range. */
    private JTextField minRangeField = new JTextField();

    /**
     * Dialog for displaying chart options.
     *
     * @param chart Reference to the calling chart
     */
    public BarChartDialog(final JFreeChart chart) {
        this.chart = chart;
        LabelledItemPanel dialogPanel = new LabelledItemPanel();

        fillFieldValues();

        autoRange.addActionListener(this);
        autoRange.setActionCommand("AutoRange");

        dialogPanel.addItem("Maximum Range", maxRangeField);
        dialogPanel.addItem("Minimum Range", minRangeField);
        dialogPanel.addItem("Auto Range", autoRange);

        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(this);
        colorButton.setActionCommand("BarColor");
//        dialogPanel.addItem("Bar Color", colorButton);

        setContentPane(dialogPanel);
        setResizable(false);
    }

    /** @see StandardDialog */
    protected void closeDialogOk() {
        super.closeDialogOk();
        this.commitChanges();
    }

    /** @see StandardDialog */
    protected void closeDialogCancel() {
        super.closeDialogCancel();
    }

    /**
     * Fills the fields with current values.
     */
    private void fillFieldValues() {
        autoRange.setSelected(chart.getCategoryPlot().getRangeAxis()
                .isAutoRange());
        maxRangeField.setText(Double.toString(chart.getCategoryPlot()
                .getRangeAxis().getRange().getUpperBound()));
        minRangeField.setText(Double.toString(chart.getCategoryPlot()
                .getRangeAxis().getRange().getLowerBound()));

    }

    /**
     * Commits the changes.
     */
    private void commitChanges() {
        chart.getCategoryPlot().getRangeAxis().setAutoRange(
                autoRange.isSelected());
        if (!autoRange.isSelected()) {
            chart.getCategoryPlot().getRangeAxis().setRange(
                    Double.parseDouble(minRangeField.getText()),
                    Double.parseDouble(maxRangeField.getText()));
        }
    }

    /** @see ActionListener */
    public void actionPerformed(final ActionEvent arg0) {
        if (arg0.getActionCommand().equalsIgnoreCase("AutoRange")) {
            maxRangeField.setEnabled(!autoRange.isSelected());
            minRangeField.setEnabled(!autoRange.isSelected());
        }
    }
}
