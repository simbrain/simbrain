
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

import org.simbrain.resource.ResourceManager;

/**
 * Iterate network action.
 */
public final class IterateNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new iterate network action with the specified
     * network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public IterateNetworkAction(final NetworkPanel networkPanel) {
        super();

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Step.gif"));

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke(' '), this);
        networkPanel.getActionMap().put(this, this);

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        System.out.println("Iterate network");
    }
}