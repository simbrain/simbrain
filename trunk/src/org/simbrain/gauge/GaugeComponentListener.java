package org.simbrain.gauge;

import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * listener for dimension changed events.
 * 
 * @author unknown
 */
public interface GaugeComponentListener extends WorkspaceComponentListener {
    /**
     * Called when the dimensions change.
     * 
     * @param newDimensions The new number of dimensions.
     */
    void dimensionsChanged(final int newDimensions);
}
