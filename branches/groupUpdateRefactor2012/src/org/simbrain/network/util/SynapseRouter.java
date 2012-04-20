/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.listeners.NetworkEvent;

/**
 * TODO.
 * 
 * @author Zach Tosi (first author!)
 * @author Jeff Yoshimi
 *
 */
public class SynapseRouter {
	
	/** A mapping of neuron groups to unified incoming synapse groups. */
	private HashMap<NeuronGroup, SynapseGroup> incomingUnified =
			new HashMap<NeuronGroup, SynapseGroup>();

	/** A mapping of neuron groups to unified outgoing synapse groups. */
	private HashMap<NeuronGroup, SynapseGroup> outgoingUnified =
			new HashMap<NeuronGroup, SynapseGroup>();
	
	/** A routing map where group pairs are mapped to connecting synapse groups. */
	private HashMap<NeuronGroupPair, SynapseGroup> routingMap =
			new HashMap<NeuronGroupPair, SynapseGroup>();
	
	//TODO: Revise / fill in javadocs and code comments	
	// TODO: Revise "null" policy to be a true wildcard (unafilliated or arbitrary neuron group)
	// TODO: Add methods for associated synapsegroups specifically with loose or
	// unaffiliated source or target neurons
	
	/**
	 * Associates a pair of neuron groups and their connecting synapse group.
	 * 
	 * @param src the source neuron group 
	 * @param targ the target neron group 
	 * @param con the connecting synapse group 
	 */
	public void associateSynapseGroupWithNeuronGroupPair(NeuronGroup src, NeuronGroup targ,
			SynapseGroup con) {	
		// Any values can be null. For instance if one wanted to register a
		// neuron group pair with no connecting synapse group (perhaps awaiting a
		// connecting group), the synapse group would simply be passed in as
		// null. Likewise, if src or targ are null it would indicate that the source
		// or target neurons were loose neurons respectively.	 
		NeuronGroupPair gp = new NeuronGroupPair(src, targ);
		routingMap.put(gp, con);
	}
	
	/**
	 * Unifies all the incoming SynapseGroups to a target NeuronGroup. That is,
	 * if a synapse is added which has @param targ as a target group, then
	 * regardless of its source it will be added to the same SynapseGroup.
	 * This function is quite expensive, which is why it is expected to be used
	 * infrequently.
	 * @param targ the target layer for which all incoming synapses are
	 * being unified into the same group.
	 */
	public void unifyIncomingSynapsesToNeuronGroup(NeuronGroup targ) {
		SynapseGroup sg = new SynapseGroup(targ.getParentNetwork());
		incomingUnified.put(targ, sg);
		HashSet<Subnetwork> parentNetworks = new HashSet<Subnetwork>();
		if(targ.getParentGroup() instanceof Subnetwork) {
			parentNetworks.add((Subnetwork)targ.getParentGroup());
			((Subnetwork)targ.getParentGroup()).addSynapseGroup(sg);
		}	
		for(NeuronGroupPair ngp : routingMap.keySet()) {		
			if(ngp.getSourceGroup() == targ) {
				SynapseGroup current = routingMap.get(ngp);	
				if(current != null) {
					if(ngp.getSourceGroup().getParentGroup() instanceof
							Subnetwork){
						Subnetwork sourceNet = (Subnetwork)ngp.getSourceGroup()
								.getParentGroup();
						if (!parentNetworks.contains(sourceNet)) {
							parentNetworks.add(sourceNet);
							sourceNet.addSynapseGroup(sg);
							sourceNet.removeSynapseGroup(current);
						}	
					}
					sg.getSynapseList().addAll(current.getSynapseList());
					
					current.getSynapseList().clear();
					current.getParentNetwork().fireGroupRemoved(current);
					current.delete();
				}
				
				
			}
		}
		sg.getParentNetwork().fireGroupAdded(sg);
	}
	
	/**
	 * Unifies all the outgoing SynapseGroups from a source NeuronGroup.
	 * That is, if a synapse is added which has @param src as a source group,
	 * then regardless of its target it will be added to the same SynapseGroup.
	 * This function is quite expensive, which is why it is expected to be used
	 * infrequently.
	 * @param src the target layer for which all incoming synapses are
	 * being unified into the same group.
	 */
	public void unifyOutgoingSynapsesToNeuronGroup(NeuronGroup src) {
		//Create a single group which will subsume all synapses outgoing from
		//src
		SynapseGroup sg = new SynapseGroup(src.getParentNetwork());
		
		//Bind this source group to the union synapse group
		outgoingUnified.put(src, sg);
		
		//If the sgs to be unified cross between subnets keep track of each
		//subnet that sg will need to be added to
		HashSet<Subnetwork> parentNetworks = new HashSet<Subnetwork>();
		if(src.getParentGroup() instanceof Subnetwork) {
			parentNetworks.add((Subnetwork)src.getParentGroup());
			((Subnetwork)src.getParentGroup()).addSynapseGroup(sg);
		}	
		
		//Iterate through ngp keys in the routing map...
		for(NeuronGroupPair ngp : routingMap.keySet()) {		
			if(ngp.getSourceGroup() == src) {
				SynapseGroup current = routingMap.get(ngp);	
				if(current != null) {
					if(ngp.getTargetGroup().getParentGroup() instanceof
							Subnetwork){
						Subnetwork targNet = (Subnetwork)ngp.getTargetGroup()
								.getParentGroup();
						if (!parentNetworks.contains(targNet)) {
							parentNetworks.add(targNet);
							targNet.addSynapseGroup(sg);
							targNet.removeSynapseGroup(current);
						}	
					}
					sg.getSynapseList().addAll(current.getSynapseList());
					
					current.getSynapseList().clear();
					current.getParentNetwork().fireGroupRemoved(current);
					current.delete();
				}
				
				
			}
		}
		sg.getParentNetwork().fireGroupAdded(sg);
	}
	
