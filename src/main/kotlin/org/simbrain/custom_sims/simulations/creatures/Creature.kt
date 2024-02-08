package org.simbrain.custom_sims.simulations.creatures
//
// import org.simbrain.network.NetworkComponent
// import org.simbrain.network.core.Network
// import org.simbrain.network.core.Neuron
// import org.simbrain.network.core.Synapse
// import org.simbrain.network.learningrules.SynapseUpdateRule
// import org.simbrain.network.neurongroups.NeuronGroup
// import org.simbrain.util.SmellSource
// import org.simbrain.world.odorworld.effectors.Speech
// import org.simbrain.world.odorworld.effectors.StraightMovement
// import org.simbrain.world.odorworld.effectors.Turning
// import org.simbrain.world.odorworld.entities.EntityType
// import org.simbrain.world.odorworld.entities.OdorWorldEntity
// import org.simbrain.world.odorworld.sensors.Hearing
// import org.simbrain.world.odorworld.sensors.ObjectSensor
//
// /**
//  * Each instance of this class represents one particular creature in the simulation.
//  * This class wraps other utilities and contains helper methods for dealing
//  * with particular creatures.
//  *
//  * @author Sharai
//  */
// class Creature(
//     /**
//      * Back reference to parent simulation.
//      */
//     private val parentSim: CreaturesSim,
//     /**
//      * The name of the creature.
//      */
//     private var name: String, net: NetworkComponent?,
//     /**
//      * The odor world agent belonging to this creature.
//      */
//     private val agent: OdorWorldEntity
// ) {
//     /**
//      * The creature's brain.
//      */
//     private val brain: CreaturesBrain
//
//     /**
//      * The creature's biochemistry.
//      */
//     private val biochem: CreaturesBiochem
//
//     /**
//      * The genetic information associated with this creature.
//      */
//     private val genome: CreaturesGenome
//
//     /**
//      * Reference to drive lobe.
//      */
//     private var drives: NeuronGroup? = null
//
//     /**
//      * Reference to stimulus lobe.
//      */
//     private lateinit var stimulus: NeuronGroup
//
//     /**
//      * Reference to verb lobe.
//      */
//     private lateinit var verbs: NeuronGroup
//
//     /**
//      * Reference to noun lobe.
//      */
//     private lateinit var nouns: NeuronGroup
//
//     /**
//      * Reference to sensory lobe.
//      */
//     private lateinit var senses: NeuronGroup
//
//     /**
//      * Reference to perception lobe.
//      */
//     private var perception: NeuronGroup? = null
//
//     /**
//      * Reference to decisions lobe.
//      */
//     private lateinit var decisions: NeuronGroup
//
//     // private WinnerTakeAll decisions;
//     /**
//      * Reference to attention lobe.
//      */
//     private lateinit var attention: NeuronGroup
//
//     /**
//      * How quickly to approach or avoid objects.
//      */
//     var baseMovementStepSize: Float = 0.01f
//
//     /**
//      * Sensors
//      */
//     var cheeseSensor: ObjectSensor? = null
//     var poisonSensor: ObjectSensor? = null
//     var toySensor: ObjectSensor? = null
//     var fishSensor: ObjectSensor? = null
//     var hazardSensor: ObjectSensor? = null
//     var flowerSensor: ObjectSensor? = null
//     var mouseSensor: ObjectSensor? = null
//     var hearCheese: Hearing? = null
//     var hearPoison: Hearing? = null
//     var hearToy: Hearing? = null
//     var hearFish: Hearing? = null
//     var hearHazard: Hearing? = null
//     var hearFlower: Hearing? = null
//     var hearMouse: Hearing? = null
//     var hearWait: Hearing? = null
//     var hearLeft: Hearing? = null
//     var hearRight: Hearing? = null
//     var hearForward: Hearing? = null
//     var hearBackward: Hearing? = null
//     var hearSleep: Hearing? = null
//     var hearApproach: Hearing? = null
//     var hearIngest: Hearing? = null
//     var hearLook: Hearing? = null
//     var hearSmell: Hearing? = null
//     var hearAttack: Hearing? = null
//     var hearPlay: Hearing? = null
//     var hearMate: Hearing? = null
//
//     // TODO: Add gender
//     init {
//         initAgent()
//
//         this.brain = CreaturesBrain(net, parentSim)
//         with(net!!.network) {
//             initDefaultBrain()
//         }
//
//         this.biochem = CreaturesBiochem()
//         this.genome = CreaturesGenome()
//
//         // Temp code to illustrate how you might use genetics
//         println(genome.getGene("AnimalType"))
//         if (genome.getGene("AnimalType").getAllele() == "cow") {
//             println("It's a cow!")
//         }
//
//         initCouplings()
//
//         // TEMP
//         initChemDashboard()
//     }
//
//     /**
//      * Update the various components of the creature.
//      */
//     fun update() {
//         brain.network.update()
//
//         val approachActivation = decisions.getNeuronByLabel("Approach")!!.activation
//         if (approachActivation > 0) {
//             approachBehavior(approachActivation)
//         }
//
//         val speakActivation = decisions.getNeuronByLabel("Speak")!!.activation
//         if (speakActivation > 0) {
//             speakBehavior(speakActivation)
//         }
//
//         biochem.update()
//     }
//
//
//     /**
//      * Configure the agent's name, smells, sensors, and effectors.
//      */
//     private fun initAgent() {
//         agent.name = name
//         agent.id = "Mouse"
//         agent.smellSource = SmellSource(doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0)) // TODO: Still used?
//
//         agent.addEffector(StraightMovement())
//         agent.addEffector(Turning(Turning.LEFT))
//         agent.addEffector(Turning(Turning.RIGHT))
//
//         // Add object sensors (Todo)
//         cheeseSensor = ObjectSensor(EntityType.SWISS)
//         poisonSensor = ObjectSensor(EntityType.POISON)
//         toySensor = ObjectSensor(EntityType.BELL)
//         fishSensor = ObjectSensor(EntityType.FISH)
//         // hazardSensor = new ObjectSensor(EntityType.???);
//         flowerSensor = ObjectSensor(EntityType.PANSY)
//         mouseSensor = ObjectSensor(EntityType.MOUSE)
//         agent.addSensor(cheeseSensor!!)
//         agent.addSensor(poisonSensor!!)
//         agent.addSensor(toySensor!!)
//         agent.addSensor(fishSensor!!)
//         // agent.addSensor(hazardSensor);
//         agent.addSensor(flowerSensor!!)
//         agent.addSensor(mouseSensor!!)
//
//         // Add hearing sensors
//         hearCheese = Hearing("Cheese", 10.0)
//         hearPoison = Hearing("Poison", 10.0)
//         hearToy = Hearing("Toy", 10.0)
//         hearFish = Hearing("Fish", 10.0)
//         hearHazard = Hearing("Hazard", 10.0)
//         hearFlower = Hearing("Flower", 10.0)
//         hearMouse = Hearing("Mouse", 10.0)
//         hearWait = Hearing("Wait", 10.0)
//         hearLeft = Hearing("Left", 10.0)
//         hearRight = Hearing("Right", 10.0)
//         hearForward = Hearing("Forward", 10.0)
//         hearBackward = Hearing("Backward", 10.0)
//         hearSleep = Hearing("Sleep", 10.0)
//         hearApproach = Hearing("Approach", 10.0)
//         hearIngest = Hearing("Ingest", 10.0)
//         hearLook = Hearing("Look", 10.0)
//         hearSmell = Hearing("Smell", 10.0)
//         hearAttack = Hearing("Attack", 10.0)
//         hearPlay = Hearing("Play", 10.0)
//         hearMate = Hearing("Mate", 10.0)
//         agent.addSensor(hearCheese!!)
//         agent.addSensor(hearPoison!!)
//         agent.addSensor(hearToy!!)
//         agent.addSensor(hearFish!!)
//         agent.addSensor(hearHazard!!)
//         agent.addSensor(hearFlower!!)
//         agent.addSensor(hearMouse!!)
//         agent.addSensor(hearWait!!)
//         agent.addSensor(hearLeft!!)
//         agent.addSensor(hearRight!!)
//         agent.addSensor(hearForward!!)
//         agent.addSensor(hearBackward!!)
//         agent.addSensor(hearSleep!!)
//         agent.addSensor(hearApproach!!)
//         agent.addSensor(hearIngest!!)
//         agent.addSensor(hearLook!!)
//         agent.addSensor(hearSmell!!)
//         agent.addSensor(hearAttack!!)
//         agent.addSensor(hearPlay!!)
//         agent.addSensor(hearMate!!)
//
//         // Add speech effectors
//         agent.addEffector(Speech("Toy", 1.0))
//         agent.addEffector(Speech("Fish", 1.0))
//         agent.addEffector(Speech("Cheese", 1.0))
//         agent.addEffector(Speech("Poison", 1.0))
//         agent.addEffector(Speech("Hazard", 1.0))
//         agent.addEffector(Speech("Flower", 1.0))
//         agent.addEffector(Speech("Mouse", 1.0))
//     }
//
//     /**
//      * Build a brain network from a template.
//      */
//     context(Network)
//     private fun initDefaultBrain() {
//         // Init non-mutable lobes #1-5
//
//         drives = brain.buildDriveLobe()
//         stimulus = brain.buildStimulusLobe()
//         verbs = brain.buildVerbLobe()
//         nouns = brain.buildNounLobe()
//         senses = brain.buildSensesLobe()
//
//         // Init Lobe #6: Decisions
//         // TODO: Make this a WTA lobe.
//         decisions = brain.createLobe(615.21, 1002.02, verbs.size(), "line", "Decisions Lobe")
//         // decisions = brain.createWTALobe(538.67, 922.84, verbs.size(), "vertical
//         // line", "Lobe #6: Decisions");
//         brain.copyLabels(verbs, decisions)
//
//         // Coupling some decision lobe cells to agent effectors
//         parentSim.sim.couple(decisions.getNeuronByLabel("Left"), agent.getEffector("Turn left"))
//         parentSim.sim.couple(decisions.getNeuronByLabel("Right"), agent.getEffector("Turn right"))
//         parentSim.sim.couple(decisions.getNeuronByLabel("Forward"), agent.getEffector("Move straight"))
//
//         // TODO: Is there a way to couple the "Backward" node to the Go-straight
//         // effector in such a way that the inverse of the Backward node's activation is
//         // what the Go-straight effector gets?
//
//         // Init Lobe #7: Attention
//         // TODO: Make this a WTA lobe.
//         attention = brain.createLobe(440.55, 872.98, stimulus.size(), "vertical line", "Attention Lobe")
//         brain.copyLabels(stimulus, attention)
//
//         // Init Attention System Dendrite Pathways
//         /*
//          * TODO: Connecting neurons w/ OneToOne is not hooking up the right neurons
//          * together. Doing this manually for now. Would prefer to handle this in a
//          * synapse group in the future.
//          */
//         // STIMULUS-TO-ATTENTION
//         connect(stimulus.getNeuronByLabel("Toy"), attention.getNeuronByLabel("Toy"), CreaturesSynapseRule(), 1.0)
//         connect(stimulus.getNeuronByLabel("Fish"), attention.getNeuronByLabel("Fish"), CreaturesSynapseRule(), 1.0)
//         connect(stimulus.getNeuronByLabel("Cheese"), attention.getNeuronByLabel("Cheese"), CreaturesSynapseRule(), 1.0)
//         connect(stimulus.getNeuronByLabel("Poison"), attention.getNeuronByLabel("Poison"), CreaturesSynapseRule(), 1.0)
//         connect(stimulus.getNeuronByLabel("Hazard"), attention.getNeuronByLabel("Hazard"), CreaturesSynapseRule(), 1.0)
//         connect(stimulus.getNeuronByLabel("Flower"), attention.getNeuronByLabel("Flower"), CreaturesSynapseRule(), 1.0)
//         connect(stimulus.getNeuronByLabel("Mouse"), attention.getNeuronByLabel("Mouse"), CreaturesSynapseRule(), 1.0)
//
//         // NOUNS-TO-ATTENTION
//         connect(nouns.getNeuronByLabel("Toy"), attention.getNeuronByLabel("Toy"), CreaturesSynapseRule(), 1.0)
//         connect(nouns.getNeuronByLabel("Fish"), attention.getNeuronByLabel("Fish"), CreaturesSynapseRule(), 1.0)
//         connect(nouns.getNeuronByLabel("Cheese"), attention.getNeuronByLabel("Cheese"), CreaturesSynapseRule(), 1.0)
//         connect(nouns.getNeuronByLabel("Poison"), attention.getNeuronByLabel("Poison"), CreaturesSynapseRule(), 1.0)
//         connect(nouns.getNeuronByLabel("Hazard"), attention.getNeuronByLabel("Hazard"), CreaturesSynapseRule(), 1.0)
//         connect(nouns.getNeuronByLabel("Flower"), attention.getNeuronByLabel("Flower"), CreaturesSynapseRule(), 1.0)
//         connect(nouns.getNeuronByLabel("Mouse"), attention.getNeuronByLabel("Mouse"), CreaturesSynapseRule(), 1.0)
//
//         //		// Init Lobe #8: Concepts
//         //		NeuronGroup concepts = brain.createLobe(1086.94, -21.61, 640, "grid", "Lobe #8: Concepts");
//         //		brain.setLobeColumns(concepts, 20);
//         //
//         //		// Init Concepts to Decisions Dendrite Pathways
//         //		brain.createSynapseGroup(concepts, decisions, "#8 to #6, Type 0");
//         //		// TODO: BUG: This second synapse group sometimes has a graphical glitch
//         //		// (Arrow points to space).
//         //		brain.createSynapseGroup(concepts, decisions, "#8 to #6, Type 1");
//
//         // Init Lobe #0: Perception
//         perception = brain.buildPerceptionLobe(arrayOf(drives, verbs, senses, attention))
//
//         //		// Init Perception to Concept Dendrite Pathway
//         //		brain.createSynapseGroup(perception, concepts, "#0 to #8, Type 0");
//
//         // TODO: TEMP -- Remove this when unneeded.
//         tempCircuits()
//     }
//
//     context(Network)
//     private fun tempCircuits() {
//         val n = 0.5 // For setting a "default" positive or negative value
//         val h = 1.0 // For setting a high positive or negative value, for wherever we want a very strong association
//
//         // Pain
//         connect(
//             perception!!.getNeuronByLabel("Pain"),
//             decisions!!.getNeuronByLabel("Wait"),
//             CreaturesSynapseRule(), -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Pain"),
//             decisions!!.getNeuronByLabel("Sleep"),
//             CreaturesSynapseRule(), -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Pain"),
//             decisions!!.getNeuronByLabel(
//                 "Approach"
//             ), CreaturesSynapseRule(), -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Pain"),
//             decisions!!.getNeuronByLabel("Attack"), CreaturesSynapseRule(), n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Pain"),
//             decisions!!.getNeuronByLabel("Play"),
//             CreaturesSynapseRule(), -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Pain"),
//             decisions!!.getNeuronByLabel("Speak"),
//             CreaturesSynapseRule(), n
//         )
//
//         // (Need for) Comfort
//         connect(
//             perception!!.getNeuronByLabel("Comfort"),
//             decisions!!.getNeuronByLabel(
//                 "Wait"
//             ), CreaturesSynapseRule(), n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Comfort"),
//             decisions!!.getNeuronByLabel(
//                 "Sleep"
//             ), CreaturesSynapseRule(), -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Comfort"),
//             decisions!!.getNeuronByLabel(
//                 "Ingest"
//             ), CreaturesSynapseRule(), n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Comfort"),
//             decisions!!.getNeuronByLabel(
//                 "Look"
//             ), CreaturesSynapseRule(), n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Comfort"),
//             decisions!!.getNeuronByLabel(
//                 "Smell"
//             ), CreaturesSynapseRule(), n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Comfort"),
//             decisions!!.getNeuronByLabel(
//                 "Attack"
//             ), CreaturesSynapseRule(), -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Comfort"),
//             decisions!!.getNeuronByLabel(
//                 "Play"
//             ), CreaturesSynapseRule(), n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Comfort"),
//             decisions!!.getNeuronByLabel(
//                 "Speak"
//             ), CreaturesSynapseRule(), n
//         )
//
//         // Hunger
//         connect(
//             perception!!.getNeuronByLabel("Hunger"),
//             decisions!!.getNeuronByLabel("Sleep"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Hunger"),
//             decisions!!.getNeuronByLabel("Approach"),
//             CreaturesSynapseRule(),
//             n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Hunger"),
//             decisions!!.getNeuronByLabel("Ingest"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Hunger"),
//             decisions!!.getNeuronByLabel("Smell"),
//             CreaturesSynapseRule(),
//             n
//         )
//
//         // Temperature
//         /*
//          * Temp would supposedly work such that too high would be too hot and too low
//          * would be too cold, but that's not quite feasible as we've set this. Maybe we
//          * should split this drive into hotness/coldness like it is in Creatures?
//          */
//         connect(
//             perception!!.getNeuronByLabel("Temperature"),
//             decisions!!.getNeuronByLabel("Wait"),
//             CreaturesSynapseRule(),
//             -1.0
//         )
//
//         // Fatigue
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Wait"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Left"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Right"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Forward"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Backward"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Sleep"),
//             CreaturesSynapseRule(),
//             n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Approach"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Attack"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Play"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Mate"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Fatigue"),
//             decisions!!.getNeuronByLabel("Speak"),
//             CreaturesSynapseRule(),
//             -n
//         )
//
//         // Drowsiness
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Wait"),
//             CreaturesSynapseRule(),
//             n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Left"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Right"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Forward"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Backward"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Sleep"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Approach"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Ingest"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Look"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Attack"),
//             CreaturesSynapseRule(),
//             -h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Play"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Mate"),
//             CreaturesSynapseRule(),
//             -n
//         )
//         connect(
//             perception!!.getNeuronByLabel("Drowsiness"),
//             decisions!!.getNeuronByLabel("Speak"),
//             CreaturesSynapseRule(),
//             -n
//         )
//
//         // Mouse
//         connect(
//             perception!!.getNeuronByLabel("Mouse"),
//             decisions!!.getNeuronByLabel("Speak"),
//             CreaturesSynapseRule(),
//             n
//         )
//
//         // Verbs
//         connect(perception!!.getNeuronByLabel("Wait"), decisions!!.getNeuronByLabel("Wait"), CreaturesSynapseRule(), h)
//         connect(perception!!.getNeuronByLabel("Left"), decisions!!.getNeuronByLabel("Left"), CreaturesSynapseRule(), h)
//         connect(
//             perception!!.getNeuronByLabel("Right"),
//             decisions!!.getNeuronByLabel("Right"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Forward"),
//             decisions!!.getNeuronByLabel("Forward"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Backward"),
//             decisions!!.getNeuronByLabel("Backward"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Sleep"),
//             decisions!!.getNeuronByLabel("Sleep"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Approach"),
//             decisions!!.getNeuronByLabel("Approach"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Ingest"),
//             decisions!!.getNeuronByLabel("Ingest"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(perception!!.getNeuronByLabel("Look"), decisions!!.getNeuronByLabel("Look"), CreaturesSynapseRule(), h)
//         connect(
//             perception!!.getNeuronByLabel("Smell"),
//             decisions!!.getNeuronByLabel("Smell"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(
//             perception!!.getNeuronByLabel("Attack"),
//             decisions!!.getNeuronByLabel("Attack"),
//             CreaturesSynapseRule(),
//             h
//         )
//         connect(perception!!.getNeuronByLabel("Play"), decisions!!.getNeuronByLabel("Play"), CreaturesSynapseRule(), h)
//         connect(perception!!.getNeuronByLabel("Mate"), decisions!!.getNeuronByLabel("Mate"), CreaturesSynapseRule(), h)
//
//         // brain.getNetworkComponent().connect(perception.getNeuronByLabel(label), decisions.getNeuronByLabel(label), new CreaturesSynapseRule(), value);
//     }
//
//     /**
//      * Creates all essential couplings for the agent between various agent
//      * components.
//      */
//     fun initCouplings() {
//         // Couplings from center smell sensor to stimulus lobe
//
//         parentSim.sim.couple(cheeseSensor, stimulus!!.getNeuronByLabel("Cheese"))
//         parentSim.sim.couple(poisonSensor, stimulus!!.getNeuronByLabel("Poison"))
//         parentSim.sim.couple(toySensor, stimulus!!.getNeuronByLabel("Toy"))
//         parentSim.sim.couple(fishSensor, stimulus!!.getNeuronByLabel("Fish"))
//         // parentSim.getSim().couple(hazardSensor, stimulus.getNeuronByLabel("Hazard"));
//         parentSim.sim.couple(flowerSensor, stimulus!!.getNeuronByLabel("Flower"))
//         parentSim.sim.couple(mouseSensor, stimulus!!.getNeuronByLabel("Mouse"))
//
//         // Couplings from hearing sensors to noun lobe
//         parentSim.sim.couple(hearCheese, nouns!!.getNeuronByLabel("Cheese"))
//         parentSim.sim.couple(hearPoison, nouns!!.getNeuronByLabel("Poison"))
//         parentSim.sim.couple(hearToy, nouns!!.getNeuronByLabel("Toy"))
//         parentSim.sim.couple(hearFish, nouns!!.getNeuronByLabel("Fish"))
//         parentSim.sim.couple(hearHazard, nouns!!.getNeuronByLabel("Hazard"))
//         parentSim.sim.couple(hearFlower, nouns!!.getNeuronByLabel("Flower"))
//         parentSim.sim.couple(hearMouse, nouns!!.getNeuronByLabel("Mouse"))
//
//         // Couplings from hearing sensors to verb lobe
//         parentSim.sim.couple(hearWait, verbs!!.getNeuronByLabel("Wait"))
//         parentSim.sim.couple(hearLeft, verbs!!.getNeuronByLabel("Left"))
//         parentSim.sim.couple(hearRight, verbs!!.getNeuronByLabel("Right"))
//         parentSim.sim.couple(hearForward, verbs!!.getNeuronByLabel("Forward"))
//         parentSim.sim.couple(hearBackward, verbs!!.getNeuronByLabel("Backward"))
//         parentSim.sim.couple(hearSleep, verbs!!.getNeuronByLabel("Sleep"))
//         parentSim.sim.couple(hearApproach, verbs!!.getNeuronByLabel("Approach"))
//         parentSim.sim.couple(hearIngest, verbs!!.getNeuronByLabel("Ingest"))
//         parentSim.sim.couple(hearLook, verbs!!.getNeuronByLabel("Look"))
//         parentSim.sim.couple(hearSmell, verbs!!.getNeuronByLabel("Smell"))
//         parentSim.sim.couple(hearAttack, verbs!!.getNeuronByLabel("Attack"))
//         parentSim.sim.couple(hearPlay, verbs!!.getNeuronByLabel("Play"))
//         parentSim.sim.couple(hearMate, verbs!!.getNeuronByLabel("Mate"))
//
//         // Couplings from biochemistry to the brain
//         couple(biochem.getChemByIndex(1), drives!!.getNeuronByLabel("Pain"))
//         couple(biochem.getChemByIndex(2), drives!!.getNeuronByLabel("Comfort"))
//         couple(biochem.getChemByIndex(3), drives!!.getNeuronByLabel("Hunger"))
//         couple(biochem.getChemByIndex(4), drives!!.getNeuronByLabel("Temperature"))
//         couple(biochem.getChemByIndex(5), drives!!.getNeuronByLabel("Fatigue"))
//         couple(biochem.getChemByIndex(6), drives!!.getNeuronByLabel("Drowsiness"))
//         couple(biochem.getChemByIndex(7), drives!!.getNeuronByLabel("Lonliness"))
//         couple(biochem.getChemByIndex(8), drives!!.getNeuronByLabel("Crowdedness"))
//         couple(biochem.getChemByIndex(9), drives!!.getNeuronByLabel("Fear"))
//         couple(biochem.getChemByIndex(10), drives!!.getNeuronByLabel("Boredom"))
//         couple(biochem.getChemByIndex(11), drives!!.getNeuronByLabel("Anger"))
//         couple(biochem.getChemByIndex(12), drives!!.getNeuronByLabel("Arousal"))
//     }
//
//     private fun couple(chem: CreaturesChem, neuron: Neuron?) {
//         val chemicalAmount = parentSim.sim.getProducer(chem, "getAmount")
//         val chemReceptor = parentSim.sim.getConsumer(neuron, "forceSetActivation")
//         parentSim.sim.couple(chemicalAmount, chemReceptor)
//     }
//
//     fun approachBehavior(strength: Double) {
//         val objOfAttention = findObjectOfAttention()
//
//         // Follow that object
//         approachObject(objOfAttention, strength)
//     }
//
//     /**
//      * Find an object in the world that fits the most active category
//      *
//      * @return
//      */
//     // TODO: Change this to discriminate between multiple objects of the same
//     // category, such that the closest object is picked.
//     private fun findObjectOfAttention(): OdorWorldEntity {
//         // The replaceAll method must be called here to remove a whitespace added at the
//         // end when getMostActiveNeuron returns.
//         val objOfAttentionName = attention!!.mostActiveNeuron.replace("\\s".toRegex(), "")
//         val objOfAttention = agent.parentWorld.getEntity(objOfAttentionName)
//
//         return objOfAttention
//     }
//
//     fun approachObject(targetObject: OdorWorldEntity?, motionAmount: Double) {
//         if (targetObject == null) {
//             System.err.println("Null pointer on target object")
//             return
//         }
//
//         // Calculate the target heading for the agent
//         //        double delta_x = agent.getCenterX() - targetObject.getCenterX();
//         //        double delta_y = agent.getCenterY() - targetObject.getCenterY();
//         //        double targetHeading = Math.toDegrees(Math.atan2(delta_y, delta_x));
//         //        targetHeading = ((targetHeading < 0) ? targetHeading + 360 : targetHeading);
//
//         // System.out.println(targetHeading + "," + agent.getHeading());
//
//         // Update position and heading
//         // TODO :Heading update feels unnatural. Maybe use braitenberg method instead,
//         // or else improve this
//         val stepSize = baseMovementStepSize * motionAmount
//
//         //        double newX = agent.getCenterX() + stepSize * (targetObject.getX() - agent.getCenterX());
//         //        double newY = agent.getCenterY() + stepSize * (targetObject.getY() - agent.getCenterY());
//         //        double newHeading = agent.getHeading() + .1 * (targetHeading - agent.getHeading());
//         //        agent.setCenterLocation((float) newX, (float) newY);
//         //        agent.setHeading(newHeading);
//     }
//
//     fun speakBehavior(speakActivation: Double) {
//         // TODO: Make the agent also speak the name of the decision neuron with either
//         // the second highest activation (after the speak neuron itself) OR the decision
//         // neuron with the highest activation in the next simulation iteration
//
//         // Get the noun to speak, and the effector that goes with it
//
//         val noun = attention!!.mostActiveNeuron.replace("\\s".toRegex(), "")
//         if (noun.isEmpty()) {
//             return
//         }
//         val effectorName = "Say: \"$noun\""
//         val effector = agent.getEffector(effectorName) as Speech
//
//         // If the speak activation is above 1, the agent will say the noun.
//         if (effector != null) {
//             effector.setAmount(speakActivation)
//         } else {
//             System.err.println("Could not find effector:$effectorName")
//         }
//     }
//
//     // TODO
//     fun sleepBehavior() {
//         /*
//          * This behavior needs to do 2 things: <ul> <li>1. Paralyze the creature
//          * temporarily, such that it won't be able to do anything even if their brain is
//          * thinking of it.</li> <li>2. Send some sort of "sleeping" signal to any
//          * chemical emitters or stimulus genes that need it.</li> </ul>
//          *
//          * We could probably get both done at once by just having some boolean isAsleep
//          * variable on the Creature level
//          */
//     }
//
//     fun ingestBehavior() {
//         val objOfAttention = findObjectOfAttention()
//
//         // TODO: Perform a check (using center smell sensor) to make sure the object is
//         // close enough to eat. If true, then:
//         // ingestObject(objOfAttention)
//     }
//
//     fun ingestObject(targetObject: OdorWorldEntity?) {
//         // Based off the object's name, ID, or smell, add some concoction of chemicals
//         // to the creature's biochemistry (if applicable).
//         // TODO: Implement this once sufficent biochemistry is implemented
//     }
//
//     fun lookBehavior() {
//         val objOfAttention = findObjectOfAttention()
//
//         // Based off the object's name, ID, or smell, add some concoction of chemicals
//         // to the creature's biochemistry (if applicable).
//         // TODO: Implement this once sufficent biochemistry is implemented
//     }
//
//     fun smellBehavior() {
//         val objOfAttention = findObjectOfAttention()
//
//         // Based off the object's name, ID, or smell, add some concoction of chemicals
//         // to the creature's biochemistry (if applicable). Use the strength of the smell
//         // to determine the dosage.
//         // TODO: Implement this once sufficent biochemistry is implemented
//     }
//
//     fun attackBehavior() {
//         val objOfAttention = findObjectOfAttention()
//
//         // TODO: Perform a check (using center smell sensor) to make sure the object is
//         // close enough to attack. If true, then:
//         // attackObject(objOfAttention);
//     }
//
//     fun attackObject(targetObject: OdorWorldEntity?) {
//         // If the attacked object is not another creature, don't do anything (unless
//         // it's a hazard or tough objects like a candle or bell, in which case add some
//         // pain and other related chemicals)
//         // If the attacked object IS a creature, then we'll need to add the relevenat
//         // chemicals and stimulus to the target creature.
//     }
//
//     fun playBehavior() {
//         val objOfAttention = findObjectOfAttention()
//
//         // TODO: Perform a check (using center smell sensor) to make sure the object is
//         // close enough to play with. If true, then:
//         // playObject(objOfAttention);
//     }
//
//     fun playObject(targetObject: OdorWorldEntity?) {
//     }
//
//     fun mateBehavior() {
//     }
//
//     fun getName(): String {
//         return (name)
//     }
//
//     fun setName(name: String) {
//         this.name = name
//     }
//
//     fun getBrain(): CreaturesBrain {
//         return (brain)
//     }
//
//     fun getAgent(): OdorWorldEntity {
//         return (agent)
//     }
//
//     // Was not here before last pull
//     fun setAgentLocation(x: Float, y: Float) {
//         //        agent.setCenterLocation(x, y);
//     }
//
//     // Was not here before last pull
//     fun setAgentSkin(type: String?) {
//         // TODO: Set entity type
//         //        agent.setEntityType(type);
//     }
//
//     // Was not here before last pull
//     fun deleteLobe(lobe: NeuronGroup) {
//         lobe.delete()
//     }
//
//     // Was not here before last pull
//     fun injectChem(name: String?, dose: Double) {
//         biochem.getChemByName(name).incrementAmount(dose)
//
//         // Needles hurt!
//         biochem.getChemByName("Pain").incrementAmount(1.0)
//     }
//
//     // TEMP
//     fun initChemDashboard() {
//         val dash = brain.createLobe(558.45, 1122.53, 2, "grid", "Chem Dashboard")
//         dash.getNeuron(0)!!.label = "Endorphin"
//         dash.getNeuron(1)!!.label = "Reward"
//
//         couple(biochem.getChemByName("Endorphin"), dash.getNeuron(0))
//         couple(biochem.getChemByName("Reward"), dash.getNeuron(1))
//     }
//
//     context(Network)
//     fun connect(source: Neuron?, target: Neuron?, rule: SynapseUpdateRule<*, *>?, value: Double): Synapse {
//         val synapse = Synapse(source, target, rule)
//         synapse.forceSetStrength(value)
//         addNetworkModelAsync(synapse)
//         return (synapse)
//     }
// }
