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
package org.simbrain.workspace;

import java.util.ArrayList;
import org.simbrain.coupling.*;
import org.simbrain.network.*;
import org.simbrain.network.pnodes.*;
import org.simbrain.world.odorworld.Agent;
import org.simbrain.world.odorworld.World;

/**
 * <b>CouplingList</b>  is a beefed-up ArrayList which allows specific operations on the workspace's
 * list of coulings, e.g. getting all couplings which link to a specific network or agent, or all those
 * currently not connectd to any world..
 */
public class CouplingList extends ArrayList {
	
	/**
	 * Return a coupling. 
	 * 
	 * @param i index of coupling
	 * @return the coupling itself.
	 */
	public Coupling getCoupling(int i) {
		return (Coupling)this.get(i);
	}
	
	/**
	 * Return the neuron pnode associated with a coupling
	 * 
	 * @param i index of coupling
	 * @return a pnodeNeuron
	 */
	public PNodeNeuron getPNodeNeuron(int i) {
		return getCoupling(i).getNeuron();
	}

	/**
	 * Perform necessary initialization (for Castor-based persistence) on 
	 * all coupling objects in the list 
	 */
	public void initCastor() {
		for (int i = 0; i < this.size(); i++ ) {
			getCoupling(i).initCastor();
		}
	}

	/**
	 * Get all PNodeNeurons associated with couplings on a specific neural network.
	 * 
	 * @param n the neural network
	 * @return a list of of neurons in that neural network which are contained in the coupling list
	 */
	public ArrayList getNeurons(NetworkPanel n) {
		ArrayList ret = new ArrayList();
		
		for (int i = 0; i < n.getPNodeNeurons().size(); i++ ) {
			for (int j = 0; j < this.size(); j++ ) {
				if(getPNodeNeuron(j) == (PNodeNeuron)n.getPNodeNeurons().get(i)) {
					ret.add(getPNodeNeuron(j));
				}
					
			}
		}
		return ret;
	}

	/**
	 * Get all motor neurons associated with couplings on a specific neural network.
	 * 
	 * @param n the neural network
	 * @return a list of all motor neurons in that neural network which are contained in the coupling list
	 */
	public ArrayList getMotorCouplingNeurons(NetworkPanel n) {
		ArrayList ret = new ArrayList();
		
		for (int i = 0; i < n.getPNodeNeurons().size(); i++ ) {
			for (int j = 0; j < this.size(); j++ ) {
				if(getPNodeNeuron(j) == (PNodeNeuron)n.getPNodeNeurons().get(i)) {
					if (getCoupling(j) instanceof MotorCoupling) {
						ret.add(getPNodeNeuron(j));						
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Get all sensory neurons associated with couplings on a specific neural network.
	 * 
	 * @param n the neural network
	 * @return a list of alll sensory neurons in that neural network which are contained in the coupling list
	 */
	public ArrayList getSensoryCouplingNeurons(NetworkPanel n) {
		ArrayList ret = new ArrayList();
		
		for (int i = 0; i < n.getPNodeNeurons().size(); i++ ) {
			for (int j = 0; j < this.size(); j++ ) {
				if(getPNodeNeuron(j) == (PNodeNeuron)n.getPNodeNeurons().get(i)) {
					if (getCoupling(j) instanceof SensoryCoupling) {
						ret.add(getPNodeNeuron(j));						
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Remove all couplings associated with a given neural network
	 * 
	 * @param n the neural network whose couplings should be removed.
	 */
	public void removeCouplings(NetworkPanel n) {
		for (int i = 0; i < n.getPNodeNeurons().size(); i++ ) {
			PNodeNeuron pn = (PNodeNeuron)n.getPNodeNeurons().get(i);
			for (int j = 0; j < this.size(); j++ ) {
				if(getPNodeNeuron(j) == pn) {
					remove(getCoupling(j));
				}
					
			}
		}
	}
		
	/**
	 * Remove all given agents from the couplng list, by setting the agent field on those
	 * couplings to null.
	 * 
	 * @param w the world whose agents should be removed
	 */
	public void removeAgentsFromCouplings(World w) {
		ArrayList agents = w.getAgentList();
		removeAgentsFromCouplings(agents);
	}
	
	/**
	 * Remove all given agents from the couplng list, by setting the agent field on those
	 * couplings to null.
	 * 
	 * @param agents the list of agents to be removed.
	 */
	public void removeAgentsFromCouplings(ArrayList agents) {
		for (int i = 0; i < this.size(); i++ ) {
			for (int j = 0; j < agents.size(); j++ ) {
				if(getCoupling(i).getAgent() ==  agents.get(j)) {
					getCoupling(i).setAgent(null);
				}
					
			}
		}	
	}
	
	/**
	 * Get couplings which are connected to no agent (whose agent field is null)
	 * 
	 * @return the list of un-associated couplings
	 */
	public CouplingList getNullAgentCouplings() {
	
		CouplingList ret = new CouplingList();
		
		for(int i = 0; i < size(); i++) {
			if (getCoupling(i).getAgent() == null) {
				ret.add(getCoupling(i));
			}
		}
		
		return ret;
	}
	
	/**
	 * Print debug information about couplings to standard output
	 * 
	 */
	public void debug() {
		for (int i = 0; i < size(); i++) {
			System.out.println("------- Coupling [" + i + "] -------");
			getCoupling(i).debug();
		}
	}

}
