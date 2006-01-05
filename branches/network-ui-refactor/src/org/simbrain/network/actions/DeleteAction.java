
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;

import org.simbrain.resource.ResourceManager;

/**
 * Clear action.
 */
public final class DeleteAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new clear action with the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public DeleteAction(final NetworkPanel networkPanel) {
        super("Delete");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Delete.gif"));
        putValue(SHORT_DESCRIPTION, "Delete selected node(\"Backspace\")");

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke("BACK_SPACE"), this);
        networkPanel.getActionMap().put(this, this);
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.deleteSelectedObjects();
    }
}