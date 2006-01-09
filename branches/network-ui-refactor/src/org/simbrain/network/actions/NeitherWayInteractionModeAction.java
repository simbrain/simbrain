
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

        putValue(SMALL_ICON, ResourceManager.getImageIcon("NeitherWay.gif"));
        putValue(SHORT_DESCRIPTION, "World and network are disconnected");
    }
}