
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simnet.coupling.InteractionMode;

/**
 * Interaction mode action.
 */
class InteractionModeAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Interaction mode. */
    private final InteractionMode interactionMode;


    /**
     * Create a new interaction mode action with the specified
     * name, network panel, and interaction mode.
     *
     * @param name name
     * @param networkPanel network panel, must not be null
     * @param interactionMode interaction mode, must not be null
     */
    InteractionModeAction(final String name,
                          final NetworkPanel networkPanel,
                          final InteractionMode interactionMode) {

        super(name);

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        if (interactionMode == null) {
            throw new IllegalArgumentException("interactionMode must not be null");
        }

        this.networkPanel = networkPanel;
        this.interactionMode = interactionMode;
    }


    /** @see AbstractAction */
    public final void actionPerformed(final ActionEvent event) {
        networkPanel.getNetwork().setInteractionMode(interactionMode);
    }
}