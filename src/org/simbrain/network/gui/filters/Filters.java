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

import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.ScreenElement;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.network.gui.nodes.TextObject;
import org.simbrain.network.gui.nodes.ViewGroupNode;

import edu.umd.cs.piccolo.PNode;

/**
 * Filters.
 */
public final class Filters {

    /** Neuron node filter. */
    private static final AbstractFilter NEURON_NODE_FILTER = new AbstractFilter() {
            /** @see AbstractFilter */
            public boolean accept(final PNode node) {
                return ((node instanceof NeuronNode) && (!isGrouped(node)));
            }
        };

    /** Synapse node filter. */
    private static final AbstractFilter SYNAPSE_NODE_FILTER = new AbstractFilter() {
            /** @see AbstractFilter */
            public boolean accept(final PNode node) {
                return ((node instanceof SynapseNode) && (!isGrouped(node)));
            }
        };

        /** Neuron node filter. */
    private static final AbstractFilter MODEL_GROUP_NODE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return (node instanceof GroupNode);
        }
    };

    /** Neuron or synapse node filter. Don't worry about grouping for this. */
    private static final AbstractFilter NEURON_OR_SYNAPSE_NODE_FILTER = new AbstractFilter() {
         /** @see AbstractFilter */
         public boolean accept(final PNode node) {
             return ((node instanceof NeuronNode) || (node instanceof SynapseNode));
         }
     };

     /** Text object filter. */
     private static final AbstractFilter TEXT_OBJECT_FILTER = new AbstractFilter() {
         /** @see AbstractFilter */
         public boolean accept(final PNode node) {
             return (node instanceof TextObject);
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

        /** Selectable filter. */
    private static final AbstractFilter SELECTABLE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
                return (((node instanceof NeuronNode) && (!isGrouped(node)))
                        || ((node instanceof SynapseNode) && (!isGrouped(node)))
                        || ((node instanceof TextObject) && (!isGrouped(node)))
                        || ((node instanceof SubnetworkNode) && (!isGrouped(node)))
                        || (node instanceof ViewGroupNode));
            }
    };

    /** Subnetwork node filter. */
    private static final AbstractFilter SUBNETWORK_NODE_FILTER = new AbstractFilter() {
            /** @see AbstractFilter */
            public boolean accept(final PNode node) {
                return (node instanceof SubnetworkNode);
            }
        };

        /** Subnetwork node filter. */
    private static final AbstractFilter PARENT_NODE_FILTER = new AbstractFilter() {
        /** @see AbstractFilter */
        public boolean accept(final PNode node) {
            return ((node instanceof SubnetworkNode) || (node instanceof GroupNode));
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

    /**
     * Return the model group node filter.
     *
     * @return the model group node filter
     */
    public static AbstractFilter getModelGroupNodeFilter() {
        return MODEL_GROUP_NODE_FILTER;
    }

    /**
     * Return the parent node filter.
     *
     * @return the parent node filter
     */
    public static AbstractFilter getParentNodeFilter() {
        return PARENT_NODE_FILTER;
    }

    /**
     * Return the parent node filter.
     *
     * @return the parent node filter
     */
    public static AbstractFilter getSelectableFilter() {
        return SELECTABLE_FILTER;
    }

    /**
     * Return the text object filter.
     *
     * @return the text object filter
     */
    public static AbstractFilter getTextObjectFilter() {
        return TEXT_OBJECT_FILTER;
    }
}