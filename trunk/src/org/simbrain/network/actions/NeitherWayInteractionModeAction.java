
package org.simbrain.network.actions;

import org.simbrain.network.NetworkPanel;
import org.simnet.coupling.InteractionMode;

import org.simbrain.resource.ResourceManager;

/**
 * Neither way interaction mode action.
 */
public final class NeitherWayInteractionModeAction
    extends InteractionModeAction {

    /**
     * Create a new neither way interaction mode action with the
     * specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public NeitherWayInteractionModeAction(final NetworkPanel networkPanel) {
        super("Neither way", networkPanel, InteractionMode.NEITHER_WAY);

        // The image and description correspond to the last interaction mode this was in,
        //  so that the GUI representation shows the current mode, rather than the mode to go 
        //  in to.  
        //  TODO: Refactor this so it is more intuitive
        putValue(SMALL_ICON, ResourceManager.getImageIcon("NetToWorld.gif"));
        putValue(SHORT_DESCRIPTION, "Network output is being sent to worlds");
    }
}