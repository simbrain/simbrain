package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionAdapter;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.groups.NeuronGroup;

/**
 * A simulation of an A-Life agent based off of the Creatures entertainment
 * software by Cyberlife Technology & Steve Grand, as seen in "Creatures:
 * Entertainment software agents with artificial life" (D. Cliff & S. Grand,
 * 1998)
 *
 * @author Sharai
 *
 */
public class Creatures extends RegisteredSimulation {

	/**
	 * A list of brains. Good for updating and maintaining multiple brains
	 */
	private List<CreaturesBrain> brainList = new ArrayList();

	@Override
	public void run() {

		// Clear workspace
		sim.getWorkspace().clearWorkspace();

		// Add doc viewer
		sim.addDocViewer(0, 0, 450, 600, "Doc",
				"src/org/simbrain/custom_sims/simulations/creatures/CreaturesDoc.html");

		// Create starting brain
		CreaturesBrain ronBrain = createBrain(451, 0, 550, 600, "Ron");
		initDefaultBrain(ronBrain);

		// Create update action
		sim.getWorkspace().addUpdateAction(
				new UpdateActionAdapter("Update Creatures Sim") {
					@Override
					public void invoke() {
						updateCreaturesSim();
					}
				});

	}

	/**
	 * Update function for the Creatures simulation.
	 */
	void updateCreaturesSim() {
		// Update all brains
		for (CreaturesBrain b : brainList) {
			b.getNetwork().update();
		}

	}

