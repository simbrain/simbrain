
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;

/**
 * Prints debug information to standard output.
 */
public final class ShowDebugAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /**
     * Create a new show help action with the specified network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public ShowDebugAction(final NetworkPanel networkPanel) {

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        networkPanel.getInputMap().put(KeyStroke.getKeyStroke("D"), this);
        networkPanel.getActionMap().put(this, this);
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        System.out.println("--- Network Panel ----");
        System.out.println(networkPanel);
        System.out.println("--- Logical Network ----");
        System.out.println(networkPanel.getNetwork());
    }
}