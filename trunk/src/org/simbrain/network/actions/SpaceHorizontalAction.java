
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;

/**
 * Space horizontal.
 */
public final class SpaceHorizontalAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new space horizontal action with the specified network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SpaceHorizontalAction(final NetworkPanel networkPanel) {

        super("Space Horizontal");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        networkPanel.spaceHorizontal();

    }
}