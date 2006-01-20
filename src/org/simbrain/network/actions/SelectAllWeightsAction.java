
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;

/**
 * Select all weights.
 */
public final class SelectAllWeightsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new select all weights action with the specified network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public SelectAllWeightsAction(final NetworkPanel networkPanel) {

        super("Select All Weights");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('w'), this);
        networkPanel.getActionMap().put(this, this);
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.clearSelection();
        networkPanel.setSelection(networkPanel.getSynapseNodes());
    }
}