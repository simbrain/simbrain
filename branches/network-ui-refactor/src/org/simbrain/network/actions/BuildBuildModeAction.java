
package org.simbrain.network.actions;

import org.simbrain.network.BuildMode;
import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Build build mode action.
 */
public final class BuildBuildModeAction
    extends BuildModeAction {

    /**
     * Create a new build build mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public BuildBuildModeAction(final NetworkPanel networkPanel) {
        super("Build", networkPanel, BuildMode.BUILD);

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Build.gif"));
    }
}