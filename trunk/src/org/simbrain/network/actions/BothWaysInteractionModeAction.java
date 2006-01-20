
package org.simbrain.network.actions;

import org.simbrain.network.NetworkPanel;
import org.simnet.coupling.InteractionMode;

import org.simbrain.resource.ResourceManager;

/**
 * Both ways interaction mode action.
 */
public final class BothWaysInteractionModeAction
    extends InteractionModeAction {

    /**
     * Create a new both ways interaction mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public BothWaysInteractionModeAction(final NetworkPanel networkPanel) {
        super("Both ways", networkPanel, InteractionMode.BOTH_WAYS);

        // The image and description correspond to the last interaction mode this was in,
        //  so that the GUI representation shows the current mode, rather than the mode to go 
        //  in to.  
        //  TODO: Refactor this so it is more intuitive
        putValue(SMALL_ICON, ResourceManager.getImageIcon("NeitherWay.gif"));
        putValue(SHORT_DESCRIPTION, "World and network are disconnected");
    }
}