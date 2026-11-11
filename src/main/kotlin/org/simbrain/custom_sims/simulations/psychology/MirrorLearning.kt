/**
 * Mirror responses learned from self-observation, with spiking neurons. When the child goes to an object and sees
 * herself doing it, visual populations that see someone going to the object become linked to motor populations that
 * perform the movement. Afterward, watching the adult make the same movement makes the child's motor populations fire.
 */
package org.simbrain.custom_sims.simulations.psychology

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.addSynapseAsync
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.learningrules.STDPRule
import org.simbrain.network.spikeresponders.JumpAndDecay
import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.util.place
import org.simbrain.util.toRadian
import org.simbrain.util.updateAction
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.behaviors.NoOpBehavior
import org.simbrain.world.odorworld.behaviors.Pursue
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import javax.swing.JInternalFrame
import kotlin.math.*
import kotlin.random.Random

val mirrorLearning = newSim("mirror_learning") {
    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Child's brain")
    val network = networkComponent.network
    network.timeStep = TIME_STEP

    suspend fun population(name: String, labels: List<String>, spacing: Double, x: Double, y: Double) =
        network.addNeuronCollection(labels.size) {
            updateRule = IzhikevichRule().apply { backgroundCurrent = 0.0 }
            activation = RESTING_POTENTIAL
        }.apply {
            label = name
            neuronList.forEachIndexed { i, neuron -> neuron.label = labels[i] }
            layout = LineLayout(spacing, LineLayout.LineOrientation.HORIZONTAL)
            applyLayout()
            setLocation(x, y)
        }

    val motorCandy = population(MOTOR_CANDY, MOTOR_PHASES, MOTOR_SPACING, 0.0, 0.0)
    val motorBell = population(MOTOR_BELL, MOTOR_PHASES, MOTOR_SPACING, POPULATION_GAP, 0.0)
    val visualCandy = population(VISUAL_CANDY, VISUAL_LABELS, VISUAL_SPACING, 0.0, LAYER_GAP)
    val visualBell = population(VISUAL_BELL, VISUAL_LABELS, VISUAL_SPACING, POPULATION_GAP, LAYER_GAP)

    for (visual in listOf(visualCandy, visualBell)) {
        for (motor in listOf(motorCandy, motorBell)) {
            for (source in visual.neuronList) {
                for (target in motor.neuronList) {
                    network.addSynapseAsync(source, target) {
                        strength = 0.0
                        lowerBound = 0.0
                        upperBound = MAX_WEIGHT
                        spikeResponder = JumpAndDecay()
                        learningRule = STDPRule().apply {
                            wMinus = 0.0
                            tauPlus = STDP_WINDOW
                            learningRate = LEARNING_RATE
                        }
                    }
                }
            }
        }
    }

    val odorWorldComponent = addOdorWorldComponent("Playroom")
    val world = odorWorldComponent.world.apply {
        isObjectsBlockMovement = false
        isUseCameraCentering = false
        wrapAround = false
        tileMap.updateMapSize(19, 14)
    }
    world.addEntity(CANDY_HOME.first, CANDY_HOME.second, EntityType.Candy).apply { name = CANDY }
    world.addEntity(BELL_HOME.first, BELL_HOME.second, EntityType.Bell).apply { name = BELL }
    world.addEntity(CHILD_HOME.first, CHILD_HOME.second, EntityType.Girl).apply {
        name = CHILD
        heading = CHILD_HEADING
    }
    world.addEntity(ADULT_HOME.first, ADULT_HOME.second, EntityType.Amy).apply {
        name = ADULT
        heading = ADULT_HEADING
    }

    val raster = RasterPlotComponent(RASTER)
    workspace.addWorkspaceComponent(raster)
    raster.model.apply {
        addDataSources(POPULATIONS.size - 1)
        POPULATIONS.forEachIndexed { index, name -> dataset.getSeries(index).key = name }
        setComponentNames(
            listOf(VISUAL_CANDY, MOTOR_CANDY, VISUAL_BELL, MOTOR_BELL).zip(
                listOf(VISUAL_LABELS, MOTOR_PHASES, VISUAL_LABELS, MOTOR_PHASES)
            ).flatMap { (population, labels) ->
                val prefix = population.substringBefore(":") + " " + population.substringAfterLast(" ")
                labels.mapIndexed { i, label -> "$prefix ${label.ifEmpty { "${i + 1}" }}" }
            }
        )
        windowSize = 400
    }

    addSidebarInfo(
        """
        # Mirror Neurons

        Seeing an action, done by yourself or someone else, can become connected to doing it. This simulation has
        seeing neurons and doing neurons, and you can watch the connections between them form.

        [Mirror neurons](https://en.wikipedia.org/wiki/Mirror_neuron) fire both when a monkey acts and when it
        watches another perform the same action. In humans, including children, the evidence comes from brain imaging
        and EEG, such as suppression of the mu rhythm while watching actions (Bowman et al., 2017). Where mirror
        neurons come from is debated. Some treat them as an adaptation for understanding actions
        ([Rizzolatti & Craighero, 2004](https://doi.org/10.1146/annurev.neuro.27.070203.144230)), others hold that
        they are learned by association ([Heyes, 2010](https://doi.org/10.1016/j.neubiorev.2009.11.007)), and many
        take a middle view. Strong evidence for learning is that practice can reverse them: after people practiced
        moving one finger while watching a different finger move, watching that finger activated the first
        ([Catmur et al., 2007](https://doi.org/10.1016/j.cub.2007.08.006)). This simulation illustrates the learning
        side.

        One source of the link is watching yourself act. When you do something and see yourself doing it, the sight
        and the movement become linked. Later, seeing someone else do it drives the same visual neurons, and through
        them the movement. This simulation models that case, with one big simplification: the child sees her own trip
        from her own point of view and the adult's from the outside, yet here the same visual neurons respond to
        both. For actions you cannot see yourself do, such as facial expressions, Heyes argues the link comes from
        being imitated by others (Ray & Heyes, 2011).

        The network is a child's brain, with four populations of spiking neurons:

        - `Visual: going to candy` and `Visual: going to bell` are seeing neurons. They fire when the child sees
          anyone going to that object, herself included. Each prefers a distance, from `far` to `near`.
        - `Motor: go to candy` and `Motor: go to bell` are doing neurons for the phases of a movement: `start`,
          `walk`, `slow down`, and `grab`. When one fires clearly while the child only watches, its label changes
          to `Mirror`.

        Every visual neuron connects to every motor neuron, starting at zero, so the child has to learn which views
        go with which phases. The lines between neurons show the weights as they grow, and a caption under each
        person shows the phase of the movement.

        # Simulation Details

        The neurons are Izhikevich spiking neurons. The weights learn with spike-timing-dependent plasticity (STDP),
        a widely used rule for spiking networks. A weight grows when a visual spike comes within about 10
        milliseconds before a motor spike. The part of STDP that weakens weights is turned off, so in effect weights
        grow when visual and motor neurons fire together. When the child goes and sees herself, far views become
        linked to `start` and close views to `grab`.

        Two further simplifications. Heyes stresses contingency, that seeing must predict doing, but this rule only
        responds to co-occurrence. And here seeing and doing happen at the same moment, while real visual feedback
        lags the movement, which Keysers and Gazzola (2014) argue makes mirror neurons predictive.

        ## Control Panel Settings

        - `Object` chooses where people go.
        - `Watch adult go` makes the adult go while the child watches.
        - `Go` makes the child go. Buttons do nothing while a trip is under way.
        - `Child can see self`: uncheck it to have the child act without seeing herself, as if in the dark.
        - `Show spikes` shows a raster plot of every neuron's spikes.
        - `Reset learning` sets the weights to zero and changes `Mirror` labels back to `Motor`.

        # Activities

        ## Activity 1: Watch the adult, act, then watch again

        1. Before the child does anything, press `Watch adult go`. The visual neurons fire from `far` to `near`, but
           the motor neurons stay silent. There are no mirror neurons yet. Check `Show spikes` to see this as a
           raster plot.
        2. Press `Go`, wait for the child to get home, and press `Go` again. The child both does and sees, so the
           `Motor: go to candy` and `Visual: going to candy` neurons fire together, and the weights between them
           grow.
        3. Press `Watch adult go` again. Now the motor neurons fire, from `start` to `grab`, while the child only
           watches. That is a mirror response, and the label changes to `Mirror: go to candy`. From now on, watching
           also strengthens the weights, because the visual spikes come just before motor spikes.

        ## Activity 2: Try a new object

        Set `Object` to `Bell` and press `Watch adult go`. There is no mirror response, because the child has
        never gone to the bell. Press `Go` once and watch again, and the bell neurons respond too.

        ## Activity 3: See single spikes

        To see single spikes, drag a box around the `Motor: go to candy` neurons, right-click one, and choose the
        time series plot. Press `Watch adult go`, and each spike shows as a sharp peak.

        ## Activity 4: Do without seeing

        Press `Reset learning` and uncheck `Child can see self`. Press `Go` as often as you like, then
        `Watch adult go`. There is no mirror response: doing without seeing teaches nothing. Check the box again
        when you are done.

        ## Activity 5: Change the learning rate (advanced)

        Click the network, press `W` to select every weight, and press Cmd-E (Ctrl-E on Windows and
        Linux). Set `Learning rate` to 0.1 (it starts at $LEARNING_RATE). Press `Reset learning`, press `Go`
        twice, then `Watch adult go`. The response is weak or absent and the label usually stays `Motor`. It takes more
        trips to get a clear response. Set the rate back to $LEARNING_RATE when you are done.

        # References

        Bowman, L. C., Thorpe, S. G., Cannon, E. N., & Fox, N. A. (2017). [_Action mechanisms for social cognition:
        Behavioral and neural correlates of developing theory of mind_](https://doi.org/10.1111/desc.12447).
        Developmental Science, 20(5), e12447.

        Heyes, C. (2010). [_Where do mirror neurons come from?_](https://doi.org/10.1016/j.neubiorev.2009.11.007)
        Neuroscience & Biobehavioral Reviews, 34(4), 575–583.

        Keysers, C., & Gazzola, V. (2014). [_Hebbian learning and predictive mirror neurons for actions, sensations
        and emotions_](https://doi.org/10.1098/rstb.2013.0175). Philosophical Transactions of the Royal Society B,
        369(1644), 20130175.

        Ray, E., & Heyes, C. (2011). [_Imitation in infancy: The wealth of the
        stimulus_](https://doi.org/10.1111/j.1467-7687.2010.00961.x). Developmental Science, 14(1), 92–105.
        """.trimIndent(),
        width = 330
    )

    setUpMirrorLearning(workspace)

}.registerReopenFunction { workspace -> setUpMirrorLearning(workspace) }

