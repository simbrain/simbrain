package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Hearing;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

import static org.simbrain.network.core.NetworkUtilsKt.connect;

/**
 * Each instance of this class represents one particular creature in the simulation.
 * This class wraps other utilities and contains helper methods for dealing
 * with particular creatures.
 *
 * @author Sharai
 */
public class Creature {

    /**
     * The name of the creature.
     */
    private String name;

    /**
     * Back reference to parent simulation.
     */
    private final CreaturesSim parentSim;

    /**
     * The creature's brain.
     */
    private CreaturesBrain brain;

    /**
     * The odor world agent belonging to this creature.
     */
    private OdorWorldEntity agent;

    /**
     * The creature's biochemistry.
     */
    private CreaturesBiochem biochem;

    /**
     * The genetic information associated with this creature.
     */
    private CreaturesGenome genome;

    /**
     * Reference to drive lobe.
     */
    private NeuronGroup drives;

    /**
     * Reference to stimulus lobe.
     */
    private NeuronGroup stimulus;

    /**
     * Reference to verb lobe.
     */
    private NeuronGroup verbs;

    /**
     * Reference to noun lobe.
     */
    private NeuronGroup nouns;

    /**
     * Reference to sensory lobe.
     */
    private NeuronGroup senses;

    /**
     * Reference to perception lobe.
     */
    private NeuronGroup perception;

    /**
     * Reference to decisions lobe.
     */
    private NeuronGroup decisions;
    // private WinnerTakeAll decisions;

    /**
     * Reference to attention lobe.
     */
    private NeuronGroup attention;

    /**
     * How quickly to approach or avoid objects.
     */
    float baseMovementStepSize = 0.01f;

    /**
     * Sensors
     */
    ObjectSensor cheeseSensor, poisonSensor, toySensor, fishSensor, hazardSensor, flowerSensor, mouseSensor;
    Hearing hearCheese, hearPoison, hearToy, hearFish, hearHazard, hearFlower, hearMouse, hearWait, hearLeft, hearRight, hearForward, hearBackward, hearSleep, hearApproach, hearIngest, hearLook, hearSmell, hearAttack, hearPlay, hearMate;

    //TODO: Add gender

