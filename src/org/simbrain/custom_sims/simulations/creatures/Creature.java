package org.simbrain.custom_sims.simulations.creatures;

import java.util.List;

import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.workspace.Coupling;
import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.Hearing;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * Each CreatureInstance represents one particular creature in the simulation,
 * and wraps around various other classes and has helper methods for dealing
 * with particular creatures.
 *
 * @author Sharai
 *
 */
public class Creature {

	/**
	 * The name of the creature.
	 */
	private String name;

	/** Back reference to parent simulation. */
	private final CreaturesSim parentSim;

	/**
	 * The brain belonging to this creature.
	 */
	private CreaturesBrain brain;

	/**
	 * The odor world agent belonging to this creature.
	 */
	private RotatingEntity agent;

	/** Reference to drive lobe. */
	private NeuronGroup drives;

	/** Reference to stimulus lobe. */
	private NeuronGroup stimulus;

	/** Reference to verb lobe. */
	private NeuronGroup verbs;

	/** Reference to noun lobe. */
	private NeuronGroup nouns;

	/** Reference to sensory lobe. */
	private NeuronGroup senses;

	/** Reference to perception lobe. */
	private NeuronGroup perception;

	/** Reference to decisions lobe. */
	private NeuronGroup decisions;
	// private WinnerTakeAll decisions;

	/** Reference to attention lobe. */
	private NeuronGroup attention;

	/** How quickly to approach or avoid objects. */
	float baseMovementStepSize = 0.001f;

	public Creature(CreaturesSim sim, String name, NetBuilder net, RotatingEntity agent) {
		this.parentSim = sim;
		this.name = name;

		this.agent = agent;
		initAgent();

		this.brain = new CreaturesBrain(net, sim);
		initDefaultBrain();

		initCouplings();
	}

	/**
	 * Update the various components of the creature.
	 */
	public void update() {
		brain.getNetwork().update();

		double approachActivation = decisions.getNeuronByLabel("Approach").getActivation();
		if (approachActivation > 0) {
			approachBehavior(approachActivation);
		}
	}

	/**
	 * Configure the agent's name, smells, sensors, and effectors.
	 */
	private void initAgent() {
		agent.setName(name);
		agent.setId("Mouse");
		agent.setSmellSource(new SmellSource(new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0 }));

