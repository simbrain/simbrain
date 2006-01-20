
package org.simbrain.network.actions;

import org.simbrain.network.NetworkPanel;
import org.simnet.coupling.InteractionMode;

import org.simbrain.resource.ResourceManager;

/**
 * Network to world interaction mode action.
 */
public final class NetworkToWorldInteractionModeAction
    extends InteractionModeAction {

    /**
     * Create a new network to world interaction mode action with the
     * specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public NetworkToWorldInteractionModeAction(final NetworkPanel networkPanel) {
        super("Network to world", networkPanel, InteractionMode.NETWORK_TO_WORLD);

        // The image and description correspond to the last interaction mode this was in,
        //  so that the GUI representation shows the current mode, rather than the mode to go 
        //  in to.  
        //  TODO: Refactor this so it is more intuitive
        putValue(SMALL_ICON, ResourceManager.getImageIcon("WorldToNet.gif"));
        putValue(SHORT_DESCRIPTION, "World is sending stimuli to the network");

    }
}