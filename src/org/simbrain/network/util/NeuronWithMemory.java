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
package org.simbrain.network.util;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

/**
 * A neuron that stores a memory of its last activation.
 *
 * @author jyoshimi
 */
public class NeuronWithMemory extends Neuron {

    /** Memory of last activation. */
    private double lastActivation;

    /**
     * {@inheritDoc}
     */
    public NeuronWithMemory(Network parent, String updateRule) {
        super(parent, updateRule);
    }

    @Override
    public void setActivation(double act) {
        lastActivation = getActivation();
        super.setActivation(act);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.network.interfaces.Neuron#setBuffer(double)
     */
    @Override
    public void setBuffer(double d) {
        lastActivation = getActivation();
        super.setBuffer(d);
    }

    /**
     * @return the lastActivation
     */
    public double getLastActivation() {
        return lastActivation;
    }

}
