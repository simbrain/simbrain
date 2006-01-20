
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;

/**
 * Aligns horizontal.
 */
public final class AlignHorizontalAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new align horizontal action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public AlignHorizontalAction(final NetworkPanel networkPanel) {

        super("Align Horizontal");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        networkPanel.alignHorizontal();

    }
}