
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.simbrain.network.NetworkPanel;

/**
 * Show input output information.
 */
public final class ShowIOInfoAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show input output information action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public ShowIOInfoAction(final NetworkPanel networkPanel) {

        super("Show IO Info");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        // Perform action
        JCheckBoxMenuItem cb = (JCheckBoxMenuItem) event.getSource();

        // Determine status
        networkPanel.setInOutMode(cb.isSelected());

    }
}