	/**
	 * Takes a target layer with a unified incoming synapse group and disjoins
	 * that group such that each connected source-target pair is connected by
	 * a different synapse group. 
	 * @param targ the target 
	 */
	public void disjoinIncomingSynapses(NeuronGroup targ) {
		if(!incomingUnified.containsKey(targ)) {
			return;
		}
		SynapseGroup unifiedGroup = incomingUnified.remove(targ);
		for(Synapse syn : unifiedGroup.getSynapseList()) {
			routeSynapse(syn);
		}
		unifiedGroup.getParentNetwork().fireGroupRemoved(unifiedGroup);
		unifiedGroup.getSynapseList().clear();
		unifiedGroup.delete();
	}

	/**
	 * Takes a tsource layer with a unified outgoing synapse group and disjoins
	 * that group such that each connected source-target pair is connected by
	 * a different synapse group. 
	 * @param src
	 */
	public void disjoinOutgoingSynapses(NeuronGroup src) {
		if(!outgoingUnified.containsKey(src)) {
			return;
		}
		SynapseGroup unifiedGroup = outgoingUnified.remove(src);
		for(Synapse syn : unifiedGroup.getSynapseList()) {
			routeSynapse(syn);
		}
		unifiedGroup.getParentNetwork().fireGroupRemoved(unifiedGroup);
		unifiedGroup.getSynapseList().clear();
		unifiedGroup.delete();
	}
	
	/**
	 * Any synapse with this neuron group as source-parent is routed to the
	 * specified synapse group.
	 * 
	 * @param src the source neuron group
	 * @param con the synapse group to route to
	 */
	public void associateSynapseGroupWithSourceNeuronGroup(NeuronGroup src,
			SynapseGroup con) {
		associateSynapseGroupWithNeuronGroupPair(src, null, con);
	}

	/**
	 * Any synapse with this neuron group as target-parent is routed to the
	 * specified synapse group.
	 * 
	 * @param src the target neuron group
	 * @param con the synapse group to route to
	 */
	public void associateSynapseGroupWithTargetNeuronGroup(NeuronGroup targ,
			SynapseGroup con) {
		associateSynapseGroupWithNeuronGroupPair(null, targ, con);
	}

