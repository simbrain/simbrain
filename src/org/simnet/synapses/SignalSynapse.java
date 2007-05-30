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
package org.simnet.synapses;

import java.lang.reflect.Method;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;


/**
 * <b>SignalSynapse</b> is a catchall name for a connection which carries a special signal,
 * e.g. a reward signal, a training signal, or some other value that a source or target neuronn
 * could make use of.  Currently used by LMS neuron.  If the label is filled in it can also be used to
 * "measure" activity in another cell.
 */
public class SignalSynapse extends Synapse {

    /** Signal synapse label.  If not blank, used to retrieve a value from the source neuron via reflection. */
    private String label = "";

    /** Buffer for retrieved value from source neuron. */
    private double val;

    /**
     * Creates a weight of some value connecting two neurons.
     *
     * @param src source neuron
     * @param tar target neuron
     * @param val initial weight value
     * @param theId Id of synapse
     */
    public SignalSynapse(final Neuron src, final Neuron tar, final double val, final String theId) {
    	setSource(src);
        setTarget(tar);
        strength = val;
        id = theId;
        this.setSendWeightedInput(false);
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public SignalSynapse(final Neuron source, final Neuron target) {
    	setSource(source);
        setTarget(target);
        this.setSendWeightedInput(false);
    }

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public SignalSynapse() {
        super();
        this.setSendWeightedInput(false);
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param s Synapse to make of the type
     */
    public SignalSynapse(final Synapse s) {
        super(s);
        this.setSendWeightedInput(false);
    }

    /**
     * @return duplicate ClampedSynapse (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        SignalSynapse cs = new SignalSynapse();
        cs = (SignalSynapse) super.duplicate(cs);
        cs.setLabel(getLabel());

        return cs;
    }

    /**
     * Update the synapse.
     */
    public void update() {
        if (!label.equalsIgnoreCase("")) {
            Class neuronClass = getSource().getClass();
            Method theMethod = null;
            try {
                theMethod = neuronClass.getMethod(label, (Class[]) null);
            } catch (SecurityException e1) {
                e1.printStackTrace();
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
            }
            try {
                val = ((Double) theMethod.invoke(getSource(), (Object[]) null)).doubleValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            val = getSource().getActivation();
        }
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Signal";
    }

    /** @see Synapse */
    public double getValue() {
        return 0;
    }

    /**
     * @return the signal synapse label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the signal synapse label.
     *
     * @param label Signal synapse label
     */
    public void setLabel(final String label) {
        this.label = label;
    }
}