private const val MOTOR_CANDY = "Motor: go to candy"
private const val MOTOR_BELL = "Motor: go to bell"
private const val VISUAL_CANDY = "Visual: going to candy"
private const val VISUAL_BELL = "Visual: going to bell"
private const val CHILD = "Child"
private const val ADULT = "Adult"
private const val CANDY = "Candy"
private const val BELL = "Bell"
private const val RASTER = "Spikes in the child's brain"

/**
 * Populations in raster order from bottom to top, so each object's visual and motor rows are adjacent.
 */
private val POPULATIONS = listOf(VISUAL_CANDY, MOTOR_CANDY, VISUAL_BELL, MOTOR_BELL)

/**
 * Label of a motor population once it has fired while the child only watched, which makes it a mirror population.
 */
private fun mirrorLabel(motorLabel: String) = motorLabel.replace("Motor:", "Mirror:")

private val MOTOR_PHASES = listOf("start", "walk", "slow down", "grab")
private val VISUAL_LABELS = listOf("far", "", "", "", "", "near")

private val CANDY_HOME = 90 to 220
private val BELL_HOME = 518 to 220
private val CHILD_HOME = 304 to 360
private val ADULT_HOME = 304 to 80
private const val CHILD_HEADING = 90.0
private const val ADULT_HEADING = 270.0

