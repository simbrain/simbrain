
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

import org.simbrain.resource.ResourceManager;

/**
 * Clear selected neurons action.
 */
public final class ClearNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new clear selected neurons action with the
     * specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public ClearNeuronsAction(final NetworkPanel networkPanel) {
        super("Clear selected neurons");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Eraser.gif"));
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        System.out.println("Clear neurons");
    }
}