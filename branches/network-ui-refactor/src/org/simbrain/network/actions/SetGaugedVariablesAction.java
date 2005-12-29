
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
 * Set gauged variables.
 */
public final class SetGaugedVariablesAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Set Gauged variables
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SetGaugedVariablesAction(final NetworkPanel networkPanel) {

        super("Set Gauge");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

    }
}