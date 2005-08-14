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
import org.simbrain.world.Agent;
import org.simbrain.world.World;

/**
 * <b>Coupling</b> represents a relation between an agent and input or output node.
 */
public class Coupling {

	//Used by Castor
	private String agentName;
	private String worldName;
	private String worldType;
	private String neuronName;
	private String networkName;
	
	// References to coupled objects
	private Agent agent;
	private PNodeNeuron neuron;
	
	public Coupling() {	
		initCastor();
	}
	
	public Coupling(Agent a, PNodeNeuron n) {
		setAgent(a);
		setNeuron(n);
		initCastor();
	}
	
	public Coupling(PNodeNeuron n) {
		setNeuron(n);
		initCastor();
	}
	
	public Coupling(Agent a) {
		setAgent(a);
		initCastor();
	}
	
	
	/**
	 * @return Returns the agent.
	 */
	public Agent getAgent() {
		return agent;
	}
	
	public World getWorld() {
		if (agent == null) return null;
		return agent.getParentWorld();
	}
	
	/**
	 * @param agent The agent to set.
	 */
	public void setAgent(Agent agent) {
		this.agent = agent;
	}
	
	public void initCastor() {
		if(neuron == null) return;
		setNeuronName(neuron.getNeuron().getId());		
		if(neuron.getParentPanel() == null) return;
		setNetworkName(neuron.getParentPanel().getName());		
		if(agent == null) return;
		setAgentName(agent.getName());
		if(agent.getParentWorld() == null) return;
		setWorldName(agent.getParentWorld().getName());
		setWorldType(agent.getParentWorld().getType());
	}
	
	/**
	 * @return Returns the agentName.
	 */
	public String getAgentName() {
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
		return worldName;
	}
	/**
	 * @param worldName The worldName to set.
	 */
	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}
	
	/**
	 * @return Returns the worldType.
	 */
	public String getWorldType() {
		return worldType;
	}
	/**
	 * @param worldType The worldType to set.
	 */
	public void setWorldType(String worldType) {
		this.worldType = worldType;
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

		System.out.println("\t Coupling information:");								
		if (getNeuron() == null) {
			System.out.println("\t PNode Neuron: null");						
		} else {
			System.out.println("\t PNode Neuron: " + getNeuron().getId());			
			System.out.println("\t Network: " + getNeuron().getParentPanel().getName());				
		}
		if (getAgent() == null) {
			System.out.println("\t Agent: null");			
			System.out.println("\t World: " + worldName);				
			System.out.println("\t Type: " + worldType);
		} else {
			System.out.println("\t Agent: " + getAgent().getName());
			System.out.println("\t World: " + getAgent().getParentWorld().getName());				
			System.out.println("\t Type: " + getAgent().getParentWorld().getType());
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

    /**
     * @return true if this coupling has an associated agent, false  otherwise
     */
    public boolean isAttached() {
        if (this.getAgent() ==  null){
            return false;
        }
        else
        	return true;
    }

}