private const val VISUAL_SPACING = 35.0
private const val MOTOR_SPACING = 55.0
private const val POPULATION_GAP = 300.0
private const val LAYER_GAP = 220.0

private const val TIME_STEP = 1.0
private const val RESTING_POTENTIAL = -65.0
private const val DRIVE_CURRENT = 28.0
private const val DRIVE_NOISE = 4.0
private const val MAX_WEIGHT = 25.0
private const val LEARNING_RATE = 0.4
private const val STDP_WINDOW = 10.0

/**
 * Motor spikes during one watch-only trip needed before a motor population is relabeled as a mirror population.
 */
private const val MIRROR_SPIKES = 5

/**
 * Preferred distances of the first and last visual neurons, and the width of their tuning curves.
 */
private const val FAR_DISTANCE = 240.0
private const val NEAR_DISTANCE = 50.0
private const val TUNING_WIDTH = 30.0

/**
 * Distances at which the child is in the start, walk, and slow down phases. The grab phase is the pause at the object.
 */
private val PHASE_DISTANCES = listOf(230.0, 165.0, 100.0)

/**
 * Captions shown beneath a person in each phase, in the order of [MOTOR_PHASES].
 */
private val PHASE_CAPTIONS = listOf("starting", "walking", "slowing down", "grabbing")

