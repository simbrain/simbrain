package org.simbrain.workspace.actions.chart;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.plot.rasterchart.RasterPlotComponent;
import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.actions.WorkspaceAction;

@SuppressWarnings("serial")
public final class NewRasterPlotAction extends WorkspaceAction {
    /**
     * Create a new histogram chart component.
     *
     * @param workspace workspace, must not be null
     */
    public NewRasterPlotAction(final Workspace workspace) {
        super("Raster", workspace);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("ScatterIcon.png"));
        putValue(SHORT_DESCRIPTION, "New Raster Plot");
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        RasterPlotComponent plot = new RasterPlotComponent("");
        workspace.addWorkspaceComponent(plot);
    }
}