		// Add hearing sensors
		agent.addSensor(new Hearing(agent, "Toy", 10));
		agent.addSensor(new Hearing(agent, "Fish", 10));
		agent.addSensor(new Hearing(agent, "Cheese", 10));
		agent.addSensor(new Hearing(agent, "Poison", 10));
		agent.addSensor(new Hearing(agent, "Hazard", 10));
		agent.addSensor(new Hearing(agent, "Flower", 10));
		agent.addSensor(new Hearing(agent, "Mouse", 10));
		agent.addSensor(new Hearing(agent, "Wait", 10));
		agent.addSensor(new Hearing(agent, "Left", 10));
		agent.addSensor(new Hearing(agent, "Right", 10));
		agent.addSensor(new Hearing(agent, "Forward", 10));
		agent.addSensor(new Hearing(agent, "Backward", 10));
		agent.addSensor(new Hearing(agent, "Sleep", 10));
		agent.addSensor(new Hearing(agent, "Approach", 10));
		agent.addSensor(new Hearing(agent, "Ingest", 10));
		agent.addSensor(new Hearing(agent, "Look", 10));
		agent.addSensor(new Hearing(agent, "Smell", 10));
		agent.addSensor(new Hearing(agent, "Attack", 10));
		agent.addSensor(new Hearing(agent, "Play", 10));
		agent.addSensor(new Hearing(agent, "Mate", 10));
	}

	/**
	 * Build a brain network from a template.
	 */
	private void initDefaultBrain() {

		// Init non-mutable lobes #1-5
		drives = brain.buildDriveLobe();
		stimulus = brain.buildStimulusLobe();
		verbs = brain.buildVerbLobe();
		nouns = brain.buildNounLobe();
		senses = brain.buildSensesLobe();

		// Init Lobe #6: Decisions
		// TODO: Make this a WTA lobe.
		decisions = brain.createLobe(538.67, 922.84, verbs.size(), "vertical line", "Lobe #6: Decisions");
		// decisions = brain.createWTALobe(538.67, 922.84, verbs.size(), "vertical line", "Lobe #6: Decisions");
		brain.copyLabels(verbs, decisions);
		
		//TODO: TEST CASE -- Remove when done
		decisions.getNeuronByLabel("Approach").setClamped(true);
		decisions.getNeuronByLabel("Approach").forceSetActivation(10.0);

		// Coupling some decision lobe cells to agent effectors
		parentSim.getSim().couple(decisions.getNeuronByLabel("Left"), agent.getEffector("Go-left"));
		parentSim.getSim().couple(decisions.getNeuronByLabel("Right"), agent.getEffector("Go-right"));
		parentSim.getSim().couple(decisions.getNeuronByLabel("Forward"), agent.getEffector("Go-straight"));
		// TODO: Is there a way to couple the "Backward" node to the Go-straight
		// effector in such a way that the inverse of the Backward node's activation is
		// what the Go-straight effector gets?

		// Init Lobe #7: Attention
		// TODO: Make this a WTA lobe.
		attention = brain.createLobe(440.55, 872.98, stimulus.size(), "vertical line", "Lobe #7: Attention");
		brain.copyLabels(stimulus, attention);
		
		// TODO: TEST CASE -- Remove when done
		Neuron testCase = attention.getNeuronByLabel("Cheese");
		testCase.setClamped(true);
		testCase.forceSetActivation(10.0);

		// Init Attention System Dendrite Pathways
		/*
		 * TODO: Connecting neurons w/ OneToOne is not hooking up the right neurons
		 * together. Doing this manually for now. Would prefer to handle this in a
		 * synapse group in the future.
		 */
		// STIMULUS-TO-ATTENTION
		brain.getBuilder().connect(stimulus.getNeuronByLabel("Toy"), attention.getNeuronByLabel("Toy"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(stimulus.getNeuronByLabel("Fish"), attention.getNeuronByLabel("Fish"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(stimulus.getNeuronByLabel("Cheese"), attention.getNeuronByLabel("Cheese"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(stimulus.getNeuronByLabel("Poison"), attention.getNeuronByLabel("Poison"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(stimulus.getNeuronByLabel("Hazard"), attention.getNeuronByLabel("Hazard"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(stimulus.getNeuronByLabel("Flower"), attention.getNeuronByLabel("Flower"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(stimulus.getNeuronByLabel("Mouse"), attention.getNeuronByLabel("Mouse"),
				new CreaturesSynapseRule(), 1);
		// NOUNS-TO-ATTENTION
		brain.getBuilder().connect(nouns.getNeuronByLabel("Toy"), attention.getNeuronByLabel("Toy"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(nouns.getNeuronByLabel("Fish"), attention.getNeuronByLabel("Fish"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(nouns.getNeuronByLabel("Cheese"), attention.getNeuronByLabel("Cheese"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(nouns.getNeuronByLabel("Poison"), attention.getNeuronByLabel("Poison"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(nouns.getNeuronByLabel("Hazard"), attention.getNeuronByLabel("Hazard"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(nouns.getNeuronByLabel("Flower"), attention.getNeuronByLabel("Flower"),
				new CreaturesSynapseRule(), 1);
		brain.getBuilder().connect(nouns.getNeuronByLabel("Mouse"), attention.getNeuronByLabel("Mouse"),
				new CreaturesSynapseRule(), 1);

		// Init Lobe #8: Concepts
		NeuronGroup concepts = brain.createLobe(1086.94, -21.61, 640, "grid", "Lobe #8: Concepts");
		brain.setLobeColumns(concepts, 20);

		// Init Concepts to Decisions Dendrite Pathways
		brain.createSynapseGroup(concepts, decisions, "#8 to #6, Type 0");
		// TODO: BUG: This second synapse group sometimes has a graphical glitch
		// (Arrow points to space).
		brain.createSynapseGroup(concepts, decisions, "#8 to #6, Type 1");

		// Init Lobe #0: Perception
		perception = brain.buildPerceptionLobe(new NeuronGroup[] { drives, verbs, senses, attention });

		// Init Perception to Concept Dendrite Pathway
		brain.createSynapseGroup(perception, concepts, "#0 to #8, Type 0");

	}

	/**
	 * Creates all essential couplings for the agent between various agent
	 * components.
	 */
	public void initCouplings() {

		// Couplings from center smell sensor to stimulus lobe
		parentSim.getSim().couple((SmellSensor) agent.getSensor("Smell-Center"), 0, stimulus.getNeuronByLabel("Toy"));
		parentSim.getSim().couple((SmellSensor) agent.getSensor("Smell-Center"), 1, stimulus.getNeuronByLabel("Fish"));
		parentSim.getSim().couple((SmellSensor) agent.getSensor("Smell-Center"), 2,
				stimulus.getNeuronByLabel("Cheese"));
		parentSim.getSim().couple((SmellSensor) agent.getSensor("Smell-Center"), 3,
				stimulus.getNeuronByLabel("Poison"));
		parentSim.getSim().couple((SmellSensor) agent.getSensor("Smell-Center"), 4,
				stimulus.getNeuronByLabel("Hazard"));
		parentSim.getSim().couple((SmellSensor) agent.getSensor("Smell-Center"), 5,
				stimulus.getNeuronByLabel("Flower"));
		parentSim.getSim().couple((SmellSensor) agent.getSensor("Smell-Center"), 6, stimulus.getNeuronByLabel("Mouse"));
		
		// Couplings from hearing sensors to noun lobe
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Toy\""), nouns.getNeuronByLabel("Toy"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Cheese\""), nouns.getNeuronByLabel("Cheese"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Fish\""), nouns.getNeuronByLabel("Fish"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Poison\""), nouns.getNeuronByLabel("Poison"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Hazard\""), nouns.getNeuronByLabel("Hazard"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Flower\""), nouns.getNeuronByLabel("Flower"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Mouse\""), nouns.getNeuronByLabel("Mouse"));

		// Couplings from hearing sensors to verb lobe
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Wait\""), verbs.getNeuronByLabel("Wait"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Left\""), verbs.getNeuronByLabel("Left"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Right\""), verbs.getNeuronByLabel("Right"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Forward\""), verbs.getNeuronByLabel("Forward"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Backward\""), verbs.getNeuronByLabel("Backward"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Sleep\""), verbs.getNeuronByLabel("Sleep"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Approach\""), verbs.getNeuronByLabel("Approach"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Ingest\""), verbs.getNeuronByLabel("Ingest"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Look\""), verbs.getNeuronByLabel("Look"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Smell\""), verbs.getNeuronByLabel("Smell"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Attack\""), verbs.getNeuronByLabel("Attack"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Play\""), verbs.getNeuronByLabel("Play"));
		parentSim.getSim().couple((Hearing) agent.getSensor("Hear: \"Mate\""), verbs.getNeuronByLabel("Mate"));
	}

	public void approachBehavior(double strength) {
		// Find an object in the world that fits the most active category
		// TODO: Change this to discriminate between multiple objects of the same
		// category, such that the closest object is picked.
		// The replaceAll method must be called here to remove a whitespace added at the
		// end when getMostActiveNeuron returns.
		String objOfAttentionName = attention.getMostActiveNeuron().replaceAll("\\s", "");
		OdorWorldEntity objOfAttention = agent.getParentWorld().getEntity(objOfAttentionName);

		// Follow that object
		approachObject(objOfAttention, strength);
	}

	public void approachObject(OdorWorldEntity targetObject, double motionAmount) {
		// Get the rise and run of the direct line between the agent and the
		// object.
		// double run = targetObject.getCenterX() - agent.getCenterX();
		// double rise = targetObject.getCenterY() - agent.getCenterY();

		// Calculate the slope of the direct line between the agent and the
		// object.
		// double slope = rise / run;

		// Calculate the angle we need to face from the slope of the line.
		// double targetAngle = Math.atan(slope);

		// Figure out how far to move.
		double stepSize = baseMovementStepSize * motionAmount;

		double deltaX = agent.getCenterX() + stepSize * (targetObject.getCenterX() - agent.getCenterX());
		double deltaY = agent.getCenterY() + stepSize * (targetObject.getCenterY() - agent.getCenterY());
		// double deltaHeading = agent.getHeading()
		// + stepSize * (targetObject.getCenterY() - agent.getCenterY());

		agent.setCenterLocation((float) deltaX, (float) deltaY);

	}
}
