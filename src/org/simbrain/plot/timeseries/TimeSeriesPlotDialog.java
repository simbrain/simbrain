package org.simbrain.plot.timeseries;

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
public class TimeSeriesPlotDialog extends StandardDialog implements ActionListener {

    /** Reference to chart component. */
    private JFreeChart chart;

    /** Auto Range check box. */
    private JCheckBox autoRangeBox = new JCheckBox();

    /** Auto Domain check box. */
    private JCheckBox autoDomainBox = new JCheckBox();

    /** Use fixed range. */
    private JCheckBox fixedRangeBox = new JCheckBox();

    /** Fixed range size. */
    private JTextField fixedRangeField = new JTextField();

    /** Text field for setting the maximum range. */
    private JTextField maxRangeField = new JTextField();

    /** Text field for setting the minimum range. */
    private JTextField minRangeField = new JTextField();

    /** Text field for setting the maximum domain. */
    private JTextField maxDomainField = new JTextField();

    /** Text field for setting the minimum domain. */
    private JTextField minDomainField = new JTextField();

    /** Time series model. */
    private TimeSeriesModel model;


    /**
     * Dialog for displaying chart options.
     *
     * @param chart Reference to the chart component to be changed
     * @param model Reference to the model
     */
    public TimeSeriesPlotDialog(final JFreeChart chart, final TimeSeriesModel model) {
        this.chart = chart;
        this.model = model;

        LabelledItemPanel dialogPanel = new LabelledItemPanel();

        fillFieldValues();

        autoRangeBox.addActionListener(this);
        autoRangeBox.setActionCommand("AutoRange");

        autoDomainBox.addActionListener(this);
        autoDomainBox.setActionCommand("AutoDomain");

        fixedRangeBox.addActionListener(this);
        fixedRangeBox.setActionCommand("FixedRange");

        dialogPanel.addItem("Window Size", fixedRangeField);
        dialogPanel.addItem("Fix Window Size", fixedRangeBox);

        dialogPanel.addItem("Maximum Domain", maxDomainField);
        dialogPanel.addItem("Minimum Domain", minDomainField);
        dialogPanel.addItem("Auto Domain", autoDomainBox);

        dialogPanel.addItem("Maximum Range", maxRangeField);
        dialogPanel.addItem("Minimum Range", minRangeField);
        dialogPanel.addItem("Auto Range", autoRangeBox);

        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(this);
        colorButton.setActionCommand("BarColor");

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
        autoRangeBox.setSelected(model.isAutoRange());
        autoDomainBox.setSelected(model.isAutoDomain());
        maxRangeField.setText(Double.toString(model.getUpperRangeBoundary()));
        minRangeField.setText(Double.toString(model.getLowerRangeBoundary()));
        maxDomainField.setText(Double.toString(model.getUpperDomainBoundary()));
        minDomainField.setText(Double.toString(model.getLowerDomainBoundary()));

        checkEnableFields();

        if (model.isFixedWindow()) {
            fixedRangeBox.setSelected(true);
            autoDomainBox.setEnabled(false);
            fixedRangeField.setText(Double.toString(chart.getXYPlot()
                    .getDomainAxis().getFixedAutoRange()));
        } else {
            fixedRangeBox.setSelected(false);
            fixedRangeField.setEnabled(false);
        }
    }

    /**
     * Checks fields for their enable status.
     */
    private void checkEnableFields() {
        maxRangeField.setEnabled(!autoRangeBox.isSelected());
        minRangeField.setEnabled(!autoRangeBox.isSelected());
        maxDomainField.setEnabled(!autoDomainBox.isSelected());
        minDomainField.setEnabled(!autoDomainBox.isSelected());
    }

    /**
     * Commits the changes.
     */
    private void commitChanges() {
        chart.getXYPlot().getRangeAxis().setAutoRange(
                autoRangeBox.isSelected());
        chart.getXYPlot().getDomainAxis().setAutoRange(
                autoDomainBox.isSelected());
        if (!autoRangeBox.isSelected()) {
            chart.getXYPlot().getRangeAxis().setRange(
                    Double.parseDouble(minRangeField.getText()),
                    Double.parseDouble(maxRangeField.getText()));
        }
        if (!autoDomainBox.isSelected()) {
            chart.getXYPlot().getDomainAxis().setRange(
                    Double.parseDouble(minDomainField.getText()),
                    Double.parseDouble(maxDomainField.getText()));
        }
        if (fixedRangeBox.isSelected()) {
            chart.getXYPlot().getDomainAxis().setFixedAutoRange(
                    Double.parseDouble(fixedRangeField.getText()));
        } else {
            chart.getXYPlot().getDomainAxis().setFixedAutoRange(-1);
        }
        model.getParent().setFixedWidth(fixedRangeBox.isSelected());
        model.getParent().setMaxSize(Integer.parseInt(fixedRangeField.getText()));
    }

    /** @see ActionListener */
    public void actionPerformed(final ActionEvent arg0) {
        if (arg0.getActionCommand().equalsIgnoreCase("AutoRange")) {
             checkEnableFields();
        } else if (arg0.getActionCommand().equalsIgnoreCase("AutoDomain")) {
            checkEnableFields();
        } else if (arg0.getActionCommand().equalsIgnoreCase("FixedRange")) {
            fixedRangeField.setEnabled(fixedRangeBox.isSelected());
            fixedRangeField.setText(Integer.toString(model.getParent().getMaxSize()));
            if (fixedRangeBox.isSelected()) {
                autoDomainBox.setSelected(fixedRangeBox.isSelected());
            } else {
                autoDomainBox.setSelected(chart.getXYPlot().getDomainAxis().isAutoRange());
            }
            autoDomainBox.setEnabled(!fixedRangeBox.isSelected());
        }
    }
}
