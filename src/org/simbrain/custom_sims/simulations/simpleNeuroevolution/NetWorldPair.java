package org.simbrain.custom_sims.simulations.simpleNeuroevolution;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

public class NetWorldPair {
	long netSeed;
	NetBuilder net;
	List<NeuronGroup> input = new ArrayList<>();
	List<Neuron>inputFlat = new ArrayList<>();
	List<Neuron> hidden = new ArrayList<>();
	NeuronGroup output;
	List<Neuron> sourceNeuron = new ArrayList<>();
	List<Neuron> targetNeuron = new ArrayList<>();
	OdorWorldBuilder world;
	RotatingEntity mouse;
	OdorWorldEntity cheese;
	OdorWorldEntity poison;
	NetworkAttribute attriubte;
	
	public NetBuilder getNet() {
		return net;
	}
	
	public OdorWorldBuilder getWorld() {
		return world;
	}
	
	
}
