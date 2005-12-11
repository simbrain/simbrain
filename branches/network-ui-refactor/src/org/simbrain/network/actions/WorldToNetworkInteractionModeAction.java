
package org.simbrain.network.actions;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.InteractionMode;

/**
 * World to network interaction mode action.
 */
public final class WorldToNetworkInteractionModeAction
    extends InteractionModeAction {

    /**
     * Create a new world to network interaction mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public WorldToNetworkInteractionModeAction(final NetworkPanel networkPanel) {
        super("World to network", networkPanel, InteractionMode.WORLD_TO_NETWORK);

        // set icon
    }
}