
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.SwingUtilities;

import org.simbrain.network.NetworkFrame;
import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

import org.simbrain.util.Utils;

/**
 * Show help action, opens help file <code>Network.html</code>
 * in an external web browser.
 */
public final class SetAutoZoomAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    JCheckBoxMenuItem cb;

    /**
     * Create a new show network preferences action with the specified
     * network panel.
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

        // Perform action
        JCheckBoxMenuItem cb = (JCheckBoxMenuItem) event.getSource();

        // Determine status
        networkPanel.setAutoZoomMode(cb.isSelected());

        networkPanel.centerCamera();

    }
}