/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.neurons;

import org.simnet.interfaces.Neuron;


/**
 * <b>TraceNeuron</b>.
 */
public class TraceNeuron extends Neuron {

    /** C1 value. */
    private double c1 = .5;

    /** C2 value. */
    private double c2  = .5;
    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public TraceNeuron() {
    }

    /**
     * TODO: Not really true...
     * @return time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.Network.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to make the type
     */
    public TraceNeuron(final Neuron n) {
        super(n);
    }

    /**
     * Returns a duplicate TraceNeuron (used, e.g., in copy/paste).
     * @return Duplicated neuron
     */
    public Neuron duplicate() {
        TraceNeuron tn = new TraceNeuron();
        tn = (TraceNeuron) super.duplicate(tn);
        tn.setC1(getC1());
        tn.setC2(getC2());
        return tn;
    }

    /**
     * Update neuron.
     */
    public void update() {
        setBuffer(activation);
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Trace";
    }

    /**
     * @return c1 value.
     */
    public double getC1() {
        return c1;
    }

    /**
     * Sets the c1 value.
     *
     * @param c1 value to be set
     */
    public void setC1(final double c1) {
        this.c1 = c1;
    }

    /**
     * @return c2 value.
     */
    public double getC2() {
        return c2;
    }

    /**
     * Sets the c2 value.
     *
     * @param c2 value to set
     */
    public void setC2(final double c2) {
        this.c2 = c2;
    }
}
