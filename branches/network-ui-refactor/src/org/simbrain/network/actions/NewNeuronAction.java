
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.InteractionMode;

import org.simbrain.network.nodes.NeuronNode;

import org.simbrain.resource.ResourceManager;

/**
 * New neuron action.
 */
public final class NewNeuronAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new neuron action with the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public NewNeuronAction(final NetworkPanel networkPanel) {
        super("New Neuron");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("New.gif"));

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke('p'), this);
        networkPanel.getActionMap().put(this, this);

    }


    /** @see AbstractAction */
    public final void actionPerformed(final ActionEvent event) {
        NeuronNode node = new NeuronNode(50, 50);
        networkPanel.getLayer().addChild(node);
        if (networkPanel.getNetwork().getFlatNeuronList().contains(node.getNeuron()) == false) {
            networkPanel.getNetwork().addNeuron(node.getNeuron());
        }
        System.out.println(networkPanel);
        networkPanel.getNetwork().debug();
    }
}