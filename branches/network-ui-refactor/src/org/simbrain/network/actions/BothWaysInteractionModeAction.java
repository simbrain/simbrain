
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

        putValue(SMALL_ICON, ResourceManager.getImageIcon("BothWays.gif"));
        putValue(SHORT_DESCRIPTION, "World and network are interacting");
    }
}