/**
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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

package org.simnet.interfaces;

import org.simnet.NetworkPreferences;
import org.simnet.interfaces.*;

/**
 * <b>Weight</b> objects represent "connections" between neurons, which learn (grow or 
 * weaken) based on various factors, including the activation level of connected neurons.
 * Learning rules are defined in {@link WeightLearningRule}.
 */
public class Synapse {

	private Neuron source;
	private Neuron target;

	public final static int NUM_PARAMETERS = 8;

	public LearningRule currentLearningRule = null;
	private double strength = NetworkPreferences.getStrength();
	public double increment = NetworkPreferences.getWtIncrement();
	public double upperBound = NetworkPreferences.getWtUpperBound();
	public double lowerBound = NetworkPreferences.getWtLowerBound();
	public double momentum = NetworkPreferences.getMomentum();
	
	//	private LinkedList recent_activity = new LinkedList(); //TODO
	//	private boolean stp_recent = false;
	//	private double last_value;
	
	// Minimum co-activity between neurons before reinforcement method called.  Add to prefs?

	/**
	 * Creates a weight of some value connecting two neurons
	 * 
	 * @param source source neuron
	 * @param target target neuron
	 * @param val initial weight value
	 */
	public Synapse(Neuron source, Neuron target, double val) {
		this.source = source;
		this.target = target;
		this.strength = val;
	}
	
	public Synapse() {
		System.out.println("Syanpse");
	}

	/**
	 * Static factory method used in lieu of clone, which creates duplicate weights.
	 * Used, for example, in copy/paste.
	 * 
	 * @param w weight to duplicate
	 * @return duplicate weight
	 */
	public static Synapse getDuplicate(Synapse w) {
		Synapse ret = new Synapse();
		ret.setParameters(w.getParameters());
		return ret;
	}

	/**
	 * Creates a weight using an array of values
	 * 
	 * @param values the parameter values for this weight
	 */
	public Synapse(String[] values) {
		setParameters(values);
	}
	
	/**
	 * Set the parameters for this weight (it's strength, learning rule, etc).
	 * 
	 * @param values an array of Strings containing new parameter settings
	 */
	public void setParameters(String[] values) {
		if (values.length < NUM_PARAMETERS)
			return;

		if (values[2] != null)
			currentLearningRule = null;
		if (values[3] != null)
			strength = Double.parseDouble(values[3]);
		if (values[4] != null)
			lowerBound = Double.parseDouble(values[4]);
		if (values[5] != null)
			upperBound = Double.parseDouble(values[5]);
		if (values[6] != null)
			increment = Double.parseDouble(values[6]);
		if (values[7] != null)
			momentum = Double.parseDouble(values[7]);

	}

	/**
	 * Get the parameters for this weight (it's strength, learning rule, etc).
	 * 
	 * @return  an array of Strings containing parameter settings for this weight
	 */
	public String[] getParameters() {
		return new String[] {
			"","",
			getLearningRule().getName(),
			Double.toString(getStrength()),
			Double.toString(getLowerBound()),
			Double.toString(getUpperBound()),
			Double.toString(getIncrement()),
			Double.toString(getMomentum())
		};
	}
	/**
	 * Creates a weight connecting source and target neurons
	 * 
	 * @param source source neuron
	 * @param target target neuron
	 */
	public Synapse(Neuron source, Neuron target) {
		this.source = source;
		this.target = target;
	}

	public void setLearningRule(LearningRule rule) {
		currentLearningRule = rule;
	}
	public LearningRule getLearningRule() {
		return currentLearningRule;
	}
	public double getStrength() {
		return strength;
	}

	public Neuron getSource() {
		return source;
	}

	public void setSource(Neuron n) {
		this.source = n;
	}

	public Neuron getTarget() {
		return target;
	}

	public void setTarget(Neuron n) {
		this.target = n;
	}

	public void setStrength(double wt) {
		strength = wt;
	}

	public double getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(double d) {
		upperBound = d;
	}

	public double getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(double d) {
		lowerBound = d;
	}
	public double getMomentum() {
		return momentum;
	}

	public void setMomentum(double d) {
		momentum = d;
	}
	public double getIncrement() {
		return increment;
	}

	public void setIncrement(double d) {
		increment = d;
	}

	/**
	 * Increment this weight by increment
	 */
	public void incrementWeight() {
		if(strength < upperBound)  {
			strength += increment;
		}
	}

	/**
	 * Decrement this weight by increment
	 */
	public void decrementWeight() {
		if(strength > lowerBound)  {
			strength -= increment;
		}
	}

	/**
	 * Increase the absolute value of this weight by increment amount
	 */
	public void reinforce() {
		if (strength > 0) {
			incrementWeight();
		} else if (strength < 0) {
			decrementWeight();
		} else if (strength == 0) {
			strength = 0;
		}
	}

	/**
	 * Decrease the absolute value of this weight by increment amount
	 */
	public void weaken() {
		if (strength > 0) {
			decrementWeight();
		} else if (strength < 0) {
			incrementWeight();
		} else if (strength == 0) {
			strength = 0;
		}
	}

	/**
	 * Randomize this weight to a value between its upper and lower bounds
	 */
	public void randomize() {
		strength = ((upperBound - lowerBound) * Math.random() + lowerBound);
	}

	public void update() {
		//updateActivityWindow();
		//currentLearningRule.apply(this);
	}


	/**
	 * If weight  value is above or below its bounds set it to those bounds
	 */
	public void checkBounds() {

		if (strength > upperBound) {
			strength = upperBound;
		}

		if (strength < lowerBound) {
			strength = lowerBound;
		}
	}
	
	//	//Can keep similar lists at the neurons
	//	public void updateActivityWindow() {
	//		recent_activity.addFirst(new Double(target.getActivation() * source.getActivation()));
	//		if (recent_activity.size() > 5) {
	//			recent_activity.removeLast();
	//		}
	//	}
	//	
	//	public double getSumRecent() {
	//		Iterator it = recent_activity.iterator();
	//		double ret = 0;
	//		while (it.hasNext()) {
	//			ret += ((Double)it.next()).doubleValue();
	//		}
	//		return ret;
	//	}
	//
	//
	//	/**
	//	 * @return
	//	 */
	//	public double getLast_value() {
	//		return last_value;
	//	}
	//
	//	/**
	//	 * @return
	//	 */
	//	public boolean isStp_recent() {
	//		return stp_recent;
	//	}
	//
	//	/**
	//	 * @param d
	//	 */
	//	public void setLast_value(double d) {
	//		last_value = d;
	//	}
	//
	//	/**
	//	 * @param b
	//	 */
	//	public void setStp_recent(boolean b) {
	//		stp_recent = b;
	//	}

}
