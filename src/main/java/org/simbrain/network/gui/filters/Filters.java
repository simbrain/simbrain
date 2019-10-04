/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.filters;

import org.piccolo2d.PNode;
import org.simbrain.network.DL4JSandbox.DL4JMultiLayerNetworkNode;
import org.simbrain.network.gui.nodes.*;

/**
 * Filters.
 */
public final class Filters {

    /**
     * Private constructor.
     */
    private Filters() {
    }

    /**
     * Neuron node filter.
     */
    private static final AbstractFilter NEURON_NODE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return ((node instanceof NeuronNode) && (!isGrouped(node)));
        }
    };

    /**
     * Neuron group node filter.
     */
    private static final AbstractFilter NEURON_GROUP_NODE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return (node.getParent() instanceof NeuronGroupNode);
        }
    };

    /**
     * Synapse node filter.
     */
    private static final AbstractFilter SYNAPSE_NODE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return ((node instanceof SynapseNode) && (!isGrouped(node)));
        }
    };

    /**
     * Neuron node filter.
     */
    private static final AbstractFilter MODEL_GROUP_NODE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return ((node instanceof NeuronGroupNode) || (node instanceof SynapseGroupNode) || (node instanceof SubnetworkNode));
        }
    };

    /**
     * Neuron or synapse node filter. Don't worry about grouping for this.
     */
    private static final AbstractFilter NEURON_OR_SYNAPSE_NODE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return ((node instanceof NeuronNode) || (node instanceof SynapseNode));
        }
    };

    /**
     * Text object filter.
     */
    private static final AbstractFilter TEXT_NODE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return (node instanceof TextNode);
        }
    };

    /**
     * Helper method to determine if nodes are grouped.
     *
     * @param node node to check
     * @return whether the node is grouped or not
     */
    private static boolean isGrouped(final PNode node) {
        if (node instanceof ScreenElement) {
            return ((ScreenElement) node).isGrouped();
        }
        return false;
    }

    /**
     * Selectable filter.
     */
    private static final AbstractFilter SELECTABLE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return (((node instanceof NeuronNode) && (!isGrouped(node)))
                    || ((node instanceof SynapseNode) && (!isGrouped(node)))
                    || ((node instanceof WeightMatrixNode) && (!isGrouped(node)))
                    || ((node instanceof NeuronArrayNode) && (!isGrouped(node)))
                    || ((node instanceof DL4JMultiLayerNetworkNode) && (!isGrouped(node)))
                    || ((node instanceof TextNode) && (!isGrouped(node)))
                    || (node instanceof ViewGroupNode));
        }
    };

    /**
     * Subnetwork node filter.
     */
    private static final AbstractFilter PARENT_NODE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return ((node instanceof NeuronGroupNode) || (node instanceof SynapseGroupNode) || (node instanceof SubnetworkNode));
        }
    };

    public static AbstractFilter getNeuronNodeFilter() {
        return NEURON_NODE_FILTER;
    }

    public static AbstractFilter getNeuronOrSynapseNodeFilter() {
        return NEURON_OR_SYNAPSE_NODE_FILTER;
    }

    public static AbstractFilter getSynapseNodeFilter() {
        return SYNAPSE_NODE_FILTER;
    }

    public static AbstractFilter getModelGroupNodeFilter() {
        return MODEL_GROUP_NODE_FILTER;
    }

    public static AbstractFilter getParentNodeFilter() {
        return PARENT_NODE_FILTER;
    }

    public static AbstractFilter getSelectableFilter() {
        return SELECTABLE_FILTER;
    }

    public static AbstractFilter getTextNodeFilter() {
        return TEXT_NODE_FILTER;
    }

    public static AbstractFilter getNeuronGroupNodeFilter() {
        return NEURON_GROUP_NODE_FILTER;
    }

}