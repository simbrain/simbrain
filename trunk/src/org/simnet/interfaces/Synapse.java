/*
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

import java.util.ArrayList;
import java.util.LinkedList;

import org.simbrain.simnet.WeightLearningRule;
import org.simnet.NetworkPreferences;
import org.simnet.synapses.*;
import org.simnet.synapses.rules.NoLearning;
import org.simnet.synapses.spikeresponders.*;
import org.simnet.util.UniqueID;

/**
 * <b>Weight</b> objects represent "connections" between neurons, which learn (grow or 
 * weaken) based on various factors, including the activation level of connected neurons.
 * Learning rules are defined in {@link WeightLearningRule}.
 */
public abstract class Synapse {

	protected Neuron source;
	protected Neuron target;

	protected SpikeResponder spikeResponder = null; //only used if source neuron is a spiking neuron
	protected String id = null;
	
	public final static int NUM_PARAMETERS = 8;

	public LearningRule currentLearningRule = new NoLearning();

	protected double strength = NetworkPreferences.getStrength();
	public double increment = 1;
	public double upperBound = 10;
	public double lowerBound = -10;
	
	private LinkedList delayManager = null;

	
	// List of synapse types 
	private static String[] typeList = { StandardSynapse.getName(),
            Hebbian.getName(), OjaSynapse.getName(), RandomSynapse.getName(),
            SubtractiveNormalizationSynapse.getName(),
            ClampedSynapse.getName(), ShortTermPlasticitySynapse.getName(),
            HebbianThresholdSynapse.getName() };	
	
	public Synapse() {
		id = UniqueID.get();
		setDelay(0);
	}
	
	/**
	 *  This constructor is used when creating a synapse of one type from another synapse of another type
	 *  Only values common to different types of synapse are copied
	 */
	public Synapse(Synapse s) {
		setStrength(s.getStrength());
		setUpperBound(s.getUpperBound());
		setLowerBound(s.getLowerBound());
		setIncrement(s.getIncrement());
		setSpikeResponder(s.getSpikeResponder());
		id = UniqueID.get();
	}
	
	public void init() {
		target.getFanIn().add(this);
		source.getFanOut().add(this);	
		setDelay(0);
	}
	
	/**
	 * Set a default spike responder if the source neuron is a 
	 * spiking neuron, else set the spikeResponder to null
	 */
	public void initSpikeResponder() {
		if (source instanceof SpikingNeuron) {
			setSpikeResponder(new Step());
		} else {
			setSpikeResponder(null);
		}
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
		s.setSpikeResponder(this.getSpikeResponder());
		return s;
	}
	
	public abstract void update();
	public abstract Synapse duplicate();
	
	

	/**
	 * For spiking source neurons, returns the spike-responder's value times the synapse strength
	 * For non-spiking neurons, returns the pre-synaptic activation times the synapse strength
	 */
	public double getValue() {
		double val;
		if (source instanceof SpikingNeuron) {
			spikeResponder.update();
			val= strength * spikeResponder.getValue();
		} else {
			val= source.getActivation() * strength;
		}
		if (delayManager == null) {
			return val;
		} else {
			enqueu(val);
			return dequeu();			
		}
	}

	/**
	 * @return the name of the class of this synapse
	 */
	public String getType() {
		return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
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
	 * If value is above or below its bounds set it to those bounds
	 */
	public double clip(double val) {

		if (val > upperBound) {
			val = upperBound;
		}

		if (val < lowerBound) {
			val = lowerBound;
		}
		
		return val;
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

	/**
	 * Helper function for combo boxes.  Associates strings with indices.
	 */	
	public static int getSynapseTypeIndex(String type) {
		for (int i = 0; i < typeList.length; i++) {
			if (type.equals(typeList[i])) {
				return i;
			}
		}
		return 0;
	}
	
	/**
	 * @return Returns the typeList.
	 */
	public static String[] getTypeList() {
		return typeList;
	}
	/**
	 * @return Returns the spikeResponder.
	 */
	public SpikeResponder getSpikeResponder() {
		return spikeResponder;
	}
	/**
	 * @param spikeResponder The spikeResponder to set.
	 */
	public void setSpikeResponder(SpikeResponder sr) {
		this.spikeResponder = sr;
		spikeResponder.setParent(this);
	}
	
	////////////////////
	//  Delay manager //
	////////////////////
	private void setDelay(int delay) {
		if (delay == 0) {
			delayManager = null;
			return;
		}
		delayManager = new LinkedList();
		delayManager.clear();
		for (int i = 0; i < delay; i++) {
			delayManager.add(new Double(0));
		}
	}
	private double dequeu() {
		return ((Double)delayManager.removeFirst()).doubleValue();
	}
	private void enqueu(double val) {
		delayManager.add(new Double(val));
	}
}
