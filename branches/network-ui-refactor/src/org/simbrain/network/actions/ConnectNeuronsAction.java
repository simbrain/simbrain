
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SynapseNode;

import org.simbrain.resource.ResourceManager;
import org.simnet.synapses.ClampedSynapse;

/**
 * Connect neurons.
 */
public final class ConnectNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Source neuron. */
    private NeuronNode source;

    /** Target neuron. */
    private NeuronNode target;

    /**
     * Create a new connect neurons action.
     *
     * @param networkPanel network panel, must not be null
     */
    public ConnectNeuronsAction(final NetworkPanel networkPanel, NeuronNode source, NeuronNode target) {
        super("Connect");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        this.source = source;
        this.target = target;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.getNetwork().addWeight(new ClampedSynapse(source.getNeuron(), target.getNeuron()));
    }
}