    public Creature(CreaturesSim sim, String name, NetworkComponent net, OdorWorldEntity agent) {
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
        if (genome.getGene("AnimalType").getAllele().equals("cow")) {
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
        agent.setSmellSource(new SmellSource(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0})); // TODO: Still used?

        agent.addEffector(new StraightMovement());
        agent.addEffector(new Turning(Turning.LEFT));
        agent.addEffector(new Turning(Turning.RIGHT));

        // Add object sensors (Todo)
        cheeseSensor = new ObjectSensor(EntityType.SWISS);
        poisonSensor = new ObjectSensor(EntityType.POISON);
        toySensor = new ObjectSensor(EntityType.BELL);
        fishSensor = new ObjectSensor(EntityType.FISH);
        //hazardSensor = new ObjectSensor(EntityType.???);
        flowerSensor = new ObjectSensor(EntityType.PANSY);
        mouseSensor = new ObjectSensor(EntityType.MOUSE);
        agent.addSensor(cheeseSensor);
        agent.addSensor(poisonSensor);
        agent.addSensor(toySensor);
        agent.addSensor(fishSensor);
        //agent.addSensor(hazardSensor);
        agent.addSensor(flowerSensor);
        agent.addSensor(mouseSensor);

        // Add hearing sensors
        hearCheese = new Hearing("Cheese", 10);
        hearPoison = new Hearing("Poison", 10);
        hearToy = new Hearing("Toy", 10);
        hearFish = new Hearing("Fish", 10);
        hearHazard = new Hearing("Hazard", 10);
        hearFlower = new Hearing("Flower", 10);
        hearMouse = new Hearing("Mouse", 10);
        hearWait = new Hearing("Wait", 10);
        hearLeft = new Hearing("Left", 10);
        hearRight = new Hearing("Right", 10);
        hearForward = new Hearing("Forward", 10);
        hearBackward = new Hearing("Backward", 10);
        hearSleep = new Hearing("Sleep", 10);
        hearApproach = new Hearing("Approach", 10);
        hearIngest = new Hearing("Ingest", 10);
        hearLook = new Hearing("Look", 10);
        hearSmell = new Hearing("Smell", 10);
        hearAttack = new Hearing("Attack", 10);
        hearPlay = new Hearing("Play", 10);
        hearMate = new Hearing("Mate", 10);
        agent.addSensor(hearCheese);
        agent.addSensor(hearPoison);
        agent.addSensor(hearToy);
        agent.addSensor(hearFish);
        agent.addSensor(hearHazard);
        agent.addSensor(hearFlower);
        agent.addSensor(hearMouse);
        agent.addSensor(hearWait);
        agent.addSensor(hearLeft);
        agent.addSensor(hearRight);
        agent.addSensor(hearForward);
        agent.addSensor(hearBackward);
        agent.addSensor(hearSleep);
        agent.addSensor(hearApproach);
        agent.addSensor(hearIngest);
        agent.addSensor(hearLook);
        agent.addSensor(hearSmell);
        agent.addSensor(hearAttack);
        agent.addSensor(hearPlay);
        agent.addSensor(hearMate);

        // Add speech effectors
        agent.addEffector(new Speech("Toy", 1));
        agent.addEffector(new Speech("Fish", 1));
        agent.addEffector(new Speech("Cheese", 1));
        agent.addEffector(new Speech("Poison", 1));
        agent.addEffector(new Speech("Hazard", 1));
        agent.addEffector(new Speech("Flower", 1));
        agent.addEffector(new Speech("Mouse", 1));
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
        parentSim.getSim().couple(decisions.getNeuronByLabel("Left"), agent.getEffector("Turn left"));
        parentSim.getSim().couple(decisions.getNeuronByLabel("Right"), agent.getEffector("Turn right"));
        parentSim.getSim().couple(decisions.getNeuronByLabel("Forward"), agent.getEffector("Move straight"));
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
        connect(stimulus.getNeuronByLabel("Toy"), attention.getNeuronByLabel("Toy"), new CreaturesSynapseRule(), 1);
        connect(stimulus.getNeuronByLabel("Fish"), attention.getNeuronByLabel("Fish"), new CreaturesSynapseRule(), 1);
        connect(stimulus.getNeuronByLabel("Cheese"), attention.getNeuronByLabel("Cheese"), new CreaturesSynapseRule(), 1);
        connect(stimulus.getNeuronByLabel("Poison"), attention.getNeuronByLabel("Poison"), new CreaturesSynapseRule(), 1);
        connect(stimulus.getNeuronByLabel("Hazard"), attention.getNeuronByLabel("Hazard"), new CreaturesSynapseRule(), 1);
        connect(stimulus.getNeuronByLabel("Flower"), attention.getNeuronByLabel("Flower"), new CreaturesSynapseRule(), 1);
        connect(stimulus.getNeuronByLabel("Mouse"), attention.getNeuronByLabel("Mouse"), new CreaturesSynapseRule(), 1);

        // NOUNS-TO-ATTENTION
        connect(nouns.getNeuronByLabel("Toy"), attention.getNeuronByLabel("Toy"), new CreaturesSynapseRule(), 1);
        connect(nouns.getNeuronByLabel("Fish"), attention.getNeuronByLabel("Fish"), new CreaturesSynapseRule(), 1);
        connect(nouns.getNeuronByLabel("Cheese"), attention.getNeuronByLabel("Cheese"), new CreaturesSynapseRule(), 1);
        connect(nouns.getNeuronByLabel("Poison"), attention.getNeuronByLabel("Poison"), new CreaturesSynapseRule(), 1);
        connect(nouns.getNeuronByLabel("Hazard"), attention.getNeuronByLabel("Hazard"), new CreaturesSynapseRule(), 1);
        connect(nouns.getNeuronByLabel("Flower"), attention.getNeuronByLabel("Flower"), new CreaturesSynapseRule(), 1);
        connect(nouns.getNeuronByLabel("Mouse"), attention.getNeuronByLabel("Mouse"), new CreaturesSynapseRule(), 1);

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
        perception = brain.buildPerceptionLobe(new NeuronGroup[]{drives, verbs, senses, attention});

        //		// Init Perception to Concept Dendrite Pathway
        //		brain.createSynapseGroup(perception, concepts, "#0 to #8, Type 0");

        // TODO: TEMP -- Remove this when unneeded.
        tempCircuits();

    }

