
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;

/**
 * Select all action.
 */
public final class SelectAllAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new select all action.
     *
     * @param networkPanel network panel, must not be null
     */
    public SelectAllAction(final NetworkPanel networkPanel) {
        super("Select all");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        // set keys?
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.selectAll();
    }
}