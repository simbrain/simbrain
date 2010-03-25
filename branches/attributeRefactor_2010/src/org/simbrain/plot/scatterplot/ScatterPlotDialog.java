package org.simbrain.plot.scatterplot;

import java.awt.Color;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
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

    /** Auto Range check box. */
    private JCheckBox autoRange = new JCheckBox();

    /** Auto Domain check box. */
    private JCheckBox autoDomain = new JCheckBox();

    /** Show the history of the plot. */
    private JCheckBox showHistory = new JCheckBox();

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

    /** Scatter plot model. */
    private ScatterPlotModel model;

    /** Series color. */
    private List<Paint> theColor;

    /** Color indicator panel. */
    private JPanel colorIndicator = new JPanel();

    /** Series color selector. */
    private JComboBox seriesColorSelector;

    /**
     * Dialog for displaying chart options.
     *
     * @param chart Reference to the chart component to be changed
     * @param model Scatter plot model
     */
    public ScatterPlotDialog(final JFreeChart chart, final ScatterPlotModel model) {
        this.model = model;
        theColor = model.getChartSeriesPaint();
        LabelledItemPanel dialogPanel = new LabelledItemPanel();

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

        dialogPanel.addItem("Show History", showHistory);

        // Color Panel
        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(this);
        colorButton.setActionCommand("ShowColorDialog");
        String[] seriesLabel = new String[model.getChartSeriesPaint().size()];
        for (int i = 0; i < model.getChartSeriesPaint().size(); ++i) {
            seriesLabel[i] = Integer.toString(i);
        }
        seriesColorSelector = new JComboBox(seriesLabel);
        seriesColorSelector.addActionListener(this);
        seriesColorSelector.setActionCommand("SeriesColorSelector");
        colorIndicator.setSize(20, 20);
        JPanel colorPanel = new JPanel();
        colorPanel.add(seriesColorSelector);
        colorPanel.add(colorButton);
        colorPanel.add(colorIndicator);
        
        dialogPanel.addItem("Series Color:", colorPanel);

        checkBoundaryFields();
        fillFieldValues();

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
     * Updates the boundary fields.
     */
    private void checkBoundaryFields() {
        maxRangeField.setEnabled(!model.isAutoRange());
        minRangeField.setEnabled(!model.isAutoRange());
        maxDomainField.setEnabled(!model.isAutoDomain());
        minDomainField.setEnabled(!model.isAutoDomain());
    }

    /**
     * Fills the fields with current values.
     */
    private void fillFieldValues() {
        dotSizeField.setText(Integer.toString(model.getDotSize()));
        autoRange.setSelected(model.isAutoRange());
        autoDomain.setSelected(model.isAutoRange());
        showHistory.setSelected(model.isShowHistory());
        maxRangeField.setText(Double.toString(model.getUpperRangeBoundary()));
        minRangeField.setText(Double.toString(model.getLowerRangeBoundary()));
        maxDomainField.setText(Double.toString(model.getUpperDomainBoundary()));
        minDomainField.setText(Double.toString(model.getLowerDomainBoundary()));
        setIndicatorColor();
    }

    /**
     * Commits the changes.
     */
    private void commitChanges() {
        model.setAutoRange(autoRange.isSelected());
        model.setAutoDomain(autoDomain.isSelected());
        if (!autoRange.isSelected()) {
            model.setUpperRangeBoundary(Double.parseDouble(maxRangeField.getText()));
            model.setLowerRangeBoundary(Double.parseDouble(minRangeField.getText()));
        }
        if (!autoDomain.isSelected()) {
            model.setUpperDomainBoundary(Double.parseDouble(maxDomainField.getText()));
            model.setLowerDomainBoundary(Double.parseDouble(minDomainField.getText()));
        }

        model.setDotSize(Integer.parseInt(dotSizeField.getText()));
        model.setChartSeriesPaint(theColor);

        model.setShowHistory(showHistory.isSelected());
    }

    /** @see ActionListener */
    public void actionPerformed(final ActionEvent arg0) {
        if (arg0.getActionCommand().equalsIgnoreCase("AutoRange")) {
            maxRangeField.setEnabled(!autoRange.isSelected());
            minRangeField.setEnabled(!autoRange.isSelected());
        } else if (arg0.getActionCommand().equalsIgnoreCase("AutoDomain")) {
            maxDomainField.setEnabled(!autoDomain.isSelected());
            minDomainField.setEnabled(!autoDomain.isSelected());
        } else if (arg0.getActionCommand().equalsIgnoreCase("ShowColorDialog")) {
            theColor.set(seriesColorSelector.getSelectedIndex(), JColorChooser.showDialog(
                    this, "Series Color", colorIndicator.getBackground()));
            setIndicatorColor();
        } else if (arg0.getActionCommand().equalsIgnoreCase("SeriesColorSelector")) {
            setIndicatorColor();
        }
    }

    /**
     * Sets the indicator color to the current color of the selected series.
     */
    private void setIndicatorColor() {
        colorIndicator.setBackground((Color) theColor.get(seriesColorSelector.getSelectedIndex()));
    }
}
