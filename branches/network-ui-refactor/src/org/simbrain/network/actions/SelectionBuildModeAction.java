
package org.simbrain.network.actions;

import org.simbrain.network.BuildMode;
import org.simbrain.network.NetworkPanel;

/**
 * Selection build mode action.
 */
public final class SelectionBuildModeAction
    extends BuildModeAction {

    /**
     * Create a new selection build mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public SelectionBuildModeAction(final NetworkPanel networkPanel) {
        super("Selection", networkPanel, BuildMode.SELECTION);

        // set icon
    }
}