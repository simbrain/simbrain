package org.simbrain.plot.timeseries;

import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Contains actions for use in Time Series Component.
 *
 * @author jyoshimi
 */
public class TimeSeriesPlotActions {

    /**
     * Shows a properties dialog for the trainer.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getPropertiesDialogAction(TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {
            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Tools.png"));
                putValue(NAME, "Plot properties...");
                putValue(SHORT_DESCRIPTION, "Show time series graph properties");
            }

            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.showPropertiesDialog();
            }
        };
    }

    /**
     * Clear the graph.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getClearGraphAction(TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {
            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Eraser.png"));
                putValue(SHORT_DESCRIPTION, "Clear graph data");
            }

            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getTimeSeriesModel().clearData();
            }
        };
    }

    /**
     * Add a data source.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getAddSourceAction(TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {
            // Initialize
            {
                putValue(NAME, "Add");
                putValue(SHORT_DESCRIPTION, "Add a data source");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getTimeSeriesModel().addTimeSeries();
            }
        };
    }

    /**
     * Add a data source.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getRemoveSourceAction(
        final TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(NAME, "Remove");
                putValue(SHORT_DESCRIPTION, "Remove a data source");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getTimeSeriesModel().removeLastTimeSeries();
            }

        };
    }


}
