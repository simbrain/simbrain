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

import org.simbrain.network.core.RootNetwork;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.listeners.NetworkEvent;

/**
 * Object which routes synapses to synapse groups, based on what neuron groups the synapses
 * are connected to.
 * 
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class SynapseRouter {
	
	/** A routing map where group pairs are mapped to connecting synapse groups. */
	private HashMap<NeuronGroupPair, SynapseGroup> routingMap =
			new HashMap<NeuronGroupPair, SynapseGroup>();

	/** A mapping of neuron groups to unified incoming synapse groups. */
	private HashMap<NeuronGroup, SynapseGroup> incomingUnified =
			new HashMap<NeuronGroup, SynapseGroup>();

	/** A mapping of neuron groups to unified outgoing synapse groups. */
	private HashMap<NeuronGroup, SynapseGroup> outgoingUnified =
			new HashMap<NeuronGroup, SynapseGroup>();
	
	/** Set to true for auto-routing (not fully tested). */ 
	private boolean autoRouting = false;

	/**
	 * Associates a pair of neuron groups and their connecting synapse group.
	 * 
	 * @param sourceNeuronGroup the source neuron group 
	 * @param targetNeuronGroup the target neron group 
	 * @param con the connecting synapse group 
	 */
	public void associateSynapseGroupWithNeuronGroupPair(NeuronGroup sourceNeuronGroup, NeuronGroup targetNeuronGroup,
			SynapseGroup con) {	
		// Any values can be null. For instance if one wanted to register a
		// neuron group pair with no connecting synapse group (perhaps awaiting a
		// connecting group), the synapse group would simply be passed in as
		// null. Likewise, if sourceNeuronGroup or targetNeuronGroup are null it would indicate that the source
		// or target neurons were loose neurons respectively.	 
		NeuronGroupPair gp = new NeuronGroupPair(sourceNeuronGroup, targetNeuronGroup);
		routingMap.put(gp, con);
	}
	
	/**
	 * Any synapse originating in this neuron group is routed to the specified synapse group.
	 * 
	 * @param sourceNeuronGroup the source neuron group
	 * @param con the synapse group to route to
	 */
	public void associateSynapseGroupWithSourceNeuronGroup(NeuronGroup sourceNeuronGroup,
			SynapseGroup con) {
		associateSynapseGroupWithNeuronGroupPair(sourceNeuronGroup, null, con);
	}

	/**
	 * Any synapse terminating in this neuron group is routed to the specified synapse group.
	 * 
	 * @param sourceNeuronGroup the target neuron group
	 * @param con the synapse group to route to
	 */
	public void associateSynapseGroupWithTargetNeuronGroup(NeuronGroup targetNeuronGroup,
			SynapseGroup con) {
		associateSynapseGroupWithNeuronGroupPair(null, targetNeuronGroup, con);
	}

	/**
	 * Unifies all the incoming SynapseGroups to a target NeuronGroup. That is,
	 * if a synapse is added which has targetNeuronGroup as a target group, then
	 * regardless of its source it will be added to the same SynapseGroup.
	 * This function is quite expensive, which is why it is expected to be used
	 * infrequently.
	 *
	 * @param targetNeuronGroup the target layer for which all incoming synapses are
	 * being unified into the same group.
	 */
	public void unifyIncomingSynapsesToNeuronGroup(NeuronGroup targetNeuronGroup) {
		SynapseGroup sg = new SynapseGroup(targetNeuronGroup.getParentNetwork());
		incomingUnified.put(targetNeuronGroup, sg);
		HashSet<Subnetwork> parentNetworks = new HashSet<Subnetwork>();
		if(targetNeuronGroup.getParentGroup() instanceof Subnetwork) {
			parentNetworks.add((Subnetwork)targetNeuronGroup.getParentGroup());
			((Subnetwork)targetNeuronGroup.getParentGroup()).addSynapseGroup(sg);
		}	
		for(NeuronGroupPair ngp : routingMap.keySet()) {		
			if(ngp.getSourceGroup() == targetNeuronGroup) {
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
	 * That is, if a synapse is added which has @param sourceNeuronGroup as a source group,
	 * then regardless of its target it will be added to the same SynapseGroup.
	 * This function is quite expensive, which is why it is expected to be used
	 * infrequently.
	 *
	 * @param sourceNeuronGroup the target layer for which all incoming synapses are
	 * being unified into the same group.
	 */
	public void unifyOutgoingSynapsesToNeuronGroup(NeuronGroup sourceNeuronGroup) {
		//Create a single group which will subsume all synapses outgoing from
		//sourceNeuronGroup
		SynapseGroup sg = new SynapseGroup(sourceNeuronGroup.getParentNetwork());
		
		//Bind this source group to the union synapse group
		outgoingUnified.put(sourceNeuronGroup, sg);
		
		//If the sgs to be unified cross between subnets keep track of each
		//subnet that sg will need to be added to
		HashSet<Subnetwork> parentNetworks = new HashSet<Subnetwork>();
		if(sourceNeuronGroup.getParentGroup() instanceof Subnetwork) {
			parentNetworks.add((Subnetwork)sourceNeuronGroup.getParentGroup());
			((Subnetwork)sourceNeuronGroup.getParentGroup()).addSynapseGroup(sg);
		}	
		
		//Iterate through ngp keys in the routing map...
		for(NeuronGroupPair ngp : routingMap.keySet()) {		
			if(ngp.getSourceGroup() == sourceNeuronGroup) {
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
	 * Takes a target neuron group with a unified incoming synapse group and disjoins
	 * that group such that each connected source-target pair is connected by
	 * a different synapse group. 
	 *
	 * @param targetNeuronGroup the target 
	 */
	public void disjoinIncomingSynapses(NeuronGroup targetNeuronGroup) {
		if(!incomingUnified.containsKey(targetNeuronGroup)) {
			return;
		}
		SynapseGroup unifiedGroup = incomingUnified.remove(targetNeuronGroup);
		for(Synapse syn : unifiedGroup.getSynapseList()) {
			routeSynapse(syn);
		}
		unifiedGroup.getParentNetwork().fireGroupRemoved(unifiedGroup);
		unifiedGroup.getSynapseList().clear();
		unifiedGroup.delete();
	}

	/**
	 * Takes a source layer with a unified outgoing synapse group and disjoins
	 * that group such that each connected source-target pair is connected by
	 * a different synapse group.
	 *
	 * @param sourceNeuronGroup
	 */
	public void disjoinOutgoingSynapses(NeuronGroup sourceNeuronGroup) {
		if(!outgoingUnified.containsKey(sourceNeuronGroup)) {
			return;
		}
		SynapseGroup unifiedGroup = outgoingUnified.remove(sourceNeuronGroup);
		for(Synapse syn : unifiedGroup.getSynapseList()) {
			routeSynapse(syn);
		}
		unifiedGroup.getParentNetwork().fireGroupRemoved(unifiedGroup);
		unifiedGroup.getSynapseList().clear();
		unifiedGroup.delete();
	}

	/**
	 * Routes a synapse to the correct synapse groups.
	 *
	 * @param syn the synapse to be routed to the correct synapse group.
	 */
	public void routeSynapse(Synapse syn) {
		NeuronGroup sourceNeuronGroup = (NeuronGroup) syn.getSource().getParentGroup();
		NeuronGroup targetNeuronGroup = (NeuronGroup) syn.getTarget().getParentGroup();
		
		// If the synapse is not affiliated with any neuron group, no routing needed
		if ((sourceNeuronGroup == null) && (targetNeuronGroup == null)) {
			return;
		}
		
		NeuronGroupPair ngp = new NeuronGroupPair(sourceNeuronGroup, targetNeuronGroup);
		SynapseGroup sg = null;

		// Take care of unified cases
		if(!(incomingUnified.isEmpty() || outgoingUnified.isEmpty())) {
			if(incomingUnified.containsKey(targetNeuronGroup)) {
				sg = incomingUnified.get(targetNeuronGroup);
				addSynapseToGroup(syn, sg);
				if(!((Subnetwork)sourceNeuronGroup.getParentGroup()).
						getSynapseGroupList().contains(sg) &&
						sourceNeuronGroup.getParentGroup() != null) {					
					((Subnetwork)sourceNeuronGroup.getParentGroup()).addSynapseGroup(sg);				
				}
				routingMap.put(ngp, sg);
			} else if (outgoingUnified.containsKey(sourceNeuronGroup)) {
				sg = outgoingUnified.get(sourceNeuronGroup);
				addSynapseToGroup(syn, sg);
				if(!((Subnetwork)targetNeuronGroup.getParentGroup()).
						getSynapseGroupList().contains(sg) &&
						targetNeuronGroup.getParentGroup() != null) {					
					((Subnetwork)targetNeuronGroup.getParentGroup()).addSynapseGroup(sg);
				}
				routingMap.put(ngp, sg);
			}
		} else {
			
			//If the neuron groups are associated with a synapse group 
			// route the synapse to the associated synapse group
			if(routingMap.get(ngp) != null) {
				//get the associated synapse group and add the synapse to it
				sg = routingMap.get(ngp); 
				addSynapseToGroup(syn, sg);
				
			} else {
				if (autoRouting == true) {

					// Is this synapse between subnetworks?
					boolean interGroupSynapse = sourceNeuronGroup
							.getParentGroup() != targetNeuronGroup
							.getParentGroup();

					// Create the new synapse group (since its been shown that
					// syn belongs to no bound SG)
					sg = new SynapseGroup(syn.getRootNetwork());
					syn.getRootNetwork().fireGroupAdded(sg);

					// If the parent subnet of sourceNeuronGroup != null add our
					// new synapse group to that subnet
					if (sourceNeuronGroup.getParentGroup() != null) {
						((Subnetwork) sourceNeuronGroup.getParentGroup())
								.addSynapseGroup(sg);

					}
					// If the target group is in a different subnet and not null
					// add our new synapse group to that subnet as well.
					if (targetNeuronGroup.getParentGroup() != null
							&& interGroupSynapse) {
						((Subnetwork) targetNeuronGroup.getParentGroup())
								.addSynapseGroup(sg);
					}

					// Add our synapse to it
					sg.addSynapse(syn);

					// Put the group in the routing map with the associated
					// synapse
					// group
					routingMap.put(ngp, sg);
				}
				
			}
		}

	}
		
	/**
	 * Helper method for placing a synapse in a synapse group, and firing the appropriate
	 * notification event.
	 *
	 * @param synapse the synapse
	 * @param synapseGroup the group to place the synapse in
	 */
	private void addSynapseToGroup(final Synapse synapse, final SynapseGroup synapseGroup) {
		
		// If the synapse already exists in that group, don't add it.
		boolean wasAdded = synapseGroup.addSynapse(synapse);
		if (wasAdded) {
			// Fire Event so network panel knows to add this synapse to appropriate
			// PNode
			synapseGroup.getParentNetwork().transferSynapseToGroup(synapse, synapseGroup);
			NetworkEvent<Group> event = new NetworkEvent<Group>(
					synapseGroup.getParentNetwork(), synapseGroup, synapseGroup);
			event.setAuxiliaryObject(synapse);
			synapseGroup.getParentNetwork().fireGroupChanged(event,"synapseAddedToGroup");   								
		}

	}
	
	/**
	 * Representation of a pair of neuron groups, which serve as a key for the routing map,
	 * which associates pairs of neuron groups with synapse groups. 
	 */
	private class NeuronGroupPair {
		
		/** A seed for generating hash codes. */
		public static final int HASH_SEED = 23;
		
		/** 
		 * An odd prime number for generating hash codes based on the seed
		 * and hash codes of the internal fields. 
		 */
		public static final int ODD_PRIME = 47;
		
		/** The source in the pair. */
		private final NeuronGroup sourceNeuronGroup;
		
		/** The target in the pair. */
		private final NeuronGroup targetNeuronGroup;
		
		/**
		 * A constructor specifying the source and target pair
		 * @param sourceNeuronGroup the source group
		 * @param targetNeuronGroup the target group
		 */
		public NeuronGroupPair(NeuronGroup sourceNeuronGroup, NeuronGroup targetNeuronGroup) {
			this.sourceNeuronGroup = sourceNeuronGroup;
			this.targetNeuronGroup = targetNeuronGroup;
		}
		
		/**
		 * Overrides the equals method such that any neuron group pair with
		 * the same neuron groups is considered equal.
		 */
		public boolean equals(Object obj) {
			if(!(obj instanceof NeuronGroupPair)){
				return false;
			} else {
				if(((NeuronGroupPair) obj).getSourceGroup() == sourceNeuronGroup
						&& ((NeuronGroupPair) obj).getTargetGroup() == targetNeuronGroup) {
					return true;
				} else {
					return false;
				}
			}
		}
		
		/**
		 * If .equals(obj) is overridden then hashcode() must also be 
		 * overridden. This method provides a unique hashcode based on the
		 * source and target neuron group (as well as which one is sourceNeuronGroup vs.
		 * targetNeuronGroup).
		 */
		public int hashCode(){
			int result = HASH_SEED;
			if(sourceNeuronGroup == null) {
				result = (result * ODD_PRIME) + 0;
			} else {
				result = (result * ODD_PRIME) + sourceNeuronGroup.hashCode();
			} if (targetNeuronGroup == null) {
				result = (result * ODD_PRIME) + 0;
			} else {
				result = (result * ODD_PRIME) + targetNeuronGroup.hashCode();
			}
			return result;
		}
		
		public NeuronGroup getSourceGroup() {
			return sourceNeuronGroup;
		}
		
		public NeuronGroup getTargetGroup() {
			return targetNeuronGroup;
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
