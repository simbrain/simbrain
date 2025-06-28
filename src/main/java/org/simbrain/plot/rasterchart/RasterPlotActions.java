package org.simbrain.plot.rasterchart;

import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Contains actions for use in raster plot
 */
public class RasterPlotActions {

    /**
     * Shows a properties dialog for the trainer.
     *
     * @param rasterPlotPanel reference to time series plot panel
     * @return the action
     */
    public static Action getPropertiesDialogAction(final RasterPlotPanel rasterPlotPanel) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Tools.png"));
                putValue(NAME, "Preferences...");
                putValue(SHORT_DESCRIPTION, "Show raster chart properties");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                rasterPlotPanel.showPropertiesDialog();
            }
        };
    }

    /**
     * Clear the graph.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getClearGraphAction(final RasterPlotPanel timeSeriesPanel) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Eraser.png"));
                putValue(SHORT_DESCRIPTION, "Clear graph data");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getRasterModel().clearData();
            }

        };
    }

    /**
     * Add a data source.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getAddSourceAction(final RasterPlotPanel timeSeriesPanel) {
        return new AbstractAction() {

            // Initialize
            {
                // putValue(SMALL_ICON,
                // ResourceManager.getSmallIcon("Eraser.png"));
                putValue(NAME, "Add");
                putValue(SHORT_DESCRIPTION, "Add a data source");
            }

            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getRasterModel().addDataSource();
            }

        };
    }

    /**
     * Add a data source.
     *
     * @param rasterPanel reference to time series plot panel
     * @return the action
     */
    public static Action getRemoveSourceAction(final RasterPlotPanel rasterPanel) {
        return new AbstractAction() {

            // Initialize
            {
                // putValue(SMALL_ICON,
                // ResourceManager.getSmallIcon("Eraser.png"));
                putValue(NAME, "Remove");
                putValue(SHORT_DESCRIPTION, "Remove a data source");
            }

            public void actionPerformed(ActionEvent arg0) {
                rasterPanel.getRasterModel().removeDataSource();
            }

        };
    }


}
