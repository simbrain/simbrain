
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

/**
 * Clear selection action.
 */
public final class ClearSelectionAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new clear selection action.
     *
     * @param networkPanel network panel, must not be null
     */
    public ClearSelectionAction(final NetworkPanel networkPanel) {
        super("Clear selection");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('u'), this);
        networkPanel.getActionMap().put(this, this);

        // conditional, only enabled if something is selected
        setEnabled(!this.networkPanel.isSelectionEmpty());

        // add a selection listener to update state based on selection
        this.networkPanel.addSelectionListener(new NetworkSelectionListener() {

                /** @see NetworkSelectionListener */
                public void selectionChanged(final NetworkSelectionEvent event) {

                    if (networkPanel.isSelectionEmpty()) {
                        setEnabled(false);
                    }
                    else {
                        setEnabled(true);
                    }
                }
            });
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.clearSelection();
    }
}