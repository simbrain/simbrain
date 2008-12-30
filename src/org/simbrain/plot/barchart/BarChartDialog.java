package org.simbrain.plot.barchart;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
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

    /** Reference to barchart model. */
    BarChartModel model;

    /** Currently selected color, or null if non has been selected. */
    private Color theColor = null;

    /**
     * Dialog for displaying chart options.
     * 
     * TODO: Remove chart as argument
     *
     * @param chart Reference to the calling chart
     */
    public BarChartDialog(final JFreeChart chart, final BarChartModel model) {
        this.chart = chart;
        LabelledItemPanel dialogPanel = new LabelledItemPanel();
        this.model = model;
        fillFieldValues();

        autoRange.addActionListener(this);
        autoRange.setActionCommand("AutoRange");

        dialogPanel.addItem("Maximum Range", maxRangeField);
        dialogPanel.addItem("Minimum Range", minRangeField);
        dialogPanel.addItem("Auto Range", autoRange);

        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(this);
        colorButton.setActionCommand("BarColor");
        dialogPanel.addItem("Bar Color", colorButton);

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
        if (theColor != null) {
        	model.setBarColor(theColor);
        }
        model.update();
    }
    
    /** @see ActionListener */
    public void actionPerformed(final ActionEvent arg0) {
        if (arg0.getActionCommand().equalsIgnoreCase("AutoRange")) {
            maxRangeField.setEnabled(!autoRange.isSelected());
            minRangeField.setEnabled(!autoRange.isSelected());
        } else if (arg0.getActionCommand().equalsIgnoreCase("BarColor")) {
            theColor = JColorChooser.showDialog(this, "Choose Color", model.getBarColor());        	
        }
    }
}
