
package org.simbrain.network.actions;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.InteractionMode;

import org.simbrain.resource.ResourceManager;

/**
 * Neither way interaction mode action.
 */
public final class NeitherWayInteractionModeAction
    extends InteractionModeAction {

    /**
     * Create a new neither way interaction mode action.
     *
     * @param networkPanel network panel, must not be null
     */
    public NeitherWayInteractionModeAction(final NetworkPanel networkPanel) {
        super("Neither way", networkPanel, InteractionMode.NEITHER_WAY);

        putValue(SMALL_ICON, ResourceManager.getImageIcon("NeitherWay.gif"));
        putValue(SHORT_DESCRIPTION, "World and network are disconnected");
    }
}