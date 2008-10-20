package org.simbrain.plot.scatterplot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * Displays a dialog for setting of available plot parameters.
 *
 */
public class ScatterPlotDialog extends StandardDialog implements ActionListener {

    /** Reference to chart component. */
    private JFreeChart chart;

    /** Reference to the dot renderer. */
    private XYDotRenderer renderer;

    /** Auto Range check box. */
    private JCheckBox autoRange = new JCheckBox();

    /** Auto Domain check box. */
    private JCheckBox autoDomain = new JCheckBox();

    /** Text field for setting the maximum range. */
    private JTextField maxRangeField = new JTextField();

    /** Text field for setting the minimum range. */
    private JTextField minRangeField = new JTextField();

    /** Text field for setting the maximum domain. */
    private JTextField maxDomainField = new JTextField();

    /** Text field for setting the minimum domain. */
    private JTextField minDomainField = new JTextField();

    /** Text field for setting the dot size. */
    private JTextField dotSizeField = new JTextField();

    /**
     * Dialog for displaying chart options.
     *
     * @param chart Reference to the chart component to be changed
     * @param renderer Reference to the dot renderer
     */
    public ScatterPlotDialog(final JFreeChart chart, final XYDotRenderer renderer) {
        this.chart = chart;
        this.renderer = renderer;
        LabelledItemPanel dialogPanel = new LabelledItemPanel();

        fillFieldValues();

        autoRange.addActionListener(this);
        autoRange.setActionCommand("AutoRange");

        autoDomain.addActionListener(this);
        autoDomain.setActionCommand("AutoDomain");

        dialogPanel.addItem("Dot Size", dotSizeField);

        dialogPanel.addItem("Maximum Domain", maxDomainField);
        dialogPanel.addItem("Minimum Domain", minDomainField);
        dialogPanel.addItem("Auto Domain", autoDomain);

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
        dotSizeField.setText(Integer.toString(renderer.getDotHeight()));
        autoRange.setSelected(chart.getXYPlot().getRangeAxis()
                .isAutoRange());
        autoDomain.setSelected(chart.getXYPlot().getDomainAxis()
                .isAutoRange());
        maxRangeField.setText(Double.toString(chart.getXYPlot()
                .getRangeAxis().getRange().getUpperBound()));
        minRangeField.setText(Double.toString(chart.getXYPlot()
                .getRangeAxis().getRange().getLowerBound()));
        maxDomainField.setText(Double.toString(chart.getXYPlot()
                .getDomainAxis().getRange().getUpperBound()));
        minDomainField.setText(Double.toString(chart.getXYPlot()
                .getDomainAxis().getRange().getLowerBound()));

        maxRangeField.setEnabled(!autoRange.isSelected());
        minRangeField.setEnabled(!autoRange.isSelected());
        maxDomainField.setEnabled(!autoDomain.isSelected());
        minDomainField.setEnabled(!autoDomain.isSelected());

    }

    /**
     * Commits the changes.
     */
    private void commitChanges() {
        chart.getXYPlot().getRangeAxis().setAutoRange(
                autoRange.isSelected());
        chart.getXYPlot().getDomainAxis().setAutoRange(
                autoDomain.isSelected());
        if (!autoRange.isSelected()) {
            chart.getXYPlot().getRangeAxis().setRange(
                    Double.parseDouble(minRangeField.getText()),
                    Double.parseDouble(maxRangeField.getText()));
        }
        if (!autoDomain.isSelected()) {
            chart.getXYPlot().getDomainAxis().setRange(
                    Double.parseDouble(minDomainField.getText()),
                    Double.parseDouble(maxDomainField.getText()));
        }

        renderer.setDotHeight(Integer.parseInt(dotSizeField.getText()));
        renderer.setDotWidth(Integer.parseInt(dotSizeField.getText()));

    }

    /** @see ActionListener */
    public void actionPerformed(final ActionEvent arg0) {
        if (arg0.getActionCommand().equalsIgnoreCase("AutoRange")) {
            maxRangeField.setEnabled(!autoRange.isSelected());
            minRangeField.setEnabled(!autoRange.isSelected());
        } else if (arg0.getActionCommand().equalsIgnoreCase("AutoDomain")) {
            maxDomainField.setEnabled(!autoDomain.isSelected());
            minDomainField.setEnabled(!autoDomain.isSelected());
        }
    }
}
