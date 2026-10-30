/**
 * Demo of SOM network learning to classify smells from flowers and cheeses.
 *
 * The mouse agent moves around an environment with different objects (cheeses and flowers),
 * each emitting a unique 9-dimensional smell vector. The SOM network learns to organize
 * these smells, with similar smells (e.g., different cheeses) activating nearby neurons.
 *
 * Smells sensed while the mouse is close to an object are averaged into a prototype for that object, and
 * [PrototypeLabeler] labels each SOM neuron with the objects whose prototypes it currently wins.
 */
package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor
import kotlin.math.atan2

val somNetSmells = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("SOM Network")
    val network = networkComponent.network

    val somNet = SOMNetwork(9, 16)
    network.addNetworkModel(somNet)
    somNet.applySimulationLayout(somNeuronInterval = SOM_SMELLS_SOM_INTERVAL)
    somNet.inputLayer.apply {
        setUpperBound(1.0)
        isClamped = false
    }

    somNet.initialLearningRate = 0.06
    somNet.initNeighborhoodSize = 100.0
    // The neighborhood must still be wide while most of the learning happens, or the cheese and flower regions
    // break up. With these rates the learning rate reaches 0.001 after about 6000 updates while the neighborhood
    // is still about 60, long enough to steer the mouse through the game described in the sidebar.
    somNet.learningDecayRate = 0.0007
    somNet.neighborhoodDecayAmount = 0.007
    // Start weights in the same range as the clipped smell inputs; Reset uses the same range
    val initialWeights = UniformRealDistribution(0.0, 1.0)
    somNet.randomize(initialWeights)
    somNet.reset()

    somNet.modelList.get<SynapseGroup>().first().displaySynapses = false

    val labeler = PrototypeLabeler(somNet.som.neuronList, somNet::winnerFor)

    val oc = addOdorWorldComponent("Flowers and Cheeses")
    val world = oc.world

    val mouse = world.addEntity(140, 205, EntityType.Mouse)

    val smellCenter = SmellSensor("Smell-Center", theta = 0.0, radius = 0.0).apply {
        smellVector = DoubleArray(9)
    }
    mouse.addSensor(smellCenter)

    mouse.addEffector(StraightMovement())
    mouse.addEffector(Turning(1.0)) // Left turn
    mouse.addEffector(Turning(-1.0)) // Right turn

    // Add cheeses with their smell sources (extracted from original XML)
    val swiss = world.addEntity(36, 107, EntityType.Swiss)
    swiss.smellSource = SmellSource(doubleArrayOf(
        0.2843023824819105, 0.7969005568449742, -0.1869959701309641,
        -0.03930965224307115, 0.053630097604160105, 0.2719959853127682,
        -0.11784800055216271, -0.017583042347626886, -0.18146609193569982
    )).apply {
        decayFunction = GaussianDecayFunction()
    }

    val gouda = world.addEntity(169, 32, EntityType.Gouda)
    gouda.smellSource = SmellSource(doubleArrayOf(
        0.15685293544672108, 1.221526823511768, -0.04346913373941728,
        0.003500259993022259, 0.22820144543044077, -0.05772506145969827,
        0.007412608834111722, 0.16480447914896104, 0.005611294738952099
    )).apply {
        decayFunction = GaussianDecayFunction()
    }

    val blueCheese = world.addEntity(284, 90, EntityType.BlueCheese)
    blueCheese.smellSource = SmellSource(doubleArrayOf(
        0.1843023824819105, 0.8969005568449742, -0.0869959701309641,
        -0.03930965224307115, 0.053630097604160105, 0.1719959853127682,
        -0.21784800055216272, -0.027583042347626888, -0.2814660919356998
    )).apply {
        decayFunction = GaussianDecayFunction()
    }

    val flax = world.addEntity(315, 349, EntityType.Flax)
    flax.smellSource = SmellSource(doubleArrayOf(
        -0.010560182705044729, 0.026571523931577266, -0.020953978263698426,
        0.11038351834856239, 0.2004498158179706, 0.2554258614582945,
        -0.035022510283423534, 1.0522166340913646, -0.21703143721480345
    )).apply {
        decayFunction = GaussianDecayFunction()
    }

    val tulip = world.addEntity(55, 349, EntityType.Tulip)
    tulip.smellSource = SmellSource(doubleArrayOf(
        -0.13098862375238882, 0.27927051862860713, 0.2580699935112152,
        -0.15164347434053907, -0.03638494874066445, -0.02723936667295006,
        0.0769829931118381, 1.2672660699668596, -0.16995842810080472
    )).apply {
        decayFunction = GaussianDecayFunction()
    }

    val pansy = world.addEntity(198, 385, EntityType.Pansy)
    pansy.smellSource = SmellSource(doubleArrayOf(
        -0.14163903550434465, -0.11730551337793613, 0.16291558437323667,
        0.041681576470888305, 0.025233570401022785, -0.14725712846433012,
        0.22536179032743064, 0.7027610174415445, 0.03595542268231242
    )).apply {
        decayFunction = GaussianDecayFunction()
    }

    couplingManager.createCoupling(smellCenter, somNet.inputLayer)

    val objectsWithLabels = listOf(
        swiss to "Swiss",
        gouda to "Gouda",
        blueCheese to "Blue",
        flax to "Flax",
        tulip to "Tulip",
        pansy to "Pansy"
    )

    val labelRadius = 30.0

    // The smell the network sees on an update was sensed where the mouse stood on the previous update, so the object
    // the mouse was near is remembered for one update before the sensed input is credited to it.
    var objectNearPreviously: String? = null

    workspace.addUpdateAction("Label SOM nodes") {
        objectNearPreviously?.let { labeler.addSample(it, somNet.inputLayer.activationArray) }
        labeler.refresh()
        objectNearPreviously = objectsWithLabels
            .firstOrNull { (entity, _) -> mouse.location.distance(entity.location) < labelRadius }
            ?.second
    }

    // Auto Tour walks the mouse straight through one object after another, in random order. Passing through each
    // object once, rather than circling or rocking at it, is what separates cheeses from flowers most reliably.
    var touring = false
    val tourSpeed = 3.0
    var tourTarget = objectsWithLabels.random().first

    workspace.addUpdateAction("Auto Tour") {
        if (!touring) return@addUpdateAction
        val dx = tourTarget.location.x - mouse.location.x
        val dy = tourTarget.location.y - mouse.location.y
        val distance = mouse.location.distance(tourTarget.location)
        if (distance <= tourSpeed) {
            mouse.location = tourTarget.location
            tourTarget = objectsWithLabels.map { it.first }.filter { it !== tourTarget }.random()
        } else {
            mouse.heading = Math.toDegrees(atan2(-dy, dx))
            mouse.location = point(mouse.location.x + dx * tourSpeed / distance, mouse.location.y + dy * tourSpeed / distance)
        }
    }

    withGui {
        val controlPanel = createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {

            addButton("Auto Tour") {
                if (workspace.updater.isRunning) return@addButton
                touring = true
                workspace.iterateWhile { workspace.updater.isRunning && somNet.somLearningRate > 0.001 }
                touring = false
            }.toolTipText = "Walk the mouse through the objects in random order until the learning rate falls below " +
                "0.001, about 6000 updates from a fresh start. Press Stop to end early, or Reset first to start over."

            addSeparator()

            addButton("Reset") {
                somNet.randomize(initialWeights)
                somNet.reset()
                labeler.clear()
                network.events.updated.fire()
            }.toolTipText = "Randomize the SOM weights, restore the initial learning parameters, and clear the labels."

        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 450, 720)
        place(oc, controlPanel.rightEdgeWithGap() + 450 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 450, 520)
    }

    addSidebarInfo(
        """
        # SOM Network (Smell)

        Self-Organizing Map (SOM) networks learn to classify inputs in an unsupervised way,
        by making neurons respond to similar inputs. Neurons near each other end up representing
        similar inputs.

        # Simulation Details

        The mouse agent moves through an environment containing cheeses and flowers, each emitting
        a unique 9-dimensional smell vector. The SOM learns from whatever the mouse smells on every
        update, and over time organizes these smells spatially. Cheeses smell more like each other
        than like flowers, so a well-organized map puts the cheeses in one region and the flowers
        in another.

        Each update, the winning neuron and every neuron within the neighborhood move their weights
        toward the current smell. The learning rate (starting at `0.06`) and the neighborhood size
        (starting at `100` pixels) both shrink as the network trains, shown as `Learning rate` and
        `N-size` below the network.

        The overall layout is decided early, while the learning rate is high and the neighborhood
        still covers most of the map. If the mouse stays at one object during that time, the whole
        neighborhood is dragged toward that one smell and forgets the others, so the regions form
        wherever the mouse happened to be last. If the mouse keeps moving among all the objects,
        the map has to make room for all of them at once, and the regions form by similarity.

        The map also represents how strong a smell is. Smells weaken with distance, and the SOM
        compares raw smell vectors, so a weak `Swiss` smell is closer to a weak `Tulip` smell than
        to a strong `Swiss` smell. A trained map usually has three ends: the cheeses, the flowers,
        and an unlabeled region where the weak smells sensed far from every object land. The
        unlabeled neurons between an object's label and that region respond to the object at
        medium distances. With so many neurons spent on smell strength, similar objects, such as the three cheeses, often
        share a neuron.

        ## About the Labeling System

        Each object's label appears on exactly one neuron: the neuron that wins for that object's
        typical smell. This keeps the map readable, but it means a label does not show every
        neuron that responds to the object.

        Because the map represents smell strength, several neurons respond to the same object at
        different distances, and only one of them carries its label. So it is normal for the mouse
        to be near an object while the neuron that lights up is unlabeled, or labeled with a similar
        object. The label marks where the object's smell usually lands; the lit neuron shows where
        the current smell lands.

        The typical smell is the average of everything the mouse has sensed within radius `30` of
        the object. On every update the network is asked which neuron
        wins each typical smell under the current weights, and that neuron gets the label. Labels therefore move
        when learning moves an object's winner, and barely move once the learning rate is small.

        ## Control Panel Settings

        - `Auto Tour`: Walk the mouse straight through the objects one after another, in random order,
          until the learning rate falls below `0.001` (about `6000` updates from a fresh start), then
          stop. Press `Stop` to end early. Once the learning rate is below `0.001`, press `Reset`
          first to start over.
        - `Reset`: Randomize the SOM weights, restore the initial learning parameters, and clear the
          labels to start training over

        # What to Do

        1. Press `Auto Tour` to watch the mouse tour the objects by itself until the SOM has nearly
           finished learning. Or press `Run` and drag the mouse near different objects yourself.
        2. Each object's label appears once the mouse has been near it (see `About the Labeling System`).
        3. Watch the labels as the SOM organizes the smells, and watch `Learning rate` and `N-size`
           shrink below the network.

        ## As a Game

        The goal: get the three cheese labels (`Swiss`, `Gouda`, `Blue`) into one region of the map
        and the three flower labels (`Flax`, `Tulip`, `Pansy`) into another, with no neuron labeled
        with both a cheese and a flower. Objects of the same kind sharing a neuron is fine, and
        common: `Swiss` and `Blue` nearly always do, and often all three cheeses do.

        You only control where the mouse goes, and the clock is running: once the learning rate
        falls to about `0.001`, the map is nearly settled.

        1. Press `Reset`, then `Run`.
        2. Steer the mouse. Some tips:
           - Sweep through all six objects in turn, and keep sweeping for as long as the learning
             rate is above about `0.01`. The order matters little: a loop along the top row and
             back along the bottom row works as well as a random order
           - Mix cheeses and flowers throughout. Touring the cheeses for a while and then the
             flowers for a while is one of the less reliable ways to play
           - On each visit, pass straight through the object once and head for the next one.
             Circling it briefly also works, but rocking back and forth over it works least well
           - Drag the mouse through the open space between objects rather than lifting it, so the
             network smells the in-between mixtures too
           - Don't linger at one object or skip one, especially early on
        3. When the learning rate is near `0.001`, the map is effectively fixed. Visit each object
           once more to confirm the labels.
        4. To see why the strategy matters, play a sloppy round: press `Reset` and spend
           long stretches at one object at a time, in no particular order. Sloppy play sorts
           cheeses from flowers about half the time or less; sweeping through the objects one
           after another nearly always does.

        To see a good round first, press `Reset` and then `Auto Tour`: it plays with the recommended
        strategy. Then press `Reset` again and try to match it by hand.

        The map keeps adjusting after the first split, so a clean split occasionally blurs a little
        near the end.

        ## Things to Try

        - Right-click the SOM network's interaction box and choose its `Edit` item to change the learning
          parameters, then press `Reset` so new initial values take effect
        - Stop the simulation, select a SOM neuron, and right-click the SOM network's interaction box
          and choose `Recall SOM memory` to put the smell that neuron is tuned to on the input nodes
        - Position the mouse between objects to observe how the network responds to smell mixtures
        - After `Auto Tour` finishes, press `Run` and drag the mouse slowly in a straight line away
          from an object. The lit neuron walks across the map from the object's label toward the
          region where weak smells land
        - The `SOM network (animals)` simulation shows the same kind of map with labeled features and no
          movement to manage

        # References

        Kohonen, T. (1982). [_Self-organized formation of topologically correct feature maps_](https://doi.org/10.1007/BF00337288). _Biological Cybernetics_, _43_(1), 59-69.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        """.trimIndent()
    )
}