private const val REACH_SPEED = 2.0
private const val ARRIVAL_DISTANCE = 50.0
private const val HOLD_TICKS = 20
/**
 * Pause at home after each trip, longer than the STDP window, so spikes from one trip cannot pair with the next.
 */
private const val REST_TICKS = 60

/**
 * Cosine between an agent's heading and the direction to an object above which the agent counts as moving toward it.
 */
private const val AIM_THRESHOLD = 0.7

/**
 * Runs one trip at a time: each actor walks to the target, pauses there to grab it, and returns home. The trip ends
 * after every actor is home and a short rest has passed.
 */
class MirrorReaches(
    val child: OdorWorldEntity,
    val adult: OdorWorldEntity,
    val candy: OdorWorldEntity,
    val bell: OdorWorldEntity
) {
    private var actors: List<OdorWorldEntity> = emptyList()
    private var target: OdorWorldEntity? = null
    private val holdCounts = mutableMapOf<OdorWorldEntity, Int>()
    private var restCount = 0

    val isReaching get() = actors.isNotEmpty()

    val isChildActing get() = child in actors

    /**
     * Whether the child sees her own movement, so it drives the visual populations as another person's would.
     */
    var childSeesSelf = true

    /**
     * The object the child is currently going to or grabbing, or null when the child is not acting.
     */
    val childTarget get() = target.takeIf { child in actors && holdCounts[child] != DONE && restCount == 0 }

    fun isGrabbing(agent: OdorWorldEntity) = (holdCounts[agent] ?: 0) in 1..HOLD_TICKS

    /**
     * The phase of [agent]'s movement, named as in [PHASE_CAPTIONS], or null when it is not on its way to the target
     * or grabbing it.
     */
    fun phaseOf(agent: OdorWorldEntity): String? {
        val target = target ?: return null
        val hold = holdCounts[agent] ?: return null
        return when {
            restCount > 0 || hold == DONE -> null
            hold > 0 -> PHASE_CAPTIONS.last()
            else -> {
                val distance = agent.location.distance(target.location)
                PHASE_CAPTIONS[PHASE_DISTANCES.indices.minBy { abs(distance - PHASE_DISTANCES[it]) }]
            }
        }
    }

    fun start(actors: List<OdorWorldEntity>, target: OdorWorldEntity) {
        if (isReaching) return
        sendHome(child)
        sendHome(adult)
        this.actors = actors
        this.target = target
        holdCounts.clear()
        restCount = 0
        actors.forEach { actor ->
            holdCounts[actor] = 0
            actor.behavior = Pursue().apply {
                targetType = target.entityType
                maxSpeed = REACH_SPEED
                visionRange = 1000.0
            }
        }
    }

    fun step() {
        val target = target ?: return
        if (restCount > 0) {
            if (++restCount > REST_TICKS) {
                actors = emptyList()
                this.target = null
                restCount = 0
            }
            return
        }
        for (actor in actors) {
            val hold = holdCounts.getValue(actor)
            if (hold == 0 && actor.location.distance(target.location) < ARRIVAL_DISTANCE) {
                stop(actor)
                holdCounts[actor] = 1
            } else if (hold > 0 && hold != DONE && hold + 1 > HOLD_TICKS) {
                sendHome(actor)
                holdCounts[actor] = DONE
            } else if (hold > 0 && hold != DONE) {
                holdCounts[actor] = hold + 1
            }
        }
        if (actors.all { holdCounts[it] == DONE }) restCount = 1
    }

    /**
     * How clearly [agent] is seen going to [obj], between 0 and 1: speed times aim while moving, and 1 while grabbing
     * it.
     */
    fun seenGoingTo(agent: OdorWorldEntity, obj: OdorWorldEntity): Double {
        if (obj === target && isGrabbing(agent)) return 1.0
        val speed = agent.speed
        if (speed <= 0.0) return 0.0
        val dist = agent.location.distance(obj.location)
        if (dist < 1e-6) return 0.0
        val heading = agent.heading.toRadian()
        val aim = (cos(heading) * (obj.x - agent.x) - sin(heading) * (obj.y - agent.y)) / dist
        val aimScore = max(0.0, (aim - AIM_THRESHOLD) / (1 - AIM_THRESHOLD))
        return min(1.0, speed / REACH_SPEED) * aimScore
    }

    private fun stop(agent: OdorWorldEntity) {
        agent.behavior = NoOpBehavior()
        agent.speed = 0.0
        agent.dtheta = 0.0
    }

    private fun sendHome(agent: OdorWorldEntity) {
        stop(agent)
        val (home, heading) = if (agent === child) CHILD_HOME to CHILD_HEADING else ADULT_HOME to ADULT_HEADING
        agent.setLocation(home.first, home.second)
        agent.heading = heading
    }

    private companion object {
        const val DONE = -1
    }
}