	/**
	 * Gives an empty creature's brain a network from a template.
	 * @param brain
	 */
	private void initDefaultBrain(CreaturesBrain brain) {
		// Init Lobe #1: Drives
		NeuronGroup drives =
				brain.createLobe(1580, 1300, 12, "grid", "Lobe #1: Drives");
		brain.setLobeColumns(drives, 6);
		brain.nameNeuron(drives, 0, "Pain");
		brain.nameNeuron(drives, 1, "Comfort");
		brain.nameNeuron(drives, 2, "Hunger");
		brain.nameNeuron(drives, 3, "Temperature");
		brain.nameNeuron(drives, 4, "Fatigue");
		brain.nameNeuron(drives, 5, "Drowsiness");
		brain.nameNeuron(drives, 6, "Lonliness");
		brain.nameNeuron(drives, 7, "Crowdedness");
		brain.nameNeuron(drives, 8, "Fear");
		brain.nameNeuron(drives, 9, "Boredom");
		brain.nameNeuron(drives, 10, "Anger");
		brain.nameNeuron(drives, 11, "Arousal");

		// Init Lobe #2: Stimulus Source
		NeuronGroup stimulus =
				brain.createLobe(610, 995, 7,
						"line", "Lobe #2: Stimulus Source");
		brain.nameNeuron(stimulus, 0, "Toy");
		brain.nameNeuron(stimulus, 1, "Fish");
		brain.nameNeuron(stimulus, 2, "Cheese");
		brain.nameNeuron(stimulus, 3, "Poison");
		brain.nameNeuron(stimulus, 4, "Hazard");
		brain.nameNeuron(stimulus, 5, "Flower");
		brain.nameNeuron(stimulus, 6, "Mouse");

		// Init Lobe #3: Verbs
		NeuronGroup verbs =
				brain.createLobe(1715, 1000, 13, "grid", "Lobe #3: Verbs");
		brain.setLobeColumns(verbs, 7);
		brain.nameNeuron(verbs, 0, "Wait");
		brain.nameNeuron(verbs, 1, "Left");
		brain.nameNeuron(verbs, 2, "Right");
		brain.nameNeuron(verbs, 3, "Forward");
		brain.nameNeuron(verbs, 4, "Backward");
		brain.nameNeuron(verbs, 5, "Sleep");
		brain.nameNeuron(verbs, 6, "Approach");
		brain.nameNeuron(verbs, 7, "Ingest");
		brain.nameNeuron(verbs, 8, "Look");
		brain.nameNeuron(verbs, 9, "Smell");
		brain.nameNeuron(verbs, 10, "Attack");
		brain.nameNeuron(verbs, 11, "Play");
		brain.nameNeuron(verbs, 12, "Mate");

		// Init Lobe #4: Nouns
		NeuronGroup nouns =
				brain.createLobe(910, -40, stimulus.size(), "line",
						"Lobe #4: Nouns");
		brain.copyLabels(stimulus, nouns);

		// Init Lobe #5: General Senses
		NeuronGroup senses =
				brain.createLobe(1490, 1550, 14,
						"grid", "Lobe #5: General Senses");
		brain.setLobeColumns(senses, 7);
		brain.nameNeuron(senses, 0, "Attacked");
		brain.nameNeuron(senses, 1, "Playing");
		brain.nameNeuron(senses, 2, "User Talked");
		brain.nameNeuron(senses, 3, "Mouse Talked");
		brain.nameNeuron(senses, 4, "It Approaches");
		brain.nameNeuron(senses, 5, "It is Near");
		brain.nameNeuron(senses, 6, "It Retreats");
		brain.nameNeuron(senses, 7, "Is Object");
		brain.nameNeuron(senses, 8, "Is Mouse");
		brain.nameNeuron(senses, 9, "Is Parent");
		brain.nameNeuron(senses, 10, "Is Sibling");
		brain.nameNeuron(senses, 11, "Is Child");
		brain.nameNeuron(senses, 12, "Opposite Sex");
		brain.nameNeuron(senses, 13, "Audible Event");

		// Init Lobe #6: Decisions
		NeuronGroup decisions =
				brain.createLobe(2510, 440, verbs.size(),
						"vertical line", "Lobe #6: Decisions");
		brain.copyLabels(verbs, decisions);

		// Init Lobe #7: Attention
		NeuronGroup attention =
				brain.createLobe(2090, 1260, stimulus.size(),
						"vertical line", "Lobe #7: Attention");
		brain.copyLabels(stimulus, attention);

		// Init Lobe #8: Concepts
		NeuronGroup concepts =
				brain.createLobe(460, 95, 640, "grid", "Lobe #8: Concepts");
		brain.setLobeColumns(concepts, 40);

		// Init Lobe #0: Perception
		NeuronGroup perception =
				brain.createLobe(65, 440,
							(drives.size() + verbs.size()
							+ senses.size() + attention.size()),
							"grid", "Lobe #0: Perception");
		brain.setLobeColumns(perception, 7);
		// Copy labels from multiple lobes to this one
		for (int i = 0; i < perception.size(); i++) {
			// Labeling after Lobe #1
			if (i < drives.size()) {
				brain.nameNeuron(perception, i,
						brain.getNeuronLabel(drives, i));
			}
			// Labeling after Lobe #3
			else if (i < (drives.size() + verbs.size())) {
				brain.nameNeuron(perception, i,
						brain.getNeuronLabel(verbs, i - drives.size()));
			}
			// Labeling after Lobe #5
			else if (i < (drives.size() + verbs.size() + senses.size())) {
				brain.nameNeuron(perception, i,
						brain.getNeuronLabel(senses,
								i - (drives.size() + verbs.size())));
			}
			// Labeling after Lobe #7
			else {
				brain.nameNeuron(perception, i,
						brain.getNeuronLabel(attention, i
								- (drives.size()
										+ verbs.size() + senses.size())));
			}
		}

	}

	/**
	 * Creates a brain network and displays it in the workspace.
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param name
	 * @return a CreaturesBrain object
	 */
	public CreaturesBrain createBrain(int x, int y,
			int width, int height, String name) {
		NetBuilder net = sim.addNetwork(x, y, width, height, name + "'s Brain");
		CreaturesBrain brain = new CreaturesBrain(net);
		brainList.add(brain);
		return brain;
	}

	public CreaturesBrain createBrain(int x, int y,
			int width, int height) {
		return createBrain(x, y, width, height, "Creature");
	}

	/**
	 * Constructor.
	 * @param desktop
	 */
	public Creatures(SimbrainDesktop desktop) {
		super(desktop);
	}

	public Creatures() {
		super();
	}

	/**
	 * Runs the constructor for the simulation.
	 */
	@Override
	public Creatures instantiate(SimbrainDesktop desktop) {
		return new Creatures(desktop);
	}

	// Accessor methods below this point
	@Override
	public String getName() {
		return "Creatures";
	}

	public List<CreaturesBrain> getBrainlist() {
		return brainList;
	}

}
