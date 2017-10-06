package org.simbrain.custom_sims.simulations.creatures;

import java.util.List;

import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.workspace.Coupling;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

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

	/** Reference to couplings consumed by the perception lobe. */
	private List<Coupling<?>> perceptCouplings;

	/** How quickly to approach or avoid objects. */
	float baseMovementStepSize = 0.001f;

	public Creature(CreaturesSim sim, String name, NetBuilder net, RotatingEntity agent) {
		this.parentSim = sim;
		this.name = name;

		this.brain = new CreaturesBrain(net, sim);
		initDefaultBrain();

		this.agent = agent;
		initAgent();
	}

	/**
	 * Update the various components of the creature.
	 */
	public void update() {
		// TODO: These coupling doesn't seem to be working right now. Need to test this.
		parentSim.getSim().getWorkspace().getCouplingManager().updateCouplings(perceptCouplings);
		brain.getNetwork().update();

		double hungerActivation = drives.getNeuronByLabel("Hunger").getActivation();
		if (hungerActivation > 0) {
			// TODO: Set amount value to something that will get higher than 1 only at
			// extreme values (such as 255). We don't slow down to a crawl to get something
			// we only want something a little bit, but we will run if there's a
			// dire need. Alternatively, we can have the base movement step size change
			// instead? Or do this calculation in method?
			this.approachObject(parentSim.cheese, 1);
		}
	}

	/**
	 * Configure the agent's name, smells, sensors, and effectors.
	 */
	private void initAgent() {
		agent.setName(name);
		agent.setSmellSource(new SmellSource(new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 255.0 }));
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
		NeuronGroup decisions = brain.createLobe(2510, 440, verbs.size(), "vertical line", "Lobe #6: Decisions");
		brain.copyLabels(verbs, decisions);

		// Init Lobe #7: Attention
		// TODO: Make this a WTA lobe.
		NeuronGroup attention = brain.createLobe(2090, 1260, stimulus.size(), "vertical line", "Lobe #7: Attention");
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
		NeuronGroup concepts = brain.createLobe(460, 95, 640, "grid", "Lobe #8: Concepts");
		brain.setLobeColumns(concepts, 40);

		// Init Concepts to Decisions Dendrite Pathways
		brain.createSynapseGroup(concepts, decisions, "#8 to #6, Type 0");
		// TODO: BUG: This second synapse group sometimes has a graphical glitch (Arrow
		// points to space).
		brain.createSynapseGroup(concepts, decisions, "#8 to #6, Type 1");

		// Init Lobe #0: Perception
		perception = brain.buildPerceptionLobe(new NeuronGroup[] { drives, verbs, senses, attention });

		// Grabs the object for the reference perceptCouplings
		perceptCouplings = brain.getPerceptCouplings();

		// Init Perception to Concept Dendrite Pathway
		brain.createSynapseGroup(perception, concepts, "#0 to #8, Type 0");

	}

	public void approachBehavior() {
		// Find the nearest object and approach
		// Could other conditions as needed
	}

	public void approachObject(OdorWorldEntity targetObject, double motionAmount) {
		// Get the rise and run of the direct line between the agent and the object.
		double run = targetObject.getCenterX() - agent.getCenterX();
		double rise = targetObject.getCenterY() - agent.getCenterY();

		// Calculate the slope of the direct line between the agent and the object.
		double slope = rise / run;

		// Calculate the angle we need to face from the slope of the line.
		double targetAngle = Math.atan(slope);

		// Figure out how far to move.
		double stepSize = baseMovementStepSize * motionAmount;

		double deltaX = agent.getCenterX() + stepSize * (targetObject.getCenterX() - agent.getCenterX());
		double deltaY = agent.getCenterY() + stepSize * (targetObject.getCenterY() - agent.getCenterY());
		// double deltaHeading = agent.getHeading()
		// + stepSize * (targetObject.getCenterY() - agent.getCenterY());

		agent.setCenterLocation((float) deltaX, (float) deltaY);

	}
}
