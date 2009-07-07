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
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Wraps a neuron object and provides all consuming and producing attributes.
 *
 * @author jyoshimi
 */
public class NeuronWrapper implements Producer, Consumer {

    /** Parent component. */
    private NetworkComponent parent;
    
    /** Wrapped Neuron. */
    private Neuron neuron;
    
    /** The producing attributes. */
    private ArrayList<ProducingAttribute<?>> producingAttributes
        = new ArrayList<ProducingAttribute<?>>();

    /** The consuming attributes. */
    private ArrayList<ConsumingAttribute<?>> consumingAttributes
        = new ArrayList<ConsumingAttribute<?>>();

    /**
     * Constructor.
     *
     * @param neuron neuron this wraps.
     */
    public NeuronWrapper(final Neuron neuron, final NetworkComponent component) {
        this.neuron = neuron;
        this.parent = component;
        ActivationAttribute activationAttribute = new ActivationAttribute();
        producingAttributes.add(activationAttribute);
        consumingAttributes.add(activationAttribute);

        UpperBoundAttribute upperBoundAttribute = new UpperBoundAttribute();
        producingAttributes.add(upperBoundAttribute);
        consumingAttributes.add(upperBoundAttribute);

        LowerBoundAttribute lowerBoundAttribute = new LowerBoundAttribute();
        producingAttributes.add(lowerBoundAttribute);
        consumingAttributes.add(lowerBoundAttribute);

        TargetValueAttribute targetValueAttribute = new TargetValueAttribute();
        producingAttributes.add(targetValueAttribute);
        consumingAttributes.add(targetValueAttribute);
    }
    
    /**
     * Returns the wrapped neuron.
     *
     * @return the neuron
     */
    public Neuron getNeuron() {
        return neuron;
    }
    
    /**
     * Sets the wrapped neuron.
     *
     * @param object theneuorn to set
     */
    public void setNeuron(final Neuron neuron) {
        this.neuron = neuron;
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
     * Implements the Activation attribute.
     * 
     * @author Matt Watson
     */
    private class ActivationAttribute extends AbstractAttribute 
            implements ProducingAttribute<Double>, ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "Activation";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return neuron.getActivation();
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            neuron.setInputValue(value == null ? 0 : value);
        }
        
        /**
         * {@inheritDoc}
         */
        public NeuronWrapper getParent() {
            return NeuronWrapper.this;
        }
    }
    
    /**
     * Implements the Upper bound attribute.
     * 
     * @author Matt Watson
     */
    private class UpperBoundAttribute extends AbstractAttribute implements ProducingAttribute<Double>,
            ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "UpperBound";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return neuron.getUpperBound();
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            neuron.setUpperBound(value);
        }
        
        /**
         * {@inheritDoc}
         */
        public NeuronWrapper getParent() {
            return NeuronWrapper.this;
        }

    }

    /**
     * Implements the Lower bound attribute.
     * 
     * @author Matt Watson
     */
    private class LowerBoundAttribute extends AbstractAttribute implements ProducingAttribute<Double>,
            ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "LowerBound";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return neuron.getLowerBound();
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            neuron.setLowerBound(value);
        }
        
        /**
         * {@inheritDoc}
         */
        public NeuronWrapper getParent() {
            return NeuronWrapper.this;
        }
    }

    /**
     * Implements the Target Value attribute.
     */
    private class TargetValueAttribute extends AbstractAttribute implements ProducingAttribute<Double>,
            ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getAttributeDescription() {
            return "TargetValue";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return neuron.getTargetValue();
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            neuron.setTargetValue(value);
        }
        
        /**
         * {@inheritDoc}
         */
        public NeuronWrapper getParent() {
            return NeuronWrapper.this;
        }

        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "TargetValue";
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return neuron.getId();
    }

    /**
     * {@inheritDoc}
     */
    public WorkspaceComponent getParentComponent() {
        return parent;
    }

}
