
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.simbrain.network.NetworkPanel;

/**
 * Set auto zoom.
 */
public final class SetAutoZoomAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /**
     * Create a new set auto zoom action with the specified network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SetAutoZoomAction(final NetworkPanel networkPanel) {

        super("Auto Zoom");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        JCheckBoxMenuItem cb = (JCheckBoxMenuItem) event.getSource();
        networkPanel.setAutoZoomMode(cb.isSelected());
    }
}