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
package org.simbrain.network.core;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.util.Utils;

/**
 * A rule for updating a neuron.
 *
 * @author jyoshimi
 */
public abstract class NeuronUpdateRule {

    /**
     * An enum specifying how a neuron sums its inputs. The enum both specifies
     * and provides the appropriate method for the distinct ways this can
     * happen. At its core it represents the connectionist (matrix
     * multiplication equivalent/algebraic) vs biological (convolution or other
     * function of a "spike" represented as a Dirac delta function) weighted
     * sums.
     *
     * @author Zach Tosi
     */
    public static enum InputType {
        WEIGHTED {
            /**
             * Gets the weighted sum of the pre-synaptic neurons' activation
             * values.
             */
            @Override
            public double getInput(Neuron n) {
                return n.getWeightedInputs();
            }

			@Override
			public double[] getSeparatedInput(Neuron n) {
				double [] ei = new double[2];
				for (Synapse s : n.getFanIn()) {
					double wt = s.calcWeightedSum();
					if (wt > 0) {
						ei[0] += wt;
					} else {
						ei[1] += wt;
					}
				}
				return ei;
			}
			
			@Override
			public double[] getNormalizedSeparatedInput(Neuron n) {
				double [] ei = new double[2];
				double e = 0;
				double i = 0;
				for (Synapse s : n.getFanIn()) {
					double wt = s.calcWeightedSum();
					if (wt > 0) {
						ei[0] += wt;
						e++;
					} else {
						ei[1] += wt;
						i++;
					}
				}
				if (e > 1) {
					ei[0] /= e;
				}
				if (i > 1) {
					ei[1] /= i;
				}
				return ei;
			}
			
            @Override
            public String toString() {
                return "Weighted";
            }

        },
        SYNAPTIC {
            /**
             * Gets the synaptic sum of the pre-synaptic neurons' firing state
             * weighted by synapses and processed through a spike responder.
             */
            @Override
            public double getInput(Neuron n) {
                return n.getSynapticInput();
            }
            
			@Override
			public double[] getSeparatedInput(Neuron n) {
				double [] ei = new double[2];
				for (Synapse s : n.getFanIn()) {
					double psr = s.calcPSR();
					if (psr > 0) {
						ei[0] += psr;
					} else {
						ei[1] += psr;
					}
				}
				return ei;
			}


			@Override
			public double[] getNormalizedSeparatedInput(Neuron n) {
				double [] ei = new double[2];
				double e = 0;
				double i = 0;
				for (Synapse s : n.getFanIn()) {
					double psr = s.calcPSR();
					if (psr > 0) {
						ei[0] += psr;
						e++;
					} else {
						ei[1] += psr;
						i++;
					}
				}
				if (e > 1) {
					ei[0] /= e;
				}
				if (i > 1) {
					ei[1] /= i;
				}
				return ei;
			}
			
            @Override
            public String toString() {
                return "Synaptic";
            }


        };
        
        /**
         * Returns the total input to a neuron using either a post-synaptic
         * response value calculated from each synapse and derived from the
         * spike-train of the pre-synaptic neuron or a simple weighted sum of
         * the product of the synapse values and their respective source
         * neurons.
         * @param n
         * @return
         */
        public abstract double getInput(Neuron n);
        
        /**
         * Returns the total excitatory and inhibitory inputs to a neuron
         * separated. Otherwise the same as {@link #getInput(Neuron)}.
         * @param n
         * @return
         */
        public abstract double [] getSeparatedInput(Neuron n);
        
        public abstract double [] getNormalizedSeparatedInput(Neuron n);
    }

    /** The maximum number of digits to display in the tool tip. */
    private static final int MAX_DIGITS = 9;

    /** The default increment of a neuron using this rule. */
    public static final double DEFAULT_INCREMENT = 0.1;

    protected InputType inputType = InputType.WEIGHTED;

    /** Amount by which to increment or decrement neuron. */
    protected double increment = DEFAULT_INCREMENT;