    private void tempCircuits() {
        double n = 0.5; // For setting a "default" positive or negative value
        double h = 1.0; // For setting a high positive or negative value, for wherever we want a very strong association

        // Pain
        connect(perception.getNeuronByLabel("Pain"),
                decisions.getNeuronByLabel("Wait"),
                new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Pain"),
                decisions.getNeuronByLabel("Sleep")
                , new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Pain"),
                decisions.getNeuronByLabel(
                "Approach"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Pain"),
                decisions.getNeuronByLabel("Attack"), new CreaturesSynapseRule(), n);
        connect(perception.getNeuronByLabel("Pain"),
                decisions.getNeuronByLabel("Play"),
                new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Pain"),
                decisions.getNeuronByLabel("Speak")
                , new CreaturesSynapseRule(), n);

        // (Need for) Comfort
        connect(perception.getNeuronByLabel("Comfort"),
                decisions.getNeuronByLabel(
                "Wait"), new CreaturesSynapseRule(), n);
        connect(perception.getNeuronByLabel("Comfort"),
                decisions.getNeuronByLabel(
                "Sleep"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Comfort"),
                decisions.getNeuronByLabel(
                "Ingest"), new CreaturesSynapseRule(), n);
        connect(perception.getNeuronByLabel("Comfort"),
                decisions.getNeuronByLabel(
                "Look"), new CreaturesSynapseRule(), n);
        connect(perception.getNeuronByLabel("Comfort"),
                decisions.getNeuronByLabel(
                "Smell"), new CreaturesSynapseRule(), n);
        connect(perception.getNeuronByLabel("Comfort"),
                decisions.getNeuronByLabel(
                "Attack"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Comfort"),
                decisions.getNeuronByLabel(
                "Play"), new CreaturesSynapseRule(), n);
        connect(perception.getNeuronByLabel("Comfort"),
                decisions.getNeuronByLabel(
                "Speak"), new CreaturesSynapseRule(), n);

        // Hunger
        connect(perception.getNeuronByLabel("Hunger"), decisions.getNeuronByLabel("Sleep"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Hunger"), decisions.getNeuronByLabel("Approach"), new CreaturesSynapseRule(), n);
        connect(perception.getNeuronByLabel("Hunger"), decisions.getNeuronByLabel("Ingest"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Hunger"), decisions.getNeuronByLabel("Smell"), new CreaturesSynapseRule(), n);

        // Temperature
        /*
         * Temp would supposedly work such that too high would be too hot and too low
         * would be too cold, but that's not quite feasible as we've set this. Maybe we
         * should split this drive into hotness/coldness like it is in Creatures?
         */
        connect(perception.getNeuronByLabel("Temperature"), decisions.getNeuronByLabel("Wait"), new CreaturesSynapseRule(), -1);

        // Fatigue
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Wait"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Left"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Right"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Forward"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Backward"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Sleep"), new CreaturesSynapseRule(), n);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Approach"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Attack"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Play"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Mate"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Fatigue"), decisions.getNeuronByLabel("Speak"), new CreaturesSynapseRule(), -n);

        // Drowsiness
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Wait"), new CreaturesSynapseRule(), n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Left"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Right"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Forward"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Backward"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Sleep"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Approach"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Ingest"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Look"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Attack"), new CreaturesSynapseRule(), -h);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Play"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Mate"), new CreaturesSynapseRule(), -n);
        connect(perception.getNeuronByLabel("Drowsiness"), decisions.getNeuronByLabel("Speak"), new CreaturesSynapseRule(), -n);

        // Mouse
        connect(perception.getNeuronByLabel("Mouse"), decisions.getNeuronByLabel("Speak"), new CreaturesSynapseRule(), n);

        // Verbs
        connect(perception.getNeuronByLabel("Wait"), decisions.getNeuronByLabel("Wait"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Left"), decisions.getNeuronByLabel("Left"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Right"), decisions.getNeuronByLabel("Right"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Forward"), decisions.getNeuronByLabel("Forward"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Backward"), decisions.getNeuronByLabel("Backward"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Sleep"), decisions.getNeuronByLabel("Sleep"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Approach"), decisions.getNeuronByLabel("Approach"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Ingest"), decisions.getNeuronByLabel("Ingest"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Look"), decisions.getNeuronByLabel("Look"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Smell"), decisions.getNeuronByLabel("Smell"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Attack"), decisions.getNeuronByLabel("Attack"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Play"), decisions.getNeuronByLabel("Play"), new CreaturesSynapseRule(), h);
        connect(perception.getNeuronByLabel("Mate"), decisions.getNeuronByLabel("Mate"), new CreaturesSynapseRule(), h);

        // brain.getNetworkComponent().connect(perception.getNeuronByLabel(label), decisions.getNeuronByLabel(label), new CreaturesSynapseRule(), value);
    }

    /**
     * Creates all essential couplings for the agent between various agent
     * components.
     */
    public void initCouplings() {

        // Couplings from center smell sensor to stimulus lobe
        parentSim.getSim().couple(cheeseSensor, stimulus.getNeuronByLabel("Cheese"));
        parentSim.getSim().couple(poisonSensor, stimulus.getNeuronByLabel("Poison"));
        parentSim.getSim().couple(toySensor, stimulus.getNeuronByLabel("Toy"));
        parentSim.getSim().couple(fishSensor, stimulus.getNeuronByLabel("Fish"));
        //parentSim.getSim().couple(hazardSensor, stimulus.getNeuronByLabel("Hazard"));
        parentSim.getSim().couple(flowerSensor, stimulus.getNeuronByLabel("Flower"));
        parentSim.getSim().couple(mouseSensor, stimulus.getNeuronByLabel("Mouse"));

        // Couplings from hearing sensors to noun lobe
        parentSim.getSim().couple(hearCheese, nouns.getNeuronByLabel("Cheese"));
        parentSim.getSim().couple(hearPoison, nouns.getNeuronByLabel("Poison"));
        parentSim.getSim().couple(hearToy, nouns.getNeuronByLabel("Toy"));
        parentSim.getSim().couple(hearFish, nouns.getNeuronByLabel("Fish"));
        parentSim.getSim().couple(hearHazard, nouns.getNeuronByLabel("Hazard"));
        parentSim.getSim().couple(hearFlower, nouns.getNeuronByLabel("Flower"));
        parentSim.getSim().couple(hearMouse, nouns.getNeuronByLabel("Mouse"));

        // Couplings from hearing sensors to verb lobe
        parentSim.getSim().couple(hearWait, verbs.getNeuronByLabel("Wait"));
        parentSim.getSim().couple(hearLeft, verbs.getNeuronByLabel("Left"));
        parentSim.getSim().couple(hearRight, verbs.getNeuronByLabel("Right"));
        parentSim.getSim().couple(hearForward, verbs.getNeuronByLabel("Forward"));
        parentSim.getSim().couple(hearBackward, verbs.getNeuronByLabel("Backward"));
        parentSim.getSim().couple(hearSleep, verbs.getNeuronByLabel("Sleep"));
        parentSim.getSim().couple(hearApproach, verbs.getNeuronByLabel("Approach"));
        parentSim.getSim().couple(hearIngest, verbs.getNeuronByLabel("Ingest"));
        parentSim.getSim().couple(hearLook, verbs.getNeuronByLabel("Look"));
        parentSim.getSim().couple(hearSmell, verbs.getNeuronByLabel("Smell"));
        parentSim.getSim().couple(hearAttack, verbs.getNeuronByLabel("Attack"));
        parentSim.getSim().couple(hearPlay, verbs.getNeuronByLabel("Play"));
        parentSim.getSim().couple(hearMate, verbs.getNeuronByLabel("Mate"));

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
        Producer chemicalAmount = parentSim.getSim().getProducer(chem, "getAmount");
        Consumer chemReceptor = parentSim.getSim().getConsumer(neuron, "forceSetActivation");
        parentSim.getSim().couple(chemicalAmount, chemReceptor);
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
//        double delta_x = agent.getCenterX() - targetObject.getCenterX();
//        double delta_y = agent.getCenterY() - targetObject.getCenterY();
//        double targetHeading = Math.toDegrees(Math.atan2(delta_y, delta_x));
//        targetHeading = ((targetHeading < 0) ? targetHeading + 360 : targetHeading);

        // System.out.println(targetHeading + "," + agent.getHeading());

        // Update position and heading
        // TODO :Heading update feels unnatural. Maybe use braitenberg method instead,
        // or else improve this
        double stepSize = baseMovementStepSize * motionAmount;
//        double newX = agent.getCenterX() + stepSize * (targetObject.getX() - agent.getCenterX());
//        double newY = agent.getCenterY() + stepSize * (targetObject.getY() - agent.getCenterY());
//        double newHeading = agent.getHeading() + .1 * (targetHeading - agent.getHeading());
//        agent.setCenterLocation((float) newX, (float) newY);
//        agent.setHeading(newHeading);

    }

    public void speakBehavior(double speakActivation) {
        // TODO: Make the agent also speak the name of the decision neuron with either
        // the second highest activation (after the speak neuron itself) OR the decision
        // neuron with the highest activation in the next simulation iteration

        // Get the noun to speak, and the effector that goes with it
        String noun = attention.getMostActiveNeuron().replaceAll("\\s", "");
        if (noun.isEmpty()) {
            return;
        }
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

    public OdorWorldEntity getAgent() {
        return (agent);
    }

    // Was not here before last pull
    public void setAgentLocation(float x, float y) {
//        agent.setCenterLocation(x, y);
    }

    // Was not here before last pull
    public void setAgentSkin(String type) {
        // TODO: Set entity type
//        agent.setEntityType(type);
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
