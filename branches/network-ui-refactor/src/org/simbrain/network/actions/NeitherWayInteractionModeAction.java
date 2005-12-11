
package org.simbrain.network.actions;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.InteractionMode;

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

        // set icon
    }
}