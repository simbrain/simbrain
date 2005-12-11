
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.InteractionMode;
import org.simbrain.network.nodes.NeuronNode;

/**
 * Add a new neuron to the NetworkPanel.
 */
public class NewNeuronAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /**
     * Create a new neuron.
     */
    public NewNeuronAction(final NetworkPanel networkPanel) {

        super("New Neuron");
        
        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public final void actionPerformed(final ActionEvent event) {
        networkPanel.getLayer().addChild(new NeuronNode(50,50));
    }
}