	/**
	 * Routes a synapse into the correct synapse group. If the neuron group
	 * pair does not exist or if they do but map to a null synapse group
	 * a neuron group pair comprised of the source and target groups
	 * is mapped to a new synapse group and syn is added to that group.
	 *
	 * @param syn the synapse to be routed to the correct synapse group.
	 */
	public void routeSynapse(Synapse syn) {
		NeuronGroup src = (NeuronGroup) syn.getSource().getParentGroup();
		NeuronGroup targ = (NeuronGroup) syn.getTarget().getParentGroup();
		
		// If the synapse is not affiliated with any neuron group, no routing
		// need be done.
		if ((src == null) && (targ == null)) {
			return;
		}
		NeuronGroupPair ngp = new NeuronGroupPair(src, targ);
		SynapseGroup sg = null;
		
		if(!(incomingUnified.isEmpty() || outgoingUnified.isEmpty())) {
			if(incomingUnified.containsKey(targ)) {
				sg = incomingUnified.get(targ);
				sg.addSynapse(syn);
				if(!((Subnetwork)src.getParentGroup()).
						getSynapseGroupList().contains(sg) &&
						src.getParentGroup() != null) {					
					((Subnetwork)src.getParentGroup()).addSynapseGroup(sg);				
				}
				routingMap.put(ngp, sg);
			} else if (outgoingUnified.containsKey(src)) {
				sg = outgoingUnified.get(src);
				sg.addSynapse(syn);
				if(!((Subnetwork)targ.getParentGroup()).
						getSynapseGroupList().contains(sg) &&
						targ.getParentGroup() != null) {					
					((Subnetwork)targ.getParentGroup()).addSynapseGroup(sg);
				}
				routingMap.put(ngp, sg);
			}
		} else {
			
			//If the neuron groups are associated with a synapse group (implies
			//that it exists as a key...)
			if(routingMap.get(ngp) != null) {
				//get the associated synapse group and add the synapse to it
				sg = routingMap.get(ngp); 
				sg.addSynapse(syn);
				
			} else {
	
				// TODO: Note to zach.  Including this caused some problems, and
				// besides I took it from our conversation that we would just assume
				// all synpase groups would be initially added.  But I'm leaving this here
				// for reference
				
				//Is this synapse between subnetworks?
				boolean interGroupSynapse = src.getParentGroup() !=
						targ.getParentGroup();
	
				//Create the new synapse group (since its been shown that syn
				//belongs to no bound SG)
				sg = new SynapseGroup(syn.getRootNetwork());
				syn.getRootNetwork().fireGroupAdded(sg);
							
				//If the parent subnet of src != null add our new synapse group
				//to that subnet
				if(src.getParentGroup() != null) {
					((Subnetwork)src.getParentGroup()).addSynapseGroup(sg);
					
				}
				//If the target group is in a different subnet and not null
				//add our new synapse group to that subnet as well.
				if(targ.getParentGroup() != null && interGroupSynapse) {		
					((Subnetwork)targ.getParentGroup()).addSynapseGroup(sg);
				}
			
				
				//Add our synapse to it
				sg.addSynapse(syn);
				
				//Put the group in the routing map with the associated synapse
				//group
				routingMap.put(ngp, sg);
				
			}
		}
			// Fire Event so network panel knows to add this synapse to appropriate
			// PNode
			sg.getParentNetwork().transferSynapseToGroup(syn, sg);
			NetworkEvent<Group> event = new NetworkEvent<Group>(
			sg.getParentNetwork(), sg, sg);
			event.setAuxiliaryObject(syn);
			sg.getParentNetwork().fireGroupChanged(event,"synapseAddedToGroup");   		
	}
	
	
	//TODO: Doc//  mention web4j.
	class NeuronGroupPair {
		
		/** A seed for generating hash codes. */
		public static final int HASH_SEED = 23;
		
		/** 
		 * An odd prime number for generating hash codes based on the seed
		 * and hash codes of the internal fields. 
		 */
		public static final int ODD_PRIME = 47;
		
		/** The source in the pair. */
		private final NeuronGroup src;
		
		/** The target in the pair. */
		private final NeuronGroup targ;
		
		/**
		 * A constructor specifying the source and target pair
		 * @param src the source group
		 * @param targ the target group
		 */
		public NeuronGroupPair(NeuronGroup src, NeuronGroup targ) {
			this.src = src;
			this.targ = targ;
		}
		
		/**
		 * Overrides the equals method such that any neuron group pair with
		 * the same neuron groups is considered equal.
		 */
		public boolean equals(Object obj) {
			if(!(obj instanceof NeuronGroupPair)){
				return false;
			} else {
				if(((NeuronGroupPair) obj).getSourceGroup() == src
						&& ((NeuronGroupPair) obj).getTargetGroup() == targ) {
					return true;
				} else {
					return false;
				}
			}
		}
		
		/**
		 * If .equals(obj) is overridden then hashcode() must also be 
		 * overridden. This method provides a unique hashcode based on the
		 * source and target neuron group (as well as which one is src vs.
		 * targ).
		 */
		public int hashCode(){
			int result = HASH_SEED;
			if(src == null) {
				result = (result * ODD_PRIME) + 0;
			} else {
				result = (result * ODD_PRIME) + src.hashCode();
			} if (targ == null) {
				result = (result * ODD_PRIME) + 0;
			} else {
				result = (result * ODD_PRIME) + targ.hashCode();
			}
			return result;
		}
		
		public NeuronGroup getSourceGroup() {
			return src;
		}
		
		public NeuronGroup getTargetGroup() {
			return targ;
		}
		
	}
	
	/**
	 * Test method
	 *
	 * @param args
	 */
	public static void main(String args[]) {
		RootNetwork rn = new RootNetwork();
		NeuronGroup ng1 = new NeuronGroup(rn);
		NeuronGroup ng2 = new NeuronGroup(rn);
		SynapseRouter srm = new SynapseRouter();
		srm.associateSynapseGroupWithNeuronGroupPair(ng1, ng2, null);

		
//		boolean test1 = srm.containsPairing(ng1, ng2);
//		
//		boolean test2 = srm.containsPairing(ng2, ng1);
		
		boolean test3 = srm.routingMap.containsKey(srm.new NeuronGroupPair(ng1, ng2));
		
		boolean test4 = srm.routingMap.containsKey(srm.new NeuronGroupPair(ng2, ng1));
		
//		System.out.println("Test1 (should be true): " + test1);
//		System.out.println("Test2 (should be false): " + test2);
		System.out.println("Test3 (should be true): " + test3);
		System.out.println("Test4 (should be false): " + test4);
	}
	
	
}
