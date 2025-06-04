package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.WinnerTakeAll
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.showNumericInputDialog
import org.simbrain.util.toRadian
import org.simbrain.workspace.updater.UpdateComponent
import org.simbrain.workspace.updater.UpdateCoupling
import org.simbrain.workspace.updater.updateAction
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.GridSensor
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.util.function.Consumer
import kotlin.math.cos
import kotlin.math.sin

val actorCritic = newSim {

    var numTrials = 5

    /**
     * Learning Rate.
     */
    var alpha = .25

    /**
     * Eligibility trace. 0 for no trace; 1 for permanent trace. .9 default. Not
     * currently used.
     */
    var lambda = 0.0

    /**
     * Prob. of taking a random action. "Exploitation" vs. "exploration".
     */
    var epsilon = .25

    /**
     * Discount factor . 0-1. 0 predict next value only. .5 predict future
     * values. As it increases toward one, values of y in the more distant
     * future become more significant.t
     */
    var gamma = 1.0

    var stop = false
    var goalAchieved = false

    val numTilesInADimension = showNumericInputDialog("Num Tiles In a Dimension", 5) ?: return@newSim

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val odorWorldComponent = addOdorWorldComponent("World")

    val tileGridRatio = 2

    val world = odorWorldComponent.world.apply {
        tileMap = TileMap(numTilesInADimension * tileGridRatio, numTilesInADimension * tileGridRatio)
        isObjectsBlockMovement = false
        wrapAround = false
    }

    val tileSize = world.tileMap.tileWidth
    val gridSize = tileSize * tileGridRatio
    val mouseHomeLocation = gridSize * numTilesInADimension - gridSize / 2

    val mouse = world.addEntity(mouseHomeLocation, mouseHomeLocation, EntityType.Mouse).apply {
        heading = 90.0
    }
    val cheese = world.addEntity(gridSize / 2, gridSize / 2, EntityType.Swiss)

    fun resetMouse() {
        mouse.setLocation(mouseHomeLocation, mouseHomeLocation)
        mouse.heading = 90.0
    }

    val cheeseSensor = ObjectSensor().apply {
        label = "Cheese sensor"
        decayFunction = StepDecayFunction()
        decayFunction.dispersion = gridSize / 2.0
        // showDispersion = true
        mouse.addSensor(this)
    }

    val reward = network.addNeuron(300, 0).apply {
        clamped = true
        label = "Reward"
    }
    val value = network.addNeuron(350, 0).apply {
        label = "Value"
        upperBound = 100.0
    }
    val tdError = network.addNeuron(400, 0).apply {
        label = "TD Error"
        upperBound = 100.0
        lowerBound = -100.0
    }

    val gridSensor = GridSensor(
        0,
        0,
        (world.width / numTilesInADimension).toInt(),
        (world.height / numTilesInADimension).toInt()
    ).apply {
        highlighterVisibility = false
        columns = numTilesInADimension
        rows = numTilesInADimension
    }

    mouse.addSensor(gridSensor)

    val sensorNeurons = network.addNeuronGroup(
        100.0, 100.0, numTilesInADimension * numTilesInADimension
    ).apply {
        layout = GridLayout(50.0, 50.0)
        label = "Sensor Nodes"
    }

    // Outputs
    val outputs = WinnerTakeAll(network, 4).apply {
        network.addNetworkModel(this)
        params.isUseRandom = true
        params.randomProb = epsilon
        // Add a little extra spacing between neurons to accommodate labels
        layout = LineLayout(80.0, LineLayout.LineOrientation.HORIZONTAL)
        applyLayout(-5, -85)
        label = "Outputs"
        neuronList[0].label = "North"
        neuronList[1].label = "South"
        neuronList[2].label = "East"
        neuronList[3].label = "West"
    }

    // Set up connections
    val wts: List<Synapse> = network.connectAllToAll(sensorNeurons, value, 0.0)
    wts.forEach(Consumer { w: Synapse -> w.lowerBound = 0.0 })
    val wts2: List<Synapse> = network.connectAllToAll(sensorNeurons, outputs, 0.0)
    wts2.forEach(Consumer { w: Synapse -> w.lowerBound = 0.0 })

    val gridCoupling = couplingManager.createCoupling(gridSensor, sensorNeurons)
    val rewardCoupling = couplingManager.createCoupling(mouse.getSensor("Cheese sensor"), reward)

    // Time Series
    val (plot, rewardSeries, valueSeries, tdErrorSeries) = addTimeSeries("Reward, TD Error", seriesNames = listOf("Reward", "Value", "TD Error"))
    plot.apply {
        model.isAutoRange = true
        //model.rangeUpperBound = 2.0
        //model.rangeLowerBound = -1.0
        model.fixedWidth = true
        events.componentMinimized.fire(true)
    }
    val rewardPlot = couplingManager.createCoupling(reward, rewardSeries)
    val valuePlot = couplingManager.createCoupling(value, valueSeries)
    val errorPlot = couplingManager.createCoupling(tdError, tdErrorSeries)

    // Network Update
    network.updateManager.clear()
    network.updateManager.addAction(updateAction("RL Update") {

        with(network) {
            sensorNeurons.update()
            with(network) {
                updateNeurons(listOf(value))
                updateNeurons(listOf(reward))
            }
            outputs.update()
        }

        // aux values are used to store the last activation of the neuron
        tdError.activation = (reward.activation + gamma * value.activation) - value.auxValue

        // Reinforce based on the source neuron's last activation (not its
        // current value), since that is what the current td error reflects.
        value.fanIn.forEach { syn ->
            syn.strength += alpha * tdError.activation * syn.source.auxValue
        }

        // Update all actor neurons. Reinforce input > output connection that
        // were active at the last time-step.
        outputs.neuronList
            .filter { it.auxValue > 0 }
            .flatMap { it.fanIn }
            .filter { it.source.auxValue > 0 }
            .forEach { syn ->
                syn.strength += alpha * tdError.activation * syn.source.auxValue
            }

        // set aux values to last activations
        tdError.auxValue = tdError.activation
        value.auxValue = value.activation
        outputs.neuronList.forEach { it.auxValue = it.activation }
        sensorNeurons.neuronList.forEach { it.auxValue = it.activation }
    })

    // Workspace update
    workspace.updater.updateManager.clear()
    workspace.updater.updateManager.addAction(updateAction("Net -> Movement") {
        outputs.neuronList.firstOrNull { it.activation > 0.0 }?.let {

            fun OdorWorldEntity.applyGridMovement() {
                val dx = cos(heading.toRadian()) * gridSize
                val dy = -sin(heading.toRadian()) * gridSize

                val newX = x + dx
                val newY = y + dy

                location = if (world.wrapAround) {
                    val maxXLocation = world.width
                    val maxYLocation = world.height
                    point((newX + maxXLocation) % maxXLocation, (newY + maxYLocation) % maxYLocation)
                } else {
                    val newLocation = point(newX, newY)
                    if (world.contains(newLocation)) {
                        newLocation
                    } else {
                        point(x, y)
                    }
                }
            }

            when (it.label) {
                "North" -> mouse.heading = 90.0
                "South" -> mouse.heading = -90.0
                "East" -> mouse.heading = 0.0
                "West" -> mouse.heading = 180.0
                else -> {}
            }

            mouse.applyGridMovement()
        }
    })
    workspace.updater.updateManager.addAction(UpdateComponent(odorWorldComponent))
    workspace.updater.updateManager.addAction(UpdateCoupling(gridCoupling))
    workspace.updater.updateManager.addAction(UpdateCoupling(rewardCoupling))
    workspace.updater.updateManager.addAction(UpdateComponent(networkComponent))
    workspace.updater.updateManager.addAction(UpdateCoupling(rewardPlot))
    workspace.updater.updateManager.addAction(UpdateCoupling(valuePlot))
    workspace.updater.updateManager.addAction(UpdateCoupling(errorPlot))

    // Doc viewer
    addSidebarInfo(
        """
    # Actor Critic model

    Based on Richard Sutton (1996), *Generalization in Reinforcement Learning: Successful Examples Using Sparse Coarse Coding*. Simbrain implementation by Jeff Yoshimi and Jonathon Vickrey.

    ## Getting started

    A model which learns the location of rewarding stimuli. Do a few runs through 5 trials using the "run" button on the control panel. Using default values, the rat should figure out how to get the cheese.

    ## Parameters and what they mean

    **Epsilon**: Probability of taking a random action. 0 for no random actions; 1 for all random actions. [Doya, 2007](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2645553/) suggests this may be related to noradrenaline, which regulates overall arousal.

    **Learning rate**: How much weights are updated at each time step. [Doya, 2007](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2645553/) suggests this may be related to acetylcholine, which regulates some forms of plasticity.

    **Discount Factor (gamma)**: Determines how "future oriented" the agent is. Range is 0–1. A lower gamma makes the agent short-sighted; higher gamma leads to longer-term planning. With gamma near 1, the agent learns to value chains of actions leading to reward.

    Tanaka et al. (2007) relate gamma to serotonin:

    > *The activity of the ventral part of the striatum was correlated with reward prediction at shorter time scales, and this correlated activity was stronger at low serotonin levels. By contrast, the activity of the dorsal part of the striatum was correlated with reward prediction at longer time scales, and this correlated activity was stronger at high serotonin levels.* ([Tanaka et al., 2007](https://www.ncbi.nlm.nih.gov/pubmed/18091999))

    ## Reward, Value, TD Error

    - **Reward (red time series)**: Increases when the agent is on top of the cheese.
    - **Value (green time series)**: Increases when the agent expects reward.
    - **TD Error (blue time series)**: Signals mismatch between expected and received reward. Positive error increases value/action weights; negative error decreases them.

    ## Changes in values with learning

    Repeated trials tend to increase value, make reward more frequent, and reduce error. These trends depend on parameter settings.
    """.trimIndent()
    )


    // Lay everything out
    withGui {

        place(networkComponent,210, 10, 522, 595)
        place(odorWorldComponent, 728, 11, 500, 500)
        place(plot, 729, 599, 519, 293)
        (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).zoomToFitSize(500, 500)

        // Control panel
        createControlPanel("RL Controls", 10, 10) {

            val tfTrials = addTextField("Trials", "" + numTrials)
            val tfGamma = addTextField("Discount (gamma)", "" + gamma)
            val tfAlpha = addTextField("Alpha", "" + alpha)
            val tfEpsilon = addTextField("Epsilon", "" + epsilon)

            addButton("Run") {
                workspace.launch {
                    numTrials = tfTrials.text.toInt()
                    gamma = tfGamma.text.toDouble()
                    alpha = tfAlpha.text.toDouble()
                    epsilon = tfEpsilon.text.toDouble()
                    outputs.params.randomProb = epsilon

                    this@addButton.isEnabled = false
                    try {

                        stop = false

                        // Run the trials
                        for (i in 1..numTrials) {
                            if (stop) {
                                break
                            }
                            tfTrials.text = "" + i
                            goalAchieved = false
                            network.clearActivations()
                            resetMouse()

                            workspace.iterateWhile {
                                if (reward.activation > 0) {
                                    goalAchieved = true
                                }
                                !goalAchieved
                            }
                        }

                        // Reset the text in the trial field
                        tfTrials.text = "" + numTrials
                    } finally {
                        this@addButton.isEnabled = true
                    }
                }

            }

            addButton("Stop") {
                goalAchieved = true
                stop = true
            }
        }
    }

}