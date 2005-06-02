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

import org.simbrain.world.Agent;

/**
 * @author yoshimi
 *
 * <b>Coupling</b> represents a relation between an agent and input or output node.
 * 
 */
public class Coupling {

	//Used by Castor
	private String agentName;
	private String worldName;
	
	private Agent agent;
	
	public Coupling() {	
	}
	
	public Coupling(Agent a) {
		setAgent(a);
	}
	
	
	/**
	 * @return Returns the agent.
	 */
	public Agent getAgent() {
		return agent;
	}
	/**
	 * @param agent The agent to set.
	 */
	public void setAgent(Agent agent) {
		this.agent = agent;
		initCastor();
	}
	
	public void initCastor() {
		if(agent == null) return;
		setAgentName(agent.getName());
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

}