private fun gaussian(x: Double, center: Double) = ((x - center) / TUNING_WIDTH).let { exp(-it * it / 2) }

/**
 * Drive each neuron of [population] by its tuning, plus noise so the neurons do not fire in lockstep.
 */
private fun drive(population: NeuronCollection, tunings: List<Double>) {
    population.neuronList.zip(tunings).forEach { (neuron, tuning) ->
        if (tuning > 0.05) {
            neuron.addInputValue(tuning * DRIVE_CURRENT + Random.nextDouble() * DRIVE_NOISE)
        }
    }
}

/**
 * Tuning of the visual neurons to a mover at [distance] from the object, scaled by how clearly it is seen ([level]).
 */
private fun visualTunings(level: Double, distance: Double, n: Int) = List(n) { i ->
    level * gaussian(distance, FAR_DISTANCE - i * (FAR_DISTANCE - NEAR_DISTANCE) / max(1, n - 1))
}

/**
 * Tuning of the motor phase neurons when the child is at [distance] from the object, or grabbing it.
 */
private fun motorTunings(distance: Double, grabbing: Boolean) =
    PHASE_DISTANCES.map { if (grabbing) 0.0 else gaussian(distance, it) } + (if (grabbing) 1.0 else 0.0)

suspend fun SimulationScope.setUpMirrorLearning(workspace: Workspace): MirrorTrips {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val network = networkComponent.network
    val collections = network.getModels<NeuronCollection>()
    val populations = POPULATIONS.map { name -> collections.first { it.label == name || it.label == mirrorLabel(name) } }
    val (visualCandy, motorCandy, visualBell, motorBell) = populations
    val synapses = network.freeSynapses

    val odorWorldComponent = workspace.componentList.filterIsInstance<OdorWorldComponent>().first()
    val world = odorWorldComponent.world
    fun entity(name: String) = world.entityList.first { it.name == name }
    val reaches = MirrorReaches(entity(CHILD), entity(ADULT), entity(CANDY), entity(BELL))
    val objects = listOf(reaches.candy to (visualCandy to motorCandy), reaches.bell to (visualBell to motorBell))

    val raster = workspace.componentList.filterIsInstance<RasterPlotComponent>().first()

    fun relabel(motor: NeuronCollection, mirror: Boolean) {
        val name = POPULATIONS[populations.indexOf(motor)]
        val label = if (mirror) mirrorLabel(name) else name
        if (motor.label == label) return
        motor.label = label
        raster.model.dataset.getSeries(populations.indexOf(motor)).key = label
    }

    val watchSpikes = mutableMapOf<NeuronCollection, Int>()

    network.updateManager.addAction(0, updateAction("Mirror learning inputs") {
        reaches.step()
        listOf(reaches.child, reaches.adult).forEach { it.caption = reaches.phaseOf(it) }
        for ((obj, pops) in objects) {
            val (visual, motor) = pops
            val seen = if (reaches.childSeesSelf) listOf(reaches.adult, reaches.child) else listOf(reaches.adult)
            for (person in seen) {
                val level = reaches.seenGoingTo(person, obj)
                if (level > 0.0) {
                    drive(visual, visualTunings(level, person.location.distance(obj.location), visual.neuronList.size))
                }
            }
            if (reaches.childTarget === obj) {
                val child = reaches.child
                drive(motor, motorTunings(child.location.distance(obj.location), reaches.isGrabbing(child)))
            }
            if (reaches.isReaching && !reaches.isChildActing) {
                val count = watchSpikes.getOrDefault(motor, 0) + motor.spikes.count { it > 0 }
                watchSpikes[motor] = count
                if (count >= MIRROR_SPIKES) relabel(motor, mirror = true)
            } else {
                watchSpikes.clear()
            }
        }
    })

    network.updateManager.addAction(updateAction("Mirror learning raster") {
        val rows = populations.sumOf { it.neuronList.size }
        var offset = 0
        populations.forEachIndexed { p, population ->
            val values = DoubleArray(rows)
            population.spikes.copyInto(values, offset)
            offset += population.neuronList.size
            raster.model.rasterConsumerList[p].setValues(values)
        }
    })

    suspend fun trip(actors: List<OdorWorldEntity>, target: OdorWorldEntity) {
        reaches.start(actors, target)
        if (workspace.updater.isRunning) {
            while (reaches.isReaching) delay(20)
        } else {
            workspace.iterateWhile { reaches.isReaching }
        }
    }

    val trips = MirrorTrips(reaches, ::trip)
    var busy = false
    var target = reaches.candy

    fun launchOnce(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        workspace.launch {
            try {
                block()
            } finally {
                busy = false
            }
        }
    }

    withGui {
        val controlPanel = createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addComboBox(
                "Object", listOf(CANDY, BELL), CANDY,
                toolTip = "The object the buttons below send people to."
            ) { name -> target = if (name == CANDY) reaches.candy else reaches.bell }
            addButton("Watch adult go") { launchOnce { trip(listOf(reaches.adult), target) } }.apply {
                toolTipText = "The adult goes to the object while the child watches."
            }
            addButton("Go") { launchOnce { trip(listOf(reaches.child), target) } }.apply {
                toolTipText = "The child goes to the object."
            }
            addCheckBox(
                "Child can see self", reaches.childSeesSelf,
                toolTip = "When checked, the child sees her own movement as she goes."
            ) { reaches.childSeesSelf = it }
            addSeparator()
            addCheckBox("Show spikes", false, toolTip = "Show a raster plot of the spikes in the child's brain.") {
                withContext(Dispatchers.Main) { raster.isGuiOn = it }
            }
            addSeparator()
            addButton("Reset learning") {
                if (busy) return@addButton
                synapses.forEach { it.strength = 0.0 }
                relabel(motorCandy, mirror = false)
                relabel(motorBell, mirror = false)
            }.apply {
                toolTipText = "Set every visual to motor weight back to zero and restore the Motor labels."
            }
        }.awaitLayout()

        val left = controlPanel.rightEdgeWithGap()
        place(odorWorldComponent, left, SIM_WINDOW_GAP, 620, 480)
        val worldFrame = (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).run {
            fitFrameToWorldSize()
            worldPanel.clearSelection()
            parentFrame.bounds
        }
        val worldBottom = worldFrame.y + worldFrame.height + SIM_WINDOW_GAP
        place(networkComponent, left + worldFrame.width + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 600, worldFrame.height + 300)
        place(raster, left, worldBottom, worldFrame.width, 300)
        (getDesktopComponent(raster).parentFrame as? JInternalFrame)?.isClosable = false
        withContext(Dispatchers.Main) { raster.isGuiOn = false }
    }

    return trips
}

/**
 * The trip runner set up for a workspace, so trips can also be run without the control panel.
 */
class MirrorTrips(
    val reaches: MirrorReaches,
    val trip: suspend (List<OdorWorldEntity>, OdorWorldEntity) -> Unit
)
