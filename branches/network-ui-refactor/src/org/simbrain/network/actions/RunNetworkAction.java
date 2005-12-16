
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;
import org.simbrain.network.NetworkThread;

import org.simbrain.resource.ResourceManager;

/**
 * Run network action.
 */
public final class RunNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new run network action.
     *
     * @param networkPanel network panel, must not be null
     */
    public RunNetworkAction(final NetworkPanel networkPanel) {
        super();

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Play.gif"));
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        if (networkPanel.getNetworkThread() == null) {
            networkPanel.setNetworkThread(new NetworkThread(networkPanel));
        }

        NetworkThread theThread= networkPanel.getNetworkThread();

        if (!theThread.isRunning()) {
            theThread.setRunning(true);
            theThread.start();
        } else {
            theThread.setRunning(false);
        }

    }
}