
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;
import org.simbrain.network.NetworkThread;

import org.simbrain.resource.ResourceManager;

/**
 * Stop network action.
 */
public final class StopNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new stop network action with the specified
     * network panel
     *
     * @param networkPanel network panel, must not be null
     */
    public StopNetworkAction(final NetworkPanel networkPanel) {

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Stop.gif"));
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        if (networkPanel.getNetworkThread() == null) {
            networkPanel.setNetworkThread(new NetworkThread(networkPanel));
        }

        NetworkThread theThread= networkPanel.getNetworkThread();

        theThread.setRunning(false);
        networkPanel.setNetworkThread(null);
    }
}