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
package org.simbrain.network.interfaces;

/**
 * <b>SpikingNeuron</b> is the superclass for spiking neuron types (e.g.
 * integrate and fire) with functions common to spiking neurons. For example a
 * boolean has spiked field is used in the gui to indicate that this neuron has
 * spiked.
 */
public abstract class SpikingNeuronUpdateRule implements NeuronUpdateRule {


    /** Parent neuron. */
    private Neuron parentNeuron;

    /** Time of last spike.  */
    private double lastSpikeTime;

    /** Whether a spike has occurred in the current time. */
    private boolean hasSpiked;

    /**
     * @param hasSpiked the hasSpiked to set
     */
    public void setHasSpiked(boolean hasSpiked) {
        if (hasSpiked == true) {
            lastSpikeTime = parentNeuron.getParentNetwork().getRootNetwork().getTime();
        }
        this.hasSpiked = hasSpiked;
    }

    /**
     * Whether the neuron has spiked in this instant or not.
     *
     * @return true if the neuron spiked.
     */
    public boolean hasSpiked() {
        return hasSpiked;
    }

    /**
     * @return the lastSpikeTime
     */
    public double getLastSpikeTime() {
        return lastSpikeTime;
    }

    /**
     * @return the parentNeuron
     */
    public Neuron getParentNeuron() {
        return parentNeuron;
    }

    /**
     * @param parentNeuron the parentNeuron to set
     */
    public void setParentNeuron(Neuron parentNeuron) {
        this.parentNeuron = parentNeuron;
    }

    /**
     * @return the hasSpiked
     */
    public boolean isHasSpiked() {
        return hasSpiked;
    }

    /**
     * @param lastSpikeTime the lastSpikeTime to set
     */
    public void setLastSpikeTime(double lastSpikeTime) {
        this.lastSpikeTime = lastSpikeTime;
    }

    /**
     * {@inheritDoc}
     */
    public int getTimeType() {
        return org.simbrain.network.interfaces.RootNetwork.CONTINUOUS;
    }

    /**
     * {@inheritDoc}
     */
    public void init(Neuron neuron) {
        setParentNeuron(neuron); 
    }

    /**
     * {@inheritDoc}
     */
    public abstract void update(Neuron neuron);

    /**
     * {@inheritDoc}
     */
    public abstract String getName();
}
