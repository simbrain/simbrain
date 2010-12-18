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
package org.simbrain.network.neurons;

import org.simbrain.network.interfaces.Neuron;

/**
 * <b>TraceNeuron</b> is used to model Sutton and Barto's model of conditioning
 * via "eligibility traces."
 */
public class TraceNeuron extends LinearNeuron {

    /** C1 value. */
    private double c1 = .5;

    /** C2 value. */
    private double c2  = .5;

    /** "Trace" of recent activity. */
    private double trace = 0;

    /**
     * Used to keep a history of traces so that this information is available
     * before the neuron is updated.
     */
    private double tracehistory = 0;

//    /**
//     * Returns a duplicate TraceNeuron (used, e.g., in copy/paste).
//     * @return Duplicated neuron
//     */
//    public TraceNeuron duplicate() {
//        TraceNeuron tn = new TraceNeuron();
//        tn = (TraceNeuron) super.duplicate(tn);
//        tn.setC1(getC1());
//        tn.setC2(getC2());
//        return tn;
//    }

    @Override
    public void update(Neuron neuron) {
        tracehistory = trace;
        trace = c1 * trace +  c2 * neuron.getActivation();
        super.update(neuron);
    }

    @Override
    public String getName() {
        return "Trace";
    }

//  /** @see Neuron */
//  public String getToolTipText() {
//      return "" + this.getActivation() + " trace = " + trace;
//  }

//    /** @see Neuron */
//    public void clear() {
//        activation = 0;
//        trace = 0;
//    }

    /**
     * Returns the difference between the predicted and actual output of this
     * neuron.
     * 
     * @return activation - trace
     */
    public double getDifference(Neuron neuron) {
        return neuron.getActivation()- trace;
    }

    /**
     * Returns the difference between the predicted and actual output of this
     * neuron.
     *
     * @return activation - tracehistory
     */
    public double getDifferenceHistory(Neuron neuron) {
        return neuron.getActivation() - tracehistory;
    }

    /**
     * @return Returns the trace.
     */
    public double getTrace() {
        return trace;
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

    /**
     * @return Returns the tracehistory.
     */
    public double getTraceHistory() {
        return tracehistory;
    }
}
