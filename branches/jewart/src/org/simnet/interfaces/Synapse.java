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
import org.simnet.synapses.rules.NoLearning;

/**
 * <b>Weight</b> objects represent "connections" between neurons, which learn (grow or 
 * weaken) based on various factors, including the activation level of connected neurons.
 * Learning rules are defined in {@link WeightLearningRule}.
 */
public abstract class Synapse {

	protected Neuron source;
	protected Neuron target;

	protected String id = null;
	
	public final static int NUM_PARAMETERS = 8;

	public LearningRule currentLearningRule = new NoLearning();

	protected double strength = NetworkPreferences.getStrength();
	public double increment = NetworkPreferences.getWtIncrement();
	public double upperBound = NetworkPreferences.getWtUpperBound();
	public double lowerBound = NetworkPreferences.getWtLowerBound();
	
	
	public Synapse() {
	}
	
	public void init() {
		target.getFanIn().add(this);
		source.getFanOut().add(this);	
	}
	

	/**
	 * Create duplicate weights.
	 * Used in copy/paste.
	 * 
	 * @param w weight to duplicate
	 * @return duplicate weight
	 */
	public Synapse duplicate(Synapse s) {
		s.setStrength(this.getStrength());
		s.setIncrement(this.getIncrement());
		s.setLearningRule(this.getLearningRule());
		s.setUpperBound(this.getUpperBound());
		s.setLowerBound(this.getLowerBound());
		return s;
	}
	
	public abstract void update();
	public abstract Synapse duplicate();


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
	
	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	public void setLearningRule(LearningRule rule) {
		currentLearningRule = rule;
	}
	public LearningRule getLearningRule() {
		return currentLearningRule;
	}

	
	public void setLearningRuleS(String name) {
		currentLearningRule = LearningRule.getLearningRule(name);
	}
	public String getLearningRuleS() {
		return currentLearningRule.getName();
	}

}
