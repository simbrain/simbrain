
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;

/**
 * Aligns vertical.
 */
public final class AlignVerticalAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new align vertical action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public AlignVerticalAction(final NetworkPanel networkPanel) {

        super("Align Vertical");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        networkPanel.alignVertical();

    }
}