package org.simbrain.plot.barchart;

import kotlin.Unit;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.simbrain.plot.ChartThemeKt;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.SwingUtilsKt;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * GUI for a bar chart.
 */
public class BarChartDesktopComponent extends DesktopComponent<BarChartComponent> implements ActionListener {

    /**
     * Main JFreeChart object.
     */
    private final JFreeChart chart;

    /**
     * Panel for chart.
     */
    private final ChartPanel chartPanel = new ChartPanel(null);

    /**
     * Preferred frame size.
     */
    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);

    /**
     * Plot action manager.
     */
    private final PlotActionManager actionManager;

    /**
     * Construct the GUI Bar Chart.
     *
     * @param frame     Generic frame
     * @param component Bar chart component
     */
    public BarChartDesktopComponent(GenericFrame frame, BarChartComponent component) {
        super(frame, component);
        setPreferredSize(PREFERRED_SIZE);
        actionManager = new PlotActionManager(this);
        setLayout(new BorderLayout());

        //JButton deleteButton = new JButton("Delete");
        //deleteButton.setActionCommand("Delete");
        //deleteButton.addActionListener(this);
        //JButton addButton = new JButton("Add");
        //addButton.setActionCommand("Add");
        //addButton.addActionListener(this);
        //JPanel buttonPanel = new JPanel();
        //buttonPanel.add(deleteButton);
        //buttonPanel.add(addButton);

        createAttachMenuBar();

        add("Center", chartPanel);
        // add("South", buttonPanel);

        String title = "";
        String xLabel = "Bar";
        String yLabel = "Value";
        boolean legend = false;
        boolean tooltips = true;
        boolean urls = false;

        chart = ChartFactory.createBarChart(title, xLabel, yLabel, this.getWorkspaceComponent().getModel().getDataset(), PlotOrientation.VERTICAL, legend, tooltips, urls);
        ChartThemeKt.applySimbrainChartTheme(chart);
        chart.getCategoryPlot().getRenderer().setSeriesPaint(0, getWorkspaceComponent().getModel().getBarColor());
        chartPanel.setChart(chart);
        chart.getCategoryPlot().getRangeAxis().setAutoRange(getWorkspaceComponent().getModel().isAutoRange());
        if (!getWorkspaceComponent().getModel().isAutoRange()) {
            chart.getCategoryPlot().getRangeAxis().setRange(getWorkspaceComponent().getModel().getLowerBound(), getWorkspaceComponent().getModel().getUpperBound());
        }

        // Add a chart setting listener
        // getWorkspaceComponent().getModel().addChartSettingsListener(new ChartSettingsListener() {
        //     // TODO: Explore parameters in chart, chart.getCategoryPlot(),
        //     // chart.getCategoryPlot().getRenderer(), chartPanel..
        //     public void chartSettingsUpdated(ChartModel model) {
        //         // Update colors
        //         chart.getCategoryPlot().getRenderer().setSeriesPaint(0, getWorkspaceComponent().getModel().getBarColor());
        //         // Update auto-range
        //         chart.getCategoryPlot().getRangeAxis().setAutoRange(getWorkspaceComponent().getModel().isAutoRange());
        //         // Update ranges
        //         if (!getWorkspaceComponent().getModel().isAutoRange()) {
        //             chart.getCategoryPlot().getRangeAxis().setRange(getWorkspaceComponent().getModel().getLowerBound(), getWorkspaceComponent().getModel().getUpperBound());
        //         }
        //     }
        // });

        // Fire the chart listener to update settings
        // getWorkspaceComponent().getModel().fireSettingsChanged();
    }

    /**
     * Creates the menu bar.
     */
    private void createAttachMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        for (Action action : actionManager.getOpenSavePlotActions()) {
            fileMenu.add(action);
        }
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createRenameAction(this));
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createCloseAction(this));

        JMenu editMenu = new JMenu("Edit");
        JMenuItem preferences = new JMenuItem("Preferences...");
        preferences.addActionListener(this);
        preferences.setActionCommand("dialog");
        editMenu.add(preferences);

        JMenu helpMenu = new JMenu("Help");
        ShowHelpAction helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/plots/barChart.html");
        JMenuItem helpItem = new JMenuItem(helpAction);
        helpMenu.add(helpItem);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(helpMenu);

        getParentFrame().setJMenuBar(bar);
    }

    @Override
    public void actionPerformed(final ActionEvent arg0) {
        if (arg0.getActionCommand().equalsIgnoreCase("dialog")) {
            var dialog = SwingUtilsKt.createEditorDialog(
                    getWorkspaceComponent().getModel(),
                    getWorkspaceComponent().getModel().getName(),
                    null,
                    (e) -> {
                chart.getCategoryPlot()
                        .getRenderer()
                        .setSeriesPaint(0, getWorkspaceComponent().getModel().getBarColor());
                chart.getCategoryPlot().getRangeAxis()
                        .setAutoRange(getWorkspaceComponent().getModel().isAutoRange());
                if (!getWorkspaceComponent().getModel().isAutoRange()) {
                    chart.getCategoryPlot().getRangeAxis()
                            .setRange(
                                    getWorkspaceComponent().getModel().getLowerBound(),
                                    getWorkspaceComponent().getModel().getUpperBound()
                            );
                }
                return Unit.INSTANCE;
            });
            SwingUtilsKt.display(dialog);
        }

    }
}
