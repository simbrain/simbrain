
package org.simbrain.network.filters;

import edu.umd.cs.piccolo.PNode;

import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simbrain.network.nodes.SynapseNode;

/**
 * Filters.
 */
public final class Filters {

    /** Neuron node filter. */
    private static final AbstractFilter NEURON_NODE_FILTER = new AbstractFilter() {
            /** @see AbstractFilter */
            public boolean accept(final PNode node) {
                return (node instanceof NeuronNode);
            }
        };

    /** Synapse node filter. */
    private static final AbstractFilter SYNAPSE_NODE_FILTER = new AbstractFilter() {
            /** @see AbstractFilter */
            public boolean accept(final PNode node) {
                return (node instanceof SynapseNode);
            }
        };

    /** Neuron or synapse node filter. */
    private static final AbstractFilter NEURON_OR_SYNAPSE_NODE_FILTER = new AbstractFilter() {
            /** @see AbstractFilter */
            public boolean accept(final PNode node) {
                return ((node instanceof NeuronNode) || (node instanceof SynapseNode));
            }
        };

    /** Subnetwork node filter. */
    private static final AbstractFilter SUBNETWORK_NODE_FILTER = new AbstractFilter() {
            /** @see AbstractFilter */
            public boolean accept(final PNode node) {
                return (node instanceof SubnetworkNode);
            }
        };


    /**
     * Private constructor.
     */
    private Filters() {
        // empty
    }


    /**
     * Return the neuron node filter.
     *
     * @return the neuron node filter
     */
    public static AbstractFilter getNeuronNodeFilter() {
        return NEURON_NODE_FILTER;
    }

    /**
     * Return the neuron or synapse node filter.
     *
     * @return the neuron or synapse node filter
     */
    public static AbstractFilter getNeuronOrSynapseNodeFilter() {
        return NEURON_OR_SYNAPSE_NODE_FILTER;
    }

    /**
     * Return the synapse node filter.
     *
     * @return the synapse node filter
     */
    public static AbstractFilter getSynapseNodeFilter() {
        return SYNAPSE_NODE_FILTER;
    }

    /**
     * Return the subnetwork node filter.
     *
     * @return the subnetwork node filter
     */
    public static AbstractFilter getSubnetworkNodeFilter() {
        return SUBNETWORK_NODE_FILTER;
    }
}