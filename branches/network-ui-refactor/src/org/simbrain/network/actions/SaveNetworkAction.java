
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.Utils;

/**
 * Save network action.
 */
public final class SaveNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new save network action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SaveNetworkAction(final NetworkPanel networkPanel) {

        super("Save");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Save.gif"));

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.saveCurrentNetwork();
    }
}