package org.simbrain.network.actions.connection;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.nodes.NeuronNode;
import org.simnet.synapses.ClampedSynapse;

/**
 * Connect neurons action.  Connects a set of source neurons to a set of target neurons.
 */
public class ConnectNeuronsSimpleAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Source neurons. */
    private Collection<NeuronNode> sourceNeurons;

    /** Target neuron. */
    private NeuronNode targetNeuron;

    /**
     * Create a new connect neurons action.  Connects a set of source neurons to a set of target neurons.
     *
     * @param networkPanel network panel, must not be null
     * @param sourceNeurons NeuronNodes to connect from
     * @param targetNeuron NeuronNodes to connect to
     */
    public ConnectNeuronsSimpleAction(final NetworkPanel networkPanel,
                                final Collection<NeuronNode> sourceNeurons,
                                final NeuronNode targetNeuron) {

        super("Connect Simple");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        this.sourceNeurons = sourceNeurons;
        this.targetNeuron = targetNeuron;

    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent arg0) {

        if (sourceNeurons.isEmpty() || targetNeuron.equals(null)) {
            return;
        }

        for (NeuronNode source : sourceNeurons) {
                networkPanel.getRootNetwork().addSynapse(
                        new ClampedSynapse(source.getNeuron(), targetNeuron
                                .getNeuron()));
        }

    }

}
