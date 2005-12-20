
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;
import org.simbrain.network.dialog.NetworkDialog;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.Utils;

/**
 * Show help action, opens help file <code>Network.html</code>
 * in an external web browser.
 */
public final class ShowNetworkPreferencesAction
    extends AbstractAction {

	/** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new delete neurons action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public ShowNetworkPreferencesAction(final NetworkPanel networkPanel) {

        super("Network Preferences");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

    	NetworkDialog dialog = new NetworkDialog(networkPanel);
    	dialog.pack();
    	dialog.setVisible(true);
    	
    }
}