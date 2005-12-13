
package org.simbrain.network.actions;

import org.simbrain.network.BuildMode;
import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Pan build mode action.
 */
public final class PanBuildModeAction
    extends BuildModeAction {

    /**
     * Create a new pan build mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public PanBuildModeAction(final NetworkPanel networkPanel) {
        super("Pan", networkPanel, BuildMode.PAN);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Pan.gif"));

    }
}