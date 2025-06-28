package org.simbrain.plot.piechart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.SwingUtilsKt;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;

/**
 * Display a PieChart.
 */
public class PieChartDesktopComponent extends DesktopComponent<PieChartComponent> {

    private final PlotActionManager actionManager;

    private final ChartPanel chartPanel = new ChartPanel(null);

    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);

    private final JFreeChart chart;

    public PieChartDesktopComponent(final GenericFrame frame, final PieChartComponent component) {
        super(frame, component);
        setPreferredSize(PREFERRED_SIZE);
        actionManager = new PlotActionManager(this);
        setLayout(new BorderLayout());

        createAttachMenuBar();

        add("Center", chartPanel);

        chart = ChartFactory.createPieChart("", getWorkspaceComponent().getModel().getDataset(), true, true, false);
        chartPanel.setChart(chart);
    }

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
        preferences.addActionListener((e) -> {
            SwingUtilsKt.display(SwingUtilsKt.createEditorDialog(getWorkspaceComponent().getModel()));
        });
        editMenu.add(preferences);

        JMenu helpMenu = new JMenu("Help");
        ShowHelpAction helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/plots/pieChart.html");
        JMenuItem helpItem = new JMenuItem(helpAction);
        helpMenu.add(helpItem);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(helpMenu);

        getParentFrame().setJMenuBar(bar);
    }

}
