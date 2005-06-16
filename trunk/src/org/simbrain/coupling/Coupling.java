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
package org.simbrain.coupling;

import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.world.odorworld.OdorWorldAgent;
import org.simbrain.world.odorworld.OdorWorld;

/**
 * <b>Coupling</b> represents a relation between an agent and input or output node.
 */
public class Coupling {

	//Used by Castor
	private String agentName;
	private String worldName;
	private String neuronName;
	private String networkName;
	
	private OdorWorldAgent agent;
	private PNodeNeuron neuron;
	
	public Coupling() {	
	}
	
	public Coupling(OdorWorldAgent a, PNodeNeuron n) {
		setAgent(a);
		setNeuron(n);
	}
	
	public Coupling(PNodeNeuron n) {
		setNeuron(n);
	}
	
	public Coupling(OdorWorldAgent a) {
		setAgent(a);
	}
	
	
	/**
	 * @return Returns the agent.
	 */
	public OdorWorldAgent getAgent() {
		return agent;
	}
	
	public OdorWorld getWorld() {
		if (agent == null) return null;
		return agent.getParent();
	}
	
	/**
	 * @param agent The agent to set.
	 */
	public void setAgent(OdorWorldAgent agent) {
		this.agent = agent;
		initCastor();
	}
	
	public void initCastor() {
		if(neuron == null) return;
		setNeuronName(neuron.getNeuron().getId());		
		if(neuron.getParentPanel() == null) return;
		setNetworkName(neuron.getParentPanel().getName());		
		if(agent == null) return;
		setAgentName(agent.getName());
		if(agent.getParent() == null) return;
		setWorldName(agent.getParent().getName());
	}
	
	/**
	 * @return Returns the agentName.
	 */
	public String getAgentName() {
//		if (agent != null) 
//			return agent.getName();
		return agentName;
	}
	/**
	 * @param agentName The agentName to set.
	 */
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}
	/**
	 * @return Returns the worldName.
	 */
	public String getWorldName() {
//		if(agent.getParent() != null) {
//			return agent.getParent().getName();
//		else 
		return worldName;
	}
	/**
	 * @param worldName The worldName to set.
	 */
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	/**
	 * @return Returns the neuron.
	 */
	public PNodeNeuron getNeuron() {
		return neuron;
	}
	/**
	 * @param neuron The neuron to set.
	 */
	public void setNeuron(PNodeNeuron neuron) {
		this.neuron = neuron;
	}
	/**
	 * @return Returns the neuronName.
	 */
	public String getNeuronName() {
		return neuronName;
	}
	/**
	 * @param neuronName The neuronName to set.
	 */
	public void setNeuronName(String neuronName) {
		this.neuronName = neuronName;
	}
	
	public void debug() {
		if (getNeuron() == null) {
			System.out.println("Neuron: null");						
		} else {
			System.out.println("Neuron: " + getNeuron().getId());			
			System.out.println("Network: " + getNeuron().getParentPanel().getName());				
		}
		if (getAgent() == null) {
			System.out.println("Agent: null");			
		} else {
			System.out.println("Agent: " + getAgent().getName());
			System.out.println("World: " + getAgent().getParent().getName());				
		}
	}
	
	/**
	 * @return Returns the networkName.
	 */
	public String getNetworkName() {
		return networkName;
	}
	/**
	 * @param networkName The networkName to set.
	 */
	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}
}
