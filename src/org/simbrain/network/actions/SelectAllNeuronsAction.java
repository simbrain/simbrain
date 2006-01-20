
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;

/**
 * Select all neurons.
 */
public final class SelectAllNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new select all neurons action with the specified network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SelectAllNeuronsAction(final NetworkPanel networkPanel) {

        super("Select All Neurons");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('n'), this);
        networkPanel.getActionMap().put(this, this);
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.clearSelection();
        networkPanel.setSelection(networkPanel.getNeuronNodes());
    }
}