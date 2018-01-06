package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.workspace.Coupling2;
import org.simbrain.workspace.Consumer2;
import org.simbrain.workspace.MismatchedAttributesException;
import org.simbrain.workspace.Producer2;
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
	 * The creature's brain.
	 */
	private CreaturesBrain brain;

	/**
	 * The odor world agent belonging to this creature.
	 */
	private RotatingEntity agent;

	/**
	 * The creature's biochemistry.
	 */
	private CreaturesBiochem biochem;
	
	/** The genetic information associated with this creature. */
    private CreaturesGenome genome;

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
	float baseMovementStepSize = 0.01f;
	
	//TODO: Add gender

	public Creature(CreaturesSim sim, String name, NetworkWrapper net, RotatingEntity agent) {
		this.parentSim = sim;
		this.name = name;

		this.agent = agent;
		initAgent();

		this.brain = new CreaturesBrain(net, sim);
		initDefaultBrain();

		this.biochem = new CreaturesBiochem();
		this.genome = new CreaturesGenome();
		
		// Temp code to illustrate how you might use genetics
		System.out.println(genome.getGene("AnimalType"));
		if(genome.getGene("AnimalType").getAllele().equals("cow")) {
		    System.out.println("It's a cow!");
		}

		initCouplings();

		// TEMP
		initChemDashboard();
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

		double speakActivation = decisions.getNeuronByLabel("Speak").getActivation();
		if (speakActivation > 0) {
			speakBehavior(speakActivation);
		}

		biochem.update();
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

		// Add speech effectors
		agent.addEffector(new Speech(agent, "Toy", 1));
		agent.addEffector(new Speech(agent, "Fish", 1));
		agent.addEffector(new Speech(agent, "Cheese", 1));
		agent.addEffector(new Speech(agent, "Poison", 1));
		agent.addEffector(new Speech(agent, "Hazard", 1));
		agent.addEffector(new Speech(agent, "Flower", 1));
		agent.addEffector(new Speech(agent, "Mouse", 1));
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
		decisions = brain.createLobe(615.21, 1002.02, verbs.size(), "line", "Decisions Lobe");
		// decisions = brain.createWTALobe(538.67, 922.84, verbs.size(), "vertical
		// line", "Lobe #6: Decisions");
		brain.copyLabels(verbs, decisions);

		// Coupling some decision lobe cells to agent effectors
		parentSim.getSim().couple(decisions.getNeuronByLabel("Left"), agent.getEffector("Go-left"));
		parentSim.getSim().couple(decisions.getNeuronByLabel("Right"), agent.getEffector("Go-right"));
		parentSim.getSim().couple(decisions.getNeuronByLabel("Forward"), agent.getEffector("Go-straight"));
		// TODO: Is there a way to couple the "Backward" node to the Go-straight
		// effector in such a way that the inverse of the Backward node's activation is
		// what the Go-straight effector gets?

		// Init Lobe #7: Attention
		// TODO: Make this a WTA lobe.
		attention = brain.createLobe(440.55, 872.98, stimulus.size(), "vertical line", "Attention Lobe");
		brain.copyLabels(stimulus, attention);

		// Init Attention System Dendrite Pathways
		/*
		 * TODO: Connecting neurons w/ OneToOne is not hooking up the right neurons
		 * together. Doing this manually for now. Would prefer to handle this in a
		 * synapse group in the future.
		 */
		// STIMULUS-TO-ATTENTION
		brain.getNetworkWrapper().connect(stimulus.getNeuronByLabel("Toy"), attention.getNeuronByLabel("Toy"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(stimulus.getNeuronByLabel("Fish"), attention.getNeuronByLabel("Fish"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(stimulus.getNeuronByLabel("Cheese"), attention.getNeuronByLabel("Cheese"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(stimulus.getNeuronByLabel("Poison"), attention.getNeuronByLabel("Poison"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(stimulus.getNeuronByLabel("Hazard"), attention.getNeuronByLabel("Hazard"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(stimulus.getNeuronByLabel("Flower"), attention.getNeuronByLabel("Flower"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(stimulus.getNeuronByLabel("Mouse"), attention.getNeuronByLabel("Mouse"),
				new CreaturesSynapseRule(), 1);

		// NOUNS-TO-ATTENTION
		brain.getNetworkWrapper().connect(nouns.getNeuronByLabel("Toy"), attention.getNeuronByLabel("Toy"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(nouns.getNeuronByLabel("Fish"), attention.getNeuronByLabel("Fish"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(nouns.getNeuronByLabel("Cheese"), attention.getNeuronByLabel("Cheese"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(nouns.getNeuronByLabel("Poison"), attention.getNeuronByLabel("Poison"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(nouns.getNeuronByLabel("Hazard"), attention.getNeuronByLabel("Hazard"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(nouns.getNeuronByLabel("Flower"), attention.getNeuronByLabel("Flower"),
				new CreaturesSynapseRule(), 1);
		brain.getNetworkWrapper().connect(nouns.getNeuronByLabel("Mouse"), attention.getNeuronByLabel("Mouse"),
				new CreaturesSynapseRule(), 1);

//		// Init Lobe #8: Concepts
//		NeuronGroup concepts = brain.createLobe(1086.94, -21.61, 640, "grid", "Lobe #8: Concepts");
//		brain.setLobeColumns(concepts, 20);
//
//		// Init Concepts to Decisions Dendrite Pathways
//		brain.createSynapseGroup(concepts, decisions, "#8 to #6, Type 0");
//		// TODO: BUG: This second synapse group sometimes has a graphical glitch
//		// (Arrow points to space).
//		brain.createSynapseGroup(concepts, decisions, "#8 to #6, Type 1");

		// Init Lobe #0: Perception
		perception = brain.buildPerceptionLobe(new NeuronGroup[] { drives, verbs, senses, attention });

//		// Init Perception to Concept Dendrite Pathway
//		brain.createSynapseGroup(perception, concepts, "#0 to #8, Type 0");

		// TODO: TEMP -- Remove this when unneeded.
		tempCircuits();

	}

	private void tempCircuits() {
		double n = 0.5; // For setting a "default" positive or negative value
		double h = 1.0; // For setting a high positive or negative value, for wherever we want a very strong association

		// Pain
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Pain"), decisions.getNeuronByLabel("Wait"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Pain"), decisions.getNeuronByLabel("Sleep"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Pain"), decisions.getNeuronByLabel("Approach"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Pain"), decisions.getNeuronByLabel("Attack"),
				new CreaturesSynapseRule(), n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Pain"), decisions.getNeuronByLabel("Play"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Pain"), decisions.getNeuronByLabel("Speak"),
				new CreaturesSynapseRule(), n);

		// (Need for) Comfort
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Comfort"), decisions.getNeuronByLabel("Wait"),
				new CreaturesSynapseRule(), n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Comfort"), decisions.getNeuronByLabel("Sleep"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Comfort"), decisions.getNeuronByLabel("Ingest"),
				new CreaturesSynapseRule(), n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Comfort"), decisions.getNeuronByLabel("Look"),
				new CreaturesSynapseRule(), n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Comfort"), decisions.getNeuronByLabel("Smell"),
				new CreaturesSynapseRule(), n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Comfort"), decisions.getNeuronByLabel("Attack"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Comfort"), decisions.getNeuronByLabel("Play"),
				new CreaturesSynapseRule(), n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Comfort"), decisions.getNeuronByLabel("Speak"),
				new CreaturesSynapseRule(), n);

		// Hunger
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Hunger"), decisions.getNeuronByLabel("Sleep"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Hunger"), decisions.getNeuronByLabel("Approach"),
				new CreaturesSynapseRule(), n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Hunger"), decisions.getNeuronByLabel("Ingest"),
				new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Hunger"), decisions.getNeuronByLabel("Smell"),
				new CreaturesSynapseRule(), n);

		// Temperature
		/*
		 * Temp would supposedly work such that too high would be too hot and too low
		 * would be too cold, but that's not quite feasible as we've set this. Maybe we
		 * should split this drive into hotness/coldness like it is in Creatures?
		 */
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Temperature"),
				decisions.getNeuronByLabel("Wait"), new CreaturesSynapseRule(), -1);

		// Fatigue
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Wait"),
				new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Left"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Right"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Forward"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"),
				decisions.getNeuronByLabel("Backward"), new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Sleep"),
				new CreaturesSynapseRule(), n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"),
				decisions.getNeuronByLabel("Approach"), new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Attack"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Play"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Mate"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Speak"),
				new CreaturesSynapseRule(), -n);

		// Drowsiness
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Wait"),
				new CreaturesSynapseRule(), n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Left"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"),
				decisions.getNeuronByLabel("Right"), new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"),
				decisions.getNeuronByLabel("Forward"), new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"),
				decisions.getNeuronByLabel("Backward"), new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"),
				decisions.getNeuronByLabel("Sleep"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"),
				decisions.getNeuronByLabel("Approach"), new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"),
				decisions.getNeuronByLabel("Ingest"), new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Look"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"),
				decisions.getNeuronByLabel("Attack"), new CreaturesSynapseRule(), -h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Play"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Mate"),
				new CreaturesSynapseRule(), -n);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Drowsiness"),
				decisions.getNeuronByLabel("Speak"), new CreaturesSynapseRule(), -n);

		// Mouse
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Mouse"), decisions.getNeuronByLabel("Speak"),
				new CreaturesSynapseRule(), n);
		
		// Verbs
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Wait"), decisions.getNeuronByLabel("Wait"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Left"), decisions.getNeuronByLabel("Left"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Right"), decisions.getNeuronByLabel("Right"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Forward"), decisions.getNeuronByLabel("Forward"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Backward"), decisions.getNeuronByLabel("Backward"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Sleep"), decisions.getNeuronByLabel("Sleep"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Approach"), decisions.getNeuronByLabel("Approach"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Ingest"), decisions.getNeuronByLabel("Ingest"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Look"), decisions.getNeuronByLabel("Look"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Smell"), decisions.getNeuronByLabel("Smell"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Attack"), decisions.getNeuronByLabel("Attack"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Play"), decisions.getNeuronByLabel("Play"), new CreaturesSynapseRule(), h);
		brain.getNetworkWrapper().connect(perception.getNeuronByLabel("Mate"), decisions.getNeuronByLabel("Mate"), new CreaturesSynapseRule(), h);

		// brain.getNetworkWrapper().connect(perception.getNeuronByLabel(label), decisions.getNeuronByLabel(label), new CreaturesSynapseRule(), value);
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

		// Couplings from biochemistry to the brain
		couple(biochem.getChemByIndex(1), drives.getNeuronByLabel("Pain"));
		couple(biochem.getChemByIndex(2), drives.getNeuronByLabel("Comfort"));
		couple(biochem.getChemByIndex(3), drives.getNeuronByLabel("Hunger"));
		couple(biochem.getChemByIndex(4), drives.getNeuronByLabel("Temperature"));
		couple(biochem.getChemByIndex(5), drives.getNeuronByLabel("Fatigue"));
		couple(biochem.getChemByIndex(6), drives.getNeuronByLabel("Drowsiness"));
		couple(biochem.getChemByIndex(7), drives.getNeuronByLabel("Lonliness"));
		couple(biochem.getChemByIndex(8), drives.getNeuronByLabel("Crowdedness"));
		couple(biochem.getChemByIndex(9), drives.getNeuronByLabel("Fear"));
		couple(biochem.getChemByIndex(10), drives.getNeuronByLabel("Boredom"));
		couple(biochem.getChemByIndex(11), drives.getNeuronByLabel("Anger"));
		couple(biochem.getChemByIndex(12), drives.getNeuronByLabel("Arousal"));
	}

	private void couple(CreaturesChem chem, Neuron neuron) {
		NetworkComponent nc = brain.getNetworkWrapper().getNetworkComponent();

		// Hopefully borrowing nc's attribute manager for the producer won't cause
		// problems later...
		Producer2 chemicalAmount = nc.getProducer(chem, "getAmount");
		Consumer2 chemReceptor = nc.getConsumer(neuron, "forceSetActivation");

		try {
			parentSim.getSim().createCoupling(chemicalAmount, chemReceptor);
		} catch (MismatchedAttributesException e) {
			e.printStackTrace();
		}
	}

	public void approachBehavior(double strength) {
		OdorWorldEntity objOfAttention = findObjectOfAttention();

		// Follow that object
		approachObject(objOfAttention, strength);
	}

	/**
	 * Find an object in the world that fits the most active category
	 * 
	 * @return
	 */
	// TODO: Change this to discriminate between multiple objects of the same
	// category, such that the closest object is picked.
	private OdorWorldEntity findObjectOfAttention() {
		// The replaceAll method must be called here to remove a whitespace added at the
		// end when getMostActiveNeuron returns.
		String objOfAttentionName = attention.getMostActiveNeuron().replaceAll("\\s", "");
		OdorWorldEntity objOfAttention = agent.getParentWorld().getEntity(objOfAttentionName);

		return objOfAttention;
	}

	public void approachObject(OdorWorldEntity targetObject, double motionAmount) {

		if (targetObject == null) {
			System.err.println("Null pointer on target object");
			return;
		}

		// Calculate the target heading for the agent
		double delta_x = agent.getCenterX() - targetObject.getCenterX();
		double delta_y = agent.getCenterY() - targetObject.getCenterY();
		double targetHeading = Math.toDegrees(Math.atan2(delta_y, delta_x));
		targetHeading = ((targetHeading < 0) ? targetHeading + 360 : targetHeading);

		// System.out.println(targetHeading + "," + agent.getHeading());

		// Update position and heading
		// TODO :Heading update feels unnatural. Maybe use braitenberg method instead,
		// or else improve this
		double stepSize = baseMovementStepSize * motionAmount;
		double newX = agent.getCenterX() + stepSize * (targetObject.getX() - agent.getCenterX());
		double newY = agent.getCenterY() + stepSize * (targetObject.getY() - agent.getCenterY());
		double newHeading = agent.getHeading() + .1 * (targetHeading - agent.getHeading());
		agent.setCenterLocation((float) newX, (float) newY);
		agent.setHeading(newHeading);

	}

	public void speakBehavior(double speakActivation) {
		// TODO: Make the agent also speak the name of the decision neuron with either
		// the second highest activation (after the speak neuron itself) OR the decision
		// neuron with the highest activation in the next simulation iteration

		// Get the noun to speak, and the effector that goes with it
		String noun = attention.getMostActiveNeuron().replaceAll("\\s", "");
		String effectorName = "Say: \"" + noun + "\"";
		Speech effector = (Speech) agent.getEffector(effectorName);

		// If the speak activation is above 1, the agent will say the noun.
		if (effector != null) {
			effector.setAmount(speakActivation);
		} else {
			System.err.println("Could not find effector:" + effectorName);
		}
	}

	// TODO
	public void sleepBehavior() {
		/*
		 * This behavior needs to do 2 things: <ul> <li>1. Paralyze the creature
		 * temporarily, such that it won't be able to do anything even if their brain is
		 * thinking of it.</li> <li>2. Send some sort of "sleeping" signal to any
		 * chemical emitters or stimulus genes that need it.</li> </ul>
		 * 
		 * We could probably get both done at once by just having some boolean isAsleep
		 * variable on the Creature level
		 */
	}

	public void ingestBehavior() {
		OdorWorldEntity objOfAttention = findObjectOfAttention();

		// TODO: Perform a check (using center smell sensor) to make sure the object is
		// close enough to eat. If true, then:
		// ingestObject(objOfAttention)
	}

	public void ingestObject(OdorWorldEntity targetObject) {
		// Based off the object's name, ID, or smell, add some concoction of chemicals
		// to the creature's biochemistry (if applicable).
		// TODO: Implement this once sufficent biochemistry is implemented
	}

	public void lookBehavior() {
		OdorWorldEntity objOfAttention = findObjectOfAttention();

		// Based off the object's name, ID, or smell, add some concoction of chemicals
		// to the creature's biochemistry (if applicable).
		// TODO: Implement this once sufficent biochemistry is implemented
	}

	public void smellBehavior() {
		OdorWorldEntity objOfAttention = findObjectOfAttention();

		// Based off the object's name, ID, or smell, add some concoction of chemicals
		// to the creature's biochemistry (if applicable). Use the strength of the smell
		// to determine the dosage.
		// TODO: Implement this once sufficent biochemistry is implemented
	}

	public void attackBehavior() {
		OdorWorldEntity objOfAttention = findObjectOfAttention();

		// TODO: Perform a check (using center smell sensor) to make sure the object is
		// close enough to attack. If true, then:
		// attackObject(objOfAttention);
	}

	public void attackObject(OdorWorldEntity targetObject) {
		// If the attacked object is not another creature, don't do anything (unless
		// it's a hazard or tough objects like a candle or bell, in which case add some
		// pain and other related chemicals)
		// If the attacked object IS a creature, then we'll need to add the relevenat
		// chemicals and stimulus to the target creature.
	}

	public void playBehavior() {
		OdorWorldEntity objOfAttention = findObjectOfAttention();

		// TODO: Perform a check (using center smell sensor) to make sure the object is
		// close enough to play with. If true, then:
		// playObject(objOfAttention);
	}

	public void playObject(OdorWorldEntity targetObject) {

	}

	public void mateBehavior() {
	}

	public String getName() {
		return (name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public CreaturesBrain getBrain() {
		return (brain);
	}

	public RotatingEntity getAgent() {
		return (agent);
	}

	// Was not here before last pull
	public void setAgentLocation(float x, float y) {
		agent.setCenterLocation(x, y);
	}

	// Was not here before last pull
	public void setAgentSkin(String type) {
		agent.setEntityType(type);
	}

	// Was not here before last pull
	public void deleteLobe(NeuronGroup lobe) {
		lobe.delete();
	}

	// Was not here before last pull
	public void injectChem(String name, double dose) {
		biochem.getChemByName(name).incrementAmount(dose);

		// Needles hurt!
		biochem.getChemByName("Pain").incrementAmount(1);
	}

	// TEMP
	public void initChemDashboard() {
		NeuronGroup dash = brain.createLobe(558.45, 1122.53, 2, "grid", "Chem Dashboard");
		dash.getNeuron(0).setLabel("Endorphin");
		dash.getNeuron(1).setLabel("Reward");

		couple(biochem.getChemByName("Endorphin"), dash.getNeuron(0));
		couple(biochem.getChemByName("Reward"), dash.getNeuron(1));
	}

}
