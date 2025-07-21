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
    # Introduction

    This simulation is based on Richard Sutton (1996), *Generalization in Reinforcement Learning: Successful Examples Using Sparse Coarse Coding*. 

    # Simulation Details

    This simulation models an agent that learns the location of rewarding stimuli using reinforcement learning. The environment of the agent is determined when you open the simulation and
    have stated your desired `Num Tiles`. 
    
    In this simulation, there are parameters that you can use to control agent's learning behavior. The parameters are:
    
    1) `Trials`: Determines how many trials the simulation will run before stopping.
    
    2) `Discount Factor (gamma)`: Determines how "future oriented" the agent is. Range is from `0`-`1`.
        
        - A lower gamma makes the agent short-sighted; higher gamma leads to longer-term planning. With gamma near `1`, the agent learns to value chains of actions that lead to reward.
        
        - Tanaka et al. (2007) relate gamma to serotonin. Below is a passage from their paper:
        
        _The activity of the ventral part of the striatum was correlated with reward prediction at shorter time scales, and this correlated activity was stronger at low serotonin levels. By contrast, the activity of 
        the dorsal part of the striatum was correlated with reward prediction at longer time scales, and this correlated activity was stronger at high serotonin levels._
        
    3) `Learning rate (Alpha)`: Determines how much the weights update at each time step. 
    
        - Doya (2007) suggests that this may be related to acetylcholine, which regulates some forms of plasticity.
    
    4) `Epsilon`: Determines the probability of taking a random action. `0` for no random actions; `1` for all random actions.
    
        - Doya (2007) suggests that this may be related to noradrenaline, which regulates overall arousal.
    
    In addition to the parameters, there is a time series plot that captures `reward`, `value`, and `TD Error`. This time series is hidden at the bottom of the screen. In the time series are
    three trend lines in three different colors: red, green, and blue. These colors correspond to the captured values mentioned above over time.
    
    1) `Reward` (red time series): This increases when the agent is on top of the cheese.
    
    2) `Value` (green time series): This increases when the agent expects a reward.
    
    3) `TD Error` (blue time series): This is the signal mismatch between expected and received reward. Positive error increases value/action weights whereas negative error decreases them.
    
    # What to Do
    
    In this simulation, there are parameters that you can use to change the agent's learning behaviors. To get a quick feel of the simulation, do the following steps:
    
    1) Click `Run` using the default parameters on the control panel.
    
    2) Observe the agent's action. It should figure out how to reach the cheese at the end of the run.
    
    3) Open the hidden time series plot that contains `reward`, `value`, and `TD Error`.
    
    4) Now, repeatedly click `Run` for a few times after simulation stops. As you continue to run trials, it will increase `value`, make `reward` more frequent, and reduce `TD Error`. 
    These trends will depend on your parameter settings and you can observe these changes in the time series plot.
    
    ## Experimenting With Other Parameter Values
    
    You can change the agent's learning behavior by changing the parameters of the simulation and then follow the steps above again to see the impacts of the changes on the simulation.
    
    # References
    
    1) Sutton, R. S. (1995). [Generalization in Reinforcement Learning: Successful Examples Using Sparse Coarse Coding.](https://proceedings.neurips.cc/paper_files/paper/1995/hash/8f1d43620bc6bb580df6e80b0dc05c48-Abstract.html) _Neural Information Processing Systems_; MIT Press.
    
    2) Tanaka, S. C., Schweighofer, N., Asahi, S., Shishida, K., Okamoto, Y., Yamawaki, S., & Doya, K. (2007). [Serotonin Differentially Regulates Short- and Long-Term Prediction of Rewards in the Ventral and Dorsal Striatum.](https://doi.org/10.1371/journal.pone.0001333) _PLoS ONE_, _2_(12), e1333.
    
    3) Doya, K. (2007). [Reinforcement learning: Computational theory and biological mechanisms.](https://doi.org/10.2976/1.2732246/10.2976/1) _HFSP Journal_, _1_(1), 30–40.
    
    # Credits
    
    Jonathon Vickrey
    
    [Jeff Yoshimi](www.jeffyoshimi.net)
    
    Kanly Thao
    
    """.trimIndent()
    )


    // Lay everything out
    withGui {

        place(networkComponent,210, 10, 520, 600)
        place(odorWorldComponent, 730, 10, 500, 500)
        place(plot, 730, 590, 520, 300)
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