
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.InteractionMode;

import org.simbrain.network.nodes.NeuronNode;

import org.simbrain.resource.ResourceManager;

/**
 * Delete selected neurons action.
 */
public final class DeleteNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new delete neurons action with the specified
     * network panel.
     *
     * @param networkpanel networkPanel, must not be null
     */
    public DeleteNeuronsAction(final NetworkPanel networkPanel) {

        super("Delete Neuron");
        
        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Delete.gif"));
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        System.out.println("Delete selected neurons");
    }
}