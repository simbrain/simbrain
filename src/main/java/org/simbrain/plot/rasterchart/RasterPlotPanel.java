package org.simbrain.plot.rasterchart;

import kotlin.Unit;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.simbrain.plot.raster.RasterModel;
import org.simbrain.util.SwingUtilsKt;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Display a raster plot. This component can be used independently of the raster
 * plot workspace component.
 */
public class RasterPlotPanel extends JPanel {

    private JFreeChart chart;

    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);

    private final ChartPanel chartPanel = new ChartPanel(null);

    /**
     * Data model.
     */
    private final RasterModel model;

    /**
     * Button panel.
     */
    private final JPanel buttonPanel = new JPanel();

    /**
     * Renderer object where things like dot color and size are set.
     */
    private XYItemRenderer renderer;

    public RasterPlotPanel(final RasterModel rasterModel) {

        model = rasterModel;
        setPreferredSize(PREFERRED_SIZE);
        setLayout(new BorderLayout());

        addAddDeleteButtons();
        addClearGraphDataButton();
        addPreferencesButton();

        add("Center", chartPanel);
        add("South", buttonPanel);

        init();
    }

    /**
     * Initialize Chart Panel.
     */
    public void init() {

        // Generate the graph
        chart = ChartFactory.createScatterPlot("", // Title
            "Iterations", // x-axis Label
            "Value(s)", // y-axis Label
                model.getDataset(), // Dataset
            PlotOrientation.VERTICAL, // Plot Orientation
            true, // Show Legend
            true, // Use tooltips
            false // Configure chart to generate URLs?
        );
        renderer = ((XYPlot) chart.getPlot()).getRenderer();
        updateChartSettings();
        model.getEvents().getPropertyChanged().on(this::updateChartSettings);
        chartPanel.setChart(chart);
        chart.setBackgroundPaint(null);

    }

    public void updateChartSettings() {

        // Renderer properties
        double size = model.getDotSize();
        double delta = size / 2.0;
        Shape shape1 = new Rectangle2D.Double(-delta, -delta, size, size);
        Shape shape2 = new Ellipse2D.Double(-delta, -delta, size, size);
        renderer.setSeriesShape(0, shape1);
        renderer.setSeriesShape(1, shape2);
        renderer.setSeriesShape(2, shape1);
        renderer.setSeriesShape(3, shape2);

        // Handle domain properties
        if (model.isFixedWidth()) {
            chart.getXYPlot().getDomainAxis().setFixedAutoRange(model.getWindowSize());
        } else {
            chart.getXYPlot().getDomainAxis().setFixedAutoRange(-1);
            chart.getXYPlot().getDomainAxis().setAutoRange(true);
        }
    }

    /**
     * Remove all buttons from the button panel; used when customzing the
     * buttons on this panel.
     */
    public void removeAllButtonsFromToolBar() {
        buttonPanel.removeAll();
    }

    /**
     * Return button panel in case user would like to add custom buttons.
     */
    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    /**
     * Add button for clearing graph data.
     */
    public void addClearGraphDataButton() {
        JButton clearButton = new JButton("Clear");
        clearButton.setAction(RasterPlotActions.getClearGraphAction(this));
        buttonPanel.add(clearButton);
    }

    /**
     * Add button for showing preferences.
     */
    public void addPreferencesButton() {
        JButton prefsButton = new JButton("Prefs");
        prefsButton.setHideActionText(true);
        prefsButton.setAction(RasterPlotActions.getPropertiesDialogAction(this));
        buttonPanel.add(prefsButton);
    }

    /**
     * Add buttons for adding and deleting sources.
     */
    public void addAddDeleteButtons() {
        JButton deleteButton = new JButton("Delete");
        deleteButton.setAction(RasterPlotActions.getRemoveSourceAction(this));
        JButton addButton = new JButton("Add");
        addButton.setAction(RasterPlotActions.getAddSourceAction(this));
        buttonPanel.add(deleteButton);
        buttonPanel.add(addButton);
    }

    /**
     * Show properties dialog.
     */
    public void showPropertiesDialog() {
        var dialog = SwingUtilsKt.createEditorDialog(model, model.getName(), (e) -> {
            updateChartSettings();
            return Unit.INSTANCE;
        });
        SwingUtilsKt.display(dialog);
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    public RasterModel getRasterModel() {
        return model;
    }
}
