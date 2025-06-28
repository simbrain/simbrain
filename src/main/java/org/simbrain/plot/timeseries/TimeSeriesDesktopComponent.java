package org.simbrain.plot.timeseries;

import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;

/**
 * Display a TimeSeriesPlot.
 */
public class TimeSeriesDesktopComponent extends DesktopComponent<TimeSeriesPlotComponent> {

    /**
     * Plot action manager.
     */
    private final PlotActionManager actionManager;

    /**
     * Panel for chart.
     */
    private final TimeSeriesPlotPanel timeSeriesPanel;

    /**
     * Construct a time series plot gui.
     *
     * @param frame     parent frame
     * @param component the underlying component
     */
    public TimeSeriesDesktopComponent(GenericFrame frame, TimeSeriesPlotComponent component) {
        super(frame, component);
        actionManager = new PlotActionManager(this);
        timeSeriesPanel = new TimeSeriesPlotPanel(component.getModel());
        createAttachMenuBar();
        this.setLayout(new BorderLayout());
        add("Center", timeSeriesPanel);
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
        editMenu.add(new JMenuItem(TimeSeriesPlotActions.getPropertiesDialogAction(timeSeriesPanel)));

        JMenu helpMenu = new JMenu("Help");
        ShowHelpAction helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/plots/timeSeries.html");
        JMenuItem helpItem = new JMenuItem(helpAction);
        helpMenu.add(helpItem);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(helpMenu);

        getParentFrame().setJMenuBar(bar);
    }

}
