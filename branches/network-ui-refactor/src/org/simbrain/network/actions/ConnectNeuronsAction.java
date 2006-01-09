
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.nodes.NeuronNode;
import org.simnet.synapses.ClampedSynapse;

/**
 * Connect neurons action.  Connects a set of source neurons to a set of target neurons.
 */
public final class ConnectNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Source neuron. */
    private Collection sourceNeurons;

    /** Target neuron. */
    private Collection targetNeurons;


    /**
     * Create a new connect neurons action.  Connects a set of source neurons to a set of target neurons.
     *
     * @param networkPanel network panel, must not be null
     * @param sourceNeurons NeuronNodes to connect from
     * @param targetNeurons NeuronNodes to connect to
     */
    public ConnectNeuronsAction(final NetworkPanel networkPanel,
                                final Collection sourceNeurons,
                                final Collection targetNeurons) {
        super("Connect");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        this.sourceNeurons = sourceNeurons;
        this.targetNeurons = targetNeurons;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        for (Iterator i = sourceNeurons.iterator(); i.hasNext();) {
            for (Iterator j = targetNeurons.iterator(); j.hasNext();) {
                NeuronNode source = (NeuronNode) i.next();
                NeuronNode target = (NeuronNode) j.next();
                networkPanel.getNetwork().addWeight(new ClampedSynapse(source.getNeuron(), target.getNeuron()));
            }
        }
    }
}