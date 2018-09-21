package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.UDF;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistributions.LogNormalDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The goal here is to create a patch of neurons that respond in a semi-reliable
 * way to a few inputs, like cheese and fish, and combos of them. A primary goal
 * is to just make it visually compelling. When near cheese we get bubbling in
 * one spatial part of the network; when near fish we get bubbling in another.
 * When near both we see a kind of combo of the other bubblings.  Users should
 * be able to see this directly via visual inspection. Rate based probably best
 * given the aims of this (once tuned I plan to use it for a few different demos
 * and lessons). As long as the primary visual goal is achieved, all else is
 * gravy.  The more cortically realistic the better. The idea that as you stay
 * near an object it would start to eventually go to neighboring regions and
 * thereby reflect past associations is great, as is the idea of eventually
 * decaying to a random walk. If some kind of decay or other parameter
 * influences whether it perseverates in one region or decays to a default mode
 * that’s great too. It would be nice to play with the simulation in both
 * modes.
 */
public class PatternsOfActivity extends RegisteredSimulation {

    // References
    Network network;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        NetBuilder net = sim.addNetwork(10, 10, 543, 545,
            "Patterns of Activity");
        network = net.getNetwork();
        NeuronGroup sensoryNet = net.addNeuronGroup(-9.25, 95.93, 3);
        sensoryNet.setClamped(true);
        sensoryNet.setLabel("Sensory");
        Neuron cheeseNeuron = sensoryNet.getNeuronList().get(0);
        cheeseNeuron.setLabel("Cheese");
        Neuron flowerNeuron = sensoryNet.getNeuronList().get(1);
        flowerNeuron.setLabel("Flower");

        // Set up odor world
        OdorWorldBuilder world = sim.addOdorWorld(547, 5, 504, 548, "World");
        world.getWorld().setObjectsBlockMovement(false);
        world.getWorld().setTileMap(new TileMap("empty.tmx"));
        OdorWorldEntity mouse = world.addEntity(120, 245, OdorWorldEntity.EntityType.MOUSE);
        mouse.addSensor(new SmellSensor(mouse, "Smell", 0,0));

        mouse.setHeading(90);
        OdorWorldEntity cheese = world.addEntity(120, 180, OdorWorldEntity.EntityType.SWISS, new double[] {1, .5, .2});
        cheese.getSmellSource().setDispersion(65);
        OdorWorldEntity flower = world.addEntity(320, 180, OdorWorldEntity.EntityType.FLOWER, new double[] {.2, 1, .3});
        flower.getSmellSource().setDispersion(65);

        // Make couplings
        sim.couple((SmellSensor) mouse.getSensor("Smell"),sensoryNet);

    }

    public PatternsOfActivity(SimbrainDesktop desktop) {
        super(desktop);
    }

    public PatternsOfActivity() {
        super();
    }

    @Override
    public String getName() {
        return "Patterns of activity";
    }

    @Override
    public PatternsOfActivity instantiate(SimbrainDesktop desktop) {
        return new PatternsOfActivity(desktop);
    }

}