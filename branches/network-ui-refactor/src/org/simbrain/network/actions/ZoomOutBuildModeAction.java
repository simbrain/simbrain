
package org.simbrain.network.actions;

import org.simbrain.network.BuildMode;
import org.simbrain.network.NetworkPanel;

/**
 * Zoom out build mode action.
 */
public final class ZoomOutBuildModeAction
    extends BuildModeAction {

    /**
     * Create a new zoom out build mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public ZoomOutBuildModeAction(final NetworkPanel networkPanel) {
        super("Zoom out", networkPanel, BuildMode.ZOOM_OUT);

        // set icon
    }
}