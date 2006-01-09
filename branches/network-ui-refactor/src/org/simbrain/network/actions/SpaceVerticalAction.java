
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;

/**
 * Space vertical.
 */
public final class SpaceVerticalAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new space vertical action with the specified network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SpaceVerticalAction(final NetworkPanel networkPanel) {

        super("Space Vertical");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        networkPanel.spaceVertical();
    }
}