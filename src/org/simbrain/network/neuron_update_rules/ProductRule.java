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
package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * <b>Product rule</b> units compute the product of the activations of incoming
 * units.  Used in "Long Short Term Memory" and "Sigma-Pi" networks.
 */
public class ProductRule extends LinearRule {

    /** Whether to use weights by default. */
    private static final boolean DEFAULT_USE_WEIGHTS = false;
    
    /** Whether to use weights or not. */
    private boolean useWeights = DEFAULT_USE_WEIGHTS;

    @Override
    public ProductRule deepCopy() {
        ProductRule pr = new ProductRule();
        pr.setUseWeights(getUseWeights());
        pr.setClipped(isClipped());
        pr.setAddNoise(getAddNoise());
        pr.setUpperBound(getUpperBound());
        pr.setLowerBound(getLowerBound());
        pr.setNoiseGenerator(getNoiseGenerator());
        return pr;
    }

    @Override
    public void update(Neuron neuron) {

        double val = 1;
        if (useWeights) {
            for (Synapse s : neuron.getFanIn()) {
                val *= s.getPsr();
            }            
        } else {
            for (Synapse s : neuron.getFanIn()) {
                val *= s.getSource().getActivation();
            }            
        }
        // Special case of isolated neuron
        if (neuron.getFanIn().size() == 0) {
            val = 0;
        }

        if (this.getAddNoise()) {
            val += getNoiseGenerator().getRandom();
        }

        if (this.isClipped()) {
            val = clip(val);
        }

        neuron.setBuffer(val);
    }

    /**
     * @return the useWeights
     */
    public boolean getUseWeights() {
        return useWeights;
    }

    /**
     * @param useWeights the useWeights to set
     */
    public void setUseWeights(boolean useWeights) {
        this.useWeights = useWeights;
    }

    @Override
    public String getDescription() {
        return "Product";
    }
}
