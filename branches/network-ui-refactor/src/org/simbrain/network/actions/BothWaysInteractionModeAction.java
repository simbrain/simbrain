
package org.simbrain.network.actions;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.InteractionMode;

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

        // set icon
    }
}