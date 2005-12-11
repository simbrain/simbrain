
package org.simbrain.network.actions;

import org.simbrain.network.BuildMode;
import org.simbrain.network.NetworkPanel;

/**
 * Zoom in build mode action.
 */
public final class ZoomInBuildModeAction
    extends BuildModeAction {

    /**
     * Create a new zoom in build mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public ZoomInBuildModeAction(final NetworkPanel networkPanel) {
        super("Zoom in", networkPanel, BuildMode.ZOOM_IN);

        // set icon
    }
}