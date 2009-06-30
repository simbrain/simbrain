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
package org.simbrain.network.attributes;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Wraps a synapse object and provides all consuming and producing attributes.
 */
public class SynapseWrapper implements Producer, Consumer {

    /** Parent component. */
    private NetworkComponent parent;
    
    /** Wrapped Neuron. */
    private Synapse synapse;
    
    /** The producing attributes. */
    private ArrayList<ProducingAttribute<?>> producingAttributes
        = new ArrayList<ProducingAttribute<?>>();

    /** The consuming attributes. */
    private ArrayList<ConsumingAttribute<?>> consumingAttributes
        = new ArrayList<ConsumingAttribute<?>>();

    /**
     * Constructor.
     *
     * @param synapse synapse this wraps.
     * @param component parent component.
     */
    public SynapseWrapper(final Synapse synapse, final NetworkComponent component) {
        this.synapse = synapse;
        this.parent = component;
        StrengthAttribute activationAttribute = new StrengthAttribute();
        producingAttributes.add(activationAttribute);
        consumingAttributes.add(activationAttribute);
    }
    
    /**
     * Returns the wrapped neuron.
     *
     * @return the neuron
     */
    public Synapse getSynapse() {
        return synapse;
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<? extends ProducingAttribute<?>> getProducingAttributes() {
        return producingAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public final List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
        return consumingAttributes;
    }

    /**
     * Implements a Strength attribute.
     */
    private class StrengthAttribute extends AbstractAttribute
            implements ProducingAttribute<Double>, ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "Strength";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return synapse.getStrength();
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            synapse.setStrength(value == null ? 0 : value);
        }
        
        /**
         * {@inheritDoc}
         */
        public SynapseWrapper getParent() {
            return SynapseWrapper.this;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return synapse.getId();
    }

    /**
     * {@inheritDoc}
     */
    public WorkspaceComponent getParentComponent() {
        return parent;
    }
}
