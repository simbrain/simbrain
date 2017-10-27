package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
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

	public Creature(CreaturesSim sim, String name, NetBuilder net, RotatingEntity agent) {
		this.parentSim = sim;
		this.name = name;

		this.agent = agent;
		initAgent();

		this.brain = new CreaturesBrain(net, sim);
		initDefaultBrain();

		this.biochem = new CreaturesBiochem();

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
		decisions = brain.createLobe(538.67, 922.84, verbs.size(), "vertical line", "Decisions Lobe");
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

		// Couplings from biochemistry to the brain
		couple(biochem.getChemByName("Pain"), drives.getNeuronByLabel("Pain"));
	}

	private void couple(CreaturesChem chem, Neuron neuron) {
		NetworkComponent nc = brain.getBuilder().getNetworkComponent();

		// Hopefully borrowing nc's attribute manager for the producer won't cause
		// problems later...
		PotentialProducer chemicalAmount = nc.getAttributeManager().createPotentialProducer(chem, "getAmount",
				double.class);
		PotentialConsumer chemReceptor = nc.getNeuronConsumer(nc, neuron, "forceSetActivation");

		parentSim.getSim().addCoupling(new Coupling(chemicalAmount, chemReceptor));
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
		Speech effector = (Speech) agent.getEffector("Say: \"" + noun + "\"");

		// If the speak activation is above 1, the agent will say the noun.
		effector.setAmount(speakActivation);
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

}
