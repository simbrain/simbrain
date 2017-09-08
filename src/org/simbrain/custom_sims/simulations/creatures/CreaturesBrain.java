package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.NetworkComponent;

/**
 * A helper class of Creatures for filling in networks, from either a base
 * template or from genetic code. (Better here than cluttering up the main
 * class)
 * 
 * @author Sharai
 *
 */
public class CreaturesBrain extends NetBuilder {

	private List<NeuronGroup> lobes = new ArrayList();

	public CreaturesBrain(NetworkComponent brain) {
		super(brain);
	}

	/**
	 * Sets up a "brain" network from a template
	 * 
	 * @param brain
	 *            The network to set up the brain in
	 */
	public void setUp() {
		System.out.println("One empty thinkpan, coming up!");

		// Set up Lobe #1: Drive Lobe
		NeuronGroup driveLobe = addNeuronGroup(0, 0, 12, "grid", "LinearRule");
		driveLobe.setLabel("Drive Lobe");
		lobes.add(driveLobe);

		// Label drive lobe neurons
		driveLobe.getNeuronList().get(0).setLabel("Myself");

		// Set up Lobe #2: Stimulus Source Lobe
		NeuronGroup stimulusLobe = addNeuronGroup(150, 0, 10, "grid", "LinearRule");
		stimulusLobe.setLabel("Object Lobe");
		lobes.add(stimulusLobe);

		// Label stimulus source lobe neurons

		// Test
		SynapseGroup testSynapse = addSynapseGroup(driveLobe, stimulusLobe);
		testSynapse.setLabel("Test");

	}

}