    /**
     * Returns the type of time update (discrete or continuous) associated with
     * this neuron.
     *
     * @return the time type
     */
    public abstract TimeType getTimeType();

    /**
     * Apply the update rule.
     *
     * @param neuron parent neuron
     */
    public abstract void update(Neuron neuron);

    /**
     * Returns a deep copy of the update rule.
     *
     * @return Duplicated update rule
     */
    public abstract NeuronUpdateRule deepCopy();

    /**
     * Increment a neuron by increment.
     * @param n neuron
     */
    public final void incrementActivation(Neuron n) {
        n.forceSetActivation(n.getActivation() + increment);
        n.getNetwork().fireNeuronChanged(n);
    }

    /**
     * Decrement a neuron by increment.
     * @param n neuron
     */
    public final void decrementActivation(Neuron n) {
        n.forceSetActivation(n.getActivation() - increment);
        n.getNetwork().fireNeuronChanged(n);
    }

    /**
     * Increment a neuron by increment, respecting neuron specific constraints.
     * Intended to be overriden.
     * @param n neuron to be incremented 
     */
    public void contextualIncrement(Neuron n) {
        incrementActivation(n);
    }

    /**
     * Decrement a neuron by increment, respecting neuron specific constraints.
     * Intended to be overriden.
     * @param n neuron
     */
    public void contextualDecrement(Neuron n) {
        decrementActivation(n);
    }

    /**
     * Returns a random value between the upper and lower bounds of this neuron.
     * Update rules that require special randomization should override this
     * method.
     *
     * @return the random value.
     */
    public double getRandomValue() {
        if (this instanceof BoundedUpdateRule) {
            return (((BoundedUpdateRule) this).getUpperBound() - ((BoundedUpdateRule) this)
                    .getLowerBound())
                    * Math.random()
                    + ((BoundedUpdateRule) this).getLowerBound();
        } else {
            return 2 * Math.random() - 1;
        }

    }

    /**
     * Returns a value for lower bound to be used in computing the saturation of
     * neuron nodes. Override this to produce nicer graphics, and fine tune
     * based on display of neurons in common use cases for a given neuron type.
     *
     * @return the graphical lower bound
     */
    public double getGraphicalLowerBound() {
        if (this instanceof BoundedUpdateRule) {
            return ((BoundedUpdateRule) this).getLowerBound();
        } else {
            return -1;
        }
    }

    /**
     * Returns a value for upper bound to be used in computing the saturation of
     * neuron nodes. Override this to produce nicer graphics, and fine tune
     * based on display of neurons in common use cases for a given neuron type.
     *
     * @return the graphical upper bound
     */
    public double getGraphicalUpperBound() {
        if (this instanceof BoundedUpdateRule) {
            return ((BoundedUpdateRule) this).getUpperBound();
        } else {
            return 1;
        }
    }

    /**
     * Set activation to 0; override for other "clearing" behavior (e.g. setting
     * other variables to 0. Called in Gui when "clear" button pressed.
     *
     * @param neuron reference to parent neuron
     */
    public void clear(final Neuron neuron) {
        neuron.forceSetActivation(0);
    }

    /**
     * Returns a brief description of this update rule. Used in combo boxes in
     * the GUI.
     *
     * @return the description.
     */
    public abstract String getDescription();

    /**
     * Returns string for tool tip or short description. Override to provide
     * custom information.
     *
     * @param neuron reference to parent neuron
     * @return tool tip text
     */
    public String getToolTipText(final Neuron neuron) {
        return "(" + neuron.getId() + ") Activation: "
                + Utils.round(neuron.getActivation(), MAX_DIGITS);
    }

    /**
     * @return the increment
     */
    public double getIncrement() {
        return increment;
    }

    /**
     * @param increment the increment to set
     */
    public void setIncrement(double increment) {
        this.increment = increment;
    }

    /**
     * @return the defaultIncrement
     */
    public static double getDefaultIncrement() {
        return DEFAULT_INCREMENT;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public boolean isSpikingNeuron() {
    	return false;
    }

    public boolean isSkipsSynapticUpdates() {
    	return false;
    }
    
}
