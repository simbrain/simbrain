package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.piccolo.TileMap
import org.simbrain.workspace.couplings.Coupling
import org.simbrain.workspace.updater.UpdateComponent
import org.simbrain.workspace.updater.UpdateCoupling
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.GridSensor
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.util.function.Consumer
import javax.swing.JInternalFrame
import javax.swing.JLabel
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

val actorCritic = newSim {

    var numTrials = 5

    /**
     * Learning Rate.
     */
    var alpha = .25

    /**
     * Prob. of taking a random action. "Exploitation" vs. "exploration".
     */
    var epsilon = .25

    /**
     * Discount factor. 0-1. 0 predict next value only. .5 predict future
     * values. As it increases toward one, values of y in the more distant
     * future become more significant.
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
        isShowSensorsAndEffectors = false
    }
    val cheese = world.addEntity(gridSize / 2, gridSize / 2, EntityType.Swiss)
    val poison = world.addEntity(gridSize / 2, mouseHomeLocation, EntityType.Poison)

    fun resetMouse() {
        mouse.setLocation(mouseHomeLocation, mouseHomeLocation)
        mouse.heading = 90.0
    }

    var cheeseReward = 1.0
    var poisonReward = -1.0

    val cheeseSensor = ObjectSensor().apply {
        label = "Cheese sensor"
        decayFunction = StepDecayFunction()
        decayFunction.dispersion = gridSize / 2.0
        mouse.addSensor(this)
    }

    val poisonSensor = ObjectSensor(EntityType.Poison).apply {
        label = "Poison sensor"
        decayFunction = StepDecayFunction()
        decayFunction.dispersion = gridSize / 2.0
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

    val sensorNeurons = network.addNeuronCollection(
        numTilesInADimension * numTilesInADimension
    ).apply {
        layout = GridLayout(50.0, 50.0)
        applyLayout(100, 100)
        label = "Sensor nodes"
    }

    // Outputs
    val outputs = network.addNeuronCollection(4).apply {
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
    val valueWts: List<Synapse> = network.connectAllToAll(sensorNeurons, value, 0.0)
    val actorWts: List<Synapse> = network.connectAllToAll(sensorNeurons, outputs, 0.0)
    actorWts.forEach(Consumer { w: Synapse -> w.lowerBound = 0.0 })

    val gridCoupling = couplingManager.createCoupling(gridSensor, sensorNeurons)

    var showValues = false
    var showGrid = true

    /**
     * Draws a color overlay on tiles based on learned value weights. Green indicates positive value, red indicates negative, with alpha proportional to magnitude.
     */
    val valueOverlay = object : PNode() {
        private val tileStroke = BasicStroke(1f)
        private val borderColor = Color(255, 255, 255, 35)

        override fun paint(paintContext: PPaintContext) {
            val graphics = paintContext.graphics as Graphics2D
            val values = valueWts.map { it.strength }
            if (values.isEmpty()) return

            val maxMagnitude = max(values.maxOfOrNull { kotlin.math.abs(it) } ?: 0.0, 1e-9)

            for (row in 0 until numTilesInADimension) {
                for (col in 0 until numTilesInADimension) {
                    val tileIndex = col + row * numTilesInADimension
                    val tileValue = values.getOrElse(tileIndex) { 0.0 }
                    val normalizedValue = (tileValue / maxMagnitude).coerceIn(-1.0, 1.0)
                    if (showGrid) {
                        val alpha = (140 * kotlin.math.abs(normalizedValue)).toInt().coerceIn(0, 255)
                        graphics.color = if (normalizedValue >= 0) {
                            Color(70, 185, 120, alpha)
                        } else {
                            Color(225, 110, 110, alpha)
                        }
                        graphics.fillRect(col * gridSize, row * gridSize, gridSize, gridSize)
                    }

                    graphics.color = borderColor
                    graphics.stroke = tileStroke
                    graphics.drawRect(col * gridSize, row * gridSize, gridSize, gridSize)
                }
            }

            if (showValues) {
                graphics.drawNumericOverlay(
                    data = values.toDoubleArray(),
                    rows = numTilesInADimension,
                    cols = numTilesInADimension,
                    imageWidth = world.width,
                    imageHeight = world.height,
                    scalingFactor = 1.0,
                    decimalPlaces = 2
                )
            }
        }
    }.apply {
        pickable = false
        setBounds(0.0, 0.0, world.width, world.height)
    }

    // Network Update
    network.updateManager.clear()
    network.updateManager.addAction(updateAction("RL Update") {

        with(network) {
            sensorNeurons.neuronList.forEach { it.accumulateFanInInputs() }
            sensorNeurons.neuronList.forEach { it.update() }
            with(network) {
                updateNeurons(listOf(value))
                updateNeurons(listOf(reward))
            }
            // Manual WTA update for outputs
            outputs.neuronList.forEach { it.accumulateFanInInputs() }
            outputs.neuronList.forEach { it.update() }
            var winner = getWinner(outputs.neuronList, false)
            if (kotlin.random.Random.nextDouble() < epsilon) {
                winner = outputs.neuronList.random()
            }
            outputs.neuronList.forEach { n ->
                n.activation = if (n === winner) 1.0 else 0.0
            }
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
    workspace.updater.updateManager.addAction(UpdateComponent(odorWorldComponent))
    workspace.updater.updateManager.addAction(UpdateCoupling(gridCoupling))
    workspace.updater.updateManager.addAction(updateAction("Update Reward") {
        reward.activation = cheeseSensor.currentValue * cheeseReward + poisonSensor.currentValue * poisonReward
    })
    workspace.updater.updateManager.addAction(UpdateComponent(networkComponent))
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
    // Doc viewer
    addSidebarInfo(
    """
    # Actor Critic

    This simulation models an agent that learns the location of rewarding and aversive stimuli using [reinforcement learning](https://en.wikipedia.org/wiki/Reinforcement_learning). Each time you press `run` a simulation is run. The agent (the mouse) initially takes random actions. But when it finds the cheese it is rewarded, and it reinforces the action of moving towards the cheese next time it is near it. When it encounters the poison it receives negative reward and learns to avoid it. It also learns to value states leading to reward and devalue states leading to punishment. In this way it slowly learns a path to the cheese while avoiding the poison. In the default case of a `5x5` world, after about `15` trials it should be able to do a pretty good job of finding the cheese. Once it learns this, you can move the cheese or poison and observe how it adapts. However you set up the cheese or poison (and you can also just delete one of them) it will build up a kind of map of the space into good and bad regions and move through the space in a predictable way.
    
    To get an immediate feel for the simulation, click the `Run` button.
    
    The color overlay on the world tiles illustrates these ideas by showing the learned values of locations (value is expected future reward). Locations are colored green proportional to positive value (near rewards like cheese) and red proportional to negative value (near punishments like poison). Notice that as trials are run first the square nearest the cheese turns green (given how the network is wired, the agent is likely to move from that square to a rewarding square soon), then squares near that, and so on until a path backward to the agent's starting position is formed. Similarly, red regions form around the poison as the agent learns to avoid it.  
        
    Use the `Show Time Series` button to open a [time series](https://docs.simbrain.net/docs/plots/timeSeries.html) window showing how `reward`, `value`, and `TD error` unfold as the simulation runs.
       
    Tip: To get the simulation to run faster, minimize the network window or make the weights in the network invisible (network > view > toggle weight visibility)
       
    # Simulation Details

    The simulation uses the [actor critic](https://en.wikipedia.org/wiki/Actor-critic_algorithm) algorithm. The algorithm is biologically realistic, modeling how dopamine is used to predict reward.
    
    `Sensor` nodes represent a flattened [sensor grid](https://docs.simbrain.net/docs/worlds/odorworld.html#grid-sensor) which is modeled on place cells in the brain. As the simulation runs notice that the agent's current location is reflected in these.
     
    `Outputs` are a simple motor system (the "actor" in "actor critic") that is based on a [winner take all](https://docs.simbrain.net/docs/network/neurongroups/wta.html) algorithm.
    
    The `value` node is the "critic", that estimates how likely reward is to be achieved relative to the current state.
    
    The `TD error` node corresponds to a dopamine signal, which indicates how much better or worse reward is relative to what the critic or value node is predicting. When things are better than expected, the node fires above `0`; when worse it fires below `0`.
   
    The weights between the sensor nodes and the outputs and between the sensor nodes and the value node are what are trained. 
    The weight change is determined by the `learning rate` and the `TD error`. So when things are better than expected (`TD error` is positive)
    the weights are strengthened; when they are worse than expected, the are weakened.
    
    ## Parameters
    
    The following parameters can be used to control how the agent learns. The parameters are:
    
    1) `Trials`: Determines how many trials the simulation will run before stopping. A trial ends when the agent reaches the cheese. 
    2) `Discount Factor (gamma)`: Determines how "future oriented" the agent is. Range is from `0`-`1`.
        
        - A lower gamma makes the agent short-sighted; higher gamma leads to longer-term planning. With gamma near `1`, the agent learns to value chains of actions that lead to reward.
        
        - Tanaka et al. (2007) relate gamma to serotonin: "The activity of the ventral part of the striatum was correlated with reward prediction at shorter time scales, and this correlated activity was stronger at low serotonin levels. By contrast, the activity of 
        the dorsal part of the striatum was correlated with reward prediction at longer time scales, and this correlated activity was stronger at high serotonin levels."
        
    3) `Learning rate (Alpha)`: Determines how much the weights update at each time step. Doya (2007) suggests that this may be related to acetylcholine, which regulates some forms of plasticity.
    
    4) `Epsilon`: Determines the probability of taking a random action. `0` for no random actions; `1` for all random actions. Doya (2007) suggests that this may be related to noradrenaline, which regulates overall arousal.
    
    5) `Cheese Reward`: The value received when the agent reaches the cheese. Positive values reinforce approach behavior.
    
    6) `Poison Reward`: The value received when the agent reaches the poison. Negative values create aversive learning and produce red tiles in the value overlay.

    7) `Show Grid`: Toggles the colored value overlay on the world tiles. When checked, green tiles indicate locations with positive learned value and red tiles indicate locations with negative learned value.

    8) `Show Values`: Toggles numeric value labels on the world tiles so you can inspect the learned value estimate for each location directly.

    9) `Show Time Series`: Opens or hides the plot of reward, value, and TD error over time.
    
    ## Time Series
    
    1) `Reward` (red time series): Positive when the agent is on the cheese, negative when on the poison.
    2) `Value` (green time series): Increases when the agent expects reward, decreases when it expects punishment. This basically tracks the color values on the world.
    3) `TD Error` (blue time series): The mismatch between expected and received reward. Positive error increases value/action weights whereas negative error decreases them. Updates happen for the location where the agent was at during the last time step. To get a feel for this, you can move the mouse directly from a neutral square to the cheese or poison, and update with the step button, and see that square turn green or red, reflecting the fact that being in that square led to something good or bad.
    
    # What to Do
    
    First, click the `run` button in the control panel on the left side of the screen. You can also just click the main desktop run but it won't automatically reset the mouse location each time it gets cheese and run through the trials (The desktop run button can be useful to just see what it does when not reset: which is to learn to just stay near cheese and eat away!).
   
    Observe the agent's actions. It should figure out how to reach the cheese while avoiding the poison at the end of the first set of trials. The green and red location coloring gives you a sense of how it builds up a map of which states are valuable (green) and which are aversive (red).
    
    You can move the cheese or poison around as you run trials, and see how it will follow old "trails" and build new ones. For example, after it learns to find the cheese in the upper left, you can pull the cheese down a little, and run until it finds the new spot. You can hold the cheese "near" it to help it along and keep re-running the sim. You can make it learn to follow an arbitrary pattern to find the cheese.
    
    It's easy to delete, move, or add additional cheese and poison entities to create customized environments (right click in odor world and select `add entity`). Watch how the world gets populated with "good" (green) and "bad" (red) regions or trails as the agent learns. You can also easily delete cheeses or poisons. 
    
    You can also study the time series plot to get a better sense of how reward, value and td error work together. Rewards only happen on the cheese or poison. Values accumulate on a path towards the rewarding stimuli.  TD error only spikes up or down after moving to a better or lower place. 
    
    After the agent has learned, try moving the cheese or poison and observe how it perseverates on old locations before adapting.
     
    ## Experimenting With Parameter Values
    
    You can change the agent's learning behavior by changing the parameters of the simulation and then follow the steps above again to see the impacts of the changes on the simulation.
    
    - By making epsilon higher, like `0.8`, it should move randomly. Then try making it low, or take it all the way to `0`. 
        At `0` it will follow the same path every time, reflecting what it's learned.
    - Higher gamma encourages longer-term thinking. At `0` it only learns one step ahead, and it will never learn the full path. As it approaches 1 it thinks more and more long term and will learn the path, but as gamma is higher it takes longer to learn. 
    
    # References
    
    Doya, K. (2007). [_Reinforcement learning: Computational theory and biological mechanisms_](https://doi.org/10.2976/1.2732246/10.2976/1) _HFSP Journal_, _1_(1), 30–40.

    Sutton, R. S. (1995). [_Generalization in Reinforcement Learning: Successful Examples Using Sparse Coarse Coding_](https://proceedings.neurips.cc/paper_files/paper/1995/hash/8f1d43620bc6bb580df6e80b0dc05c48-Abstract.html) _Neural Information Processing Systems_. MIT Press.

    Tanaka, S. C., Schweighofer, N., Asahi, S., Shishida, K., Okamoto, Y., Yamawaki, S., & Doya, K. (2007). [_Serotonin Differentially Regulates Short- and Long-Term Prediction of Rewards in the Ventral and Dorsal Striatum_](https://doi.org/10.1371/journal.pone.0001333) _PLoS ONE_, _2_(12), e1333.
    
    # Credits
    
    Jonathon Vickrey
    
    [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
    
    Kanly Thao
    
    """.trimIndent()
    )


    // Lay everything out
    withGui {

        val odorWorldDesktopComponent = (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).apply {
            worldPanel.canvas.layer.addChild(world.tileMap.layers.size, valueOverlay)
            zoomToFitSize(500, 500)
        }

        fun refreshValueOverlay() {
            valueOverlay.invalidatePaint()
            odorWorldDesktopComponent.worldPanel.canvas.repaint()
        }

        var plotFrame: JInternalFrame? = null
        var timeSeriesPlot: TimeSeriesPlotComponent? = null
        var timeSeriesCouplings = emptyList<Coupling>()
        var timeSeriesUpdateActions = emptyList<UpdateCoupling>()
        var plotX = 0
        val plotY = SIM_WINDOW_GAP + 500 + SIM_WINDOW_GAP

        suspend fun showTimeSeries() {
            val existingFrame = plotFrame
            if (existingFrame != null) {
                existingFrame.isIcon = false
                existingFrame.toFront()
                return
            }

            val (plot, rewardSeries, valueSeries, tdErrorSeries) = addTimeSeries(
                "Reward, TD Error",
                seriesNames = listOf("Reward", "Value", "TD Error")
            )
            plot.apply {
                model.isAutoRange = true
                model.fixedWidth = true
            }
            timeSeriesCouplings = listOf(
                couplingManager.createCoupling(reward, rewardSeries),
                couplingManager.createCoupling(value, valueSeries),
                couplingManager.createCoupling(tdError, tdErrorSeries)
            )
            timeSeriesUpdateActions = timeSeriesCouplings.map(::UpdateCoupling)
            timeSeriesUpdateActions.forEach(workspace.updater.updateManager::addAction)

            withContext(Dispatchers.Swing) {
                place(plot, plotX, plotY, 520, 300)
                timeSeriesPlot = plot
                plotFrame = getDesktopComponent(plot).parentFrame as? JInternalFrame
                plotFrame?.apply {
                    isClosable = false
                    toFront()
                }
            }
        }

        suspend fun hideTimeSeries() {
            timeSeriesUpdateActions.forEach(workspace.updater.updateManager::removeAction)
            couplingManager.removeCouplings(timeSeriesCouplings)
            withContext(Dispatchers.Swing) {
                timeSeriesPlot?.let(workspace::removeWorkspaceComponent)
            }
            plotFrame = null
            timeSeriesPlot = null
            timeSeriesCouplings = emptyList()
            timeSeriesUpdateActions = emptyList()
        }

        // Control panel
        val controlPanel = createControlPanel("RL Controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {

            val tfTrials = addTextField("Trials", "" + numTrials)
            val tfGamma = addTextField("Discount (gamma)", "" + gamma)
            val tfAlpha = addTextField("Alpha", "" + alpha)
            val tfEpsilon = addTextField("Epsilon", "" + epsilon)
            val tfCheeseReward = addTextField("Cheese Reward", "" + cheeseReward)
            val tfPoisonReward = addTextField("Poison Reward", "" + poisonReward)
            addCheckBox("Show Grid", showGrid) {
                showGrid = it
                refreshValueOverlay()
            }
            addCheckBox("Show Values", showValues) {
                showValues = it
                refreshValueOverlay()
            }
            // Hyphens are just a hack to make sure the panel is big enough when trial numbers are shown
            val progressLabel = JLabel("Status: ------ Ready ------")
            addComponent(progressLabel)

            addButton("Run") {
                workspace.launch {
                    numTrials = tfTrials.text.toInt()
                    gamma = tfGamma.text.toDouble()
                    alpha = tfAlpha.text.toDouble()
                    epsilon = tfEpsilon.text.toDouble()
                    cheeseReward = tfCheeseReward.text.toDouble()
                    poisonReward = tfPoisonReward.text.toDouble()

                    this@addButton.isEnabled = false
                    try {

                        stop = false

                        // Run the trials
                        for (i in 1..numTrials) {
                            if (stop) {
                                break
                            }
                            progressLabel.text = "Status: Running Trial $i of $numTrials"
                            goalAchieved = false
                            network.clearActivations()
                            value.auxValue = 0.0
                            resetMouse()

                            workspace.iterateWhile {
                                if (reward.activation > 0) {
                                    goalAchieved = true
                                }
                                !goalAchieved
                            }
                        }

                        // Reset the status
                        progressLabel.text = "Status: Completed $numTrials trials"
                    } finally {
                        this@addButton.isEnabled = true
                    }
                }

            }

            addButton("Stop") {
                goalAchieved = true
                stop = true
                progressLabel.text = "Status: Stopped"
            }

            addButton("Reset") {
                valueWts.forEach { it.strength = 0.0 }
                actorWts.forEach { it.strength = 0.0 }
                network.clearActivations()
                value.auxValue = 0.0
                resetMouse()
                progressLabel.text = "Status: Reset"
            }

            addSeparator()

            val timeSeriesButton = addButton("Show Time Series") {
                if (plotFrame == null) {
                    showTimeSeries()
                    this@addButton.text = "Hide Time Series"
                } else {
                    hideTimeSeries()
                    this@addButton.text = "Show Time Series"
                }
            }
            timeSeriesButton.toolTipText = "Create or remove the reward, value, and TD error plot"
        }.awaitLayout()

        val mainX = controlPanel.rightEdgeWithGap()
        plotX = mainX + 520 + SIM_WINDOW_GAP
        place(networkComponent, mainX, SIM_WINDOW_GAP, 520, 600)
        place(odorWorldComponent, mainX + 520 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 500, 500)
    }

}
