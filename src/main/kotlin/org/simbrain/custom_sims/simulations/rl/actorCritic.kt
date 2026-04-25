package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.StepDecayFunction
import org.simbrain.util.piccolo.TileMap
import org.simbrain.workspace.updater.UpdateComponent
import org.simbrain.workspace.updater.UpdateCoupling
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.GridSensor
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.awt.BasicStroke
import java.awt.Color
import java.util.function.Consumer
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
    valueWts.forEach(Consumer { w: Synapse -> w.lowerBound = 0.0 })
    val actorWts: List<Synapse> = network.connectAllToAll(sensorNeurons, outputs, 0.0)
    actorWts.forEach(Consumer { w: Synapse -> w.lowerBound = 0.0 })

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

    /**
     * Draws a color overlay on tiles based on learned value weights. Green indicates positive value, red indicates negative, with alpha proportional to magnitude.
     */
    val valueOverlay = object : PNode() {
        private val tileStroke = BasicStroke(1f)
        private val borderColor = Color(255, 255, 255, 35)

        override fun paint(paintContext: PPaintContext) {
            val graphics = paintContext.graphics
            val values = valueWts.map { it.strength }
            if (values.isEmpty()) return

            val maxMagnitude = max(values.maxOfOrNull { kotlin.math.abs(it) } ?: 0.0, 1e-9)

            for (row in 0 until numTilesInADimension) {
                for (col in 0 until numTilesInADimension) {
                    val tileIndex = col + row * numTilesInADimension
                    val normalizedValue = (values.getOrElse(tileIndex) { 0.0 } / maxMagnitude).coerceIn(-1.0, 1.0)
                    val alpha = (140 * kotlin.math.abs(normalizedValue)).toInt().coerceIn(0, 255)
                    graphics.color = if (normalizedValue >= 0) {
                        Color(70, 185, 120, alpha)
                    } else {
                        Color(225, 110, 110, alpha)
                    }
                    graphics.fillRect(col * gridSize, row * gridSize, gridSize, gridSize)

                    graphics.color = borderColor
                    graphics.stroke = tileStroke
                    graphics.drawRect(col * gridSize, row * gridSize, gridSize, gridSize)
                }
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
    workspace.updater.updateManager.addAction(UpdateCoupling(rewardCoupling))
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
    workspace.updater.updateManager.addAction(UpdateCoupling(rewardPlot))
    workspace.updater.updateManager.addAction(UpdateCoupling(valuePlot))
    workspace.updater.updateManager.addAction(UpdateCoupling(errorPlot))

    // Doc viewer
    addSidebarInfo(
    """
    # Actor Critic

    This simulation models an agent that learns the location of rewarding stimuli using [reinforcement learning](https://en.wikipedia.org/wiki/Reinforcement_learning). Each time you press `run` a simulation is run. The agent (the mouse) initially takes random actions. But when it finds the cheese it is rewarded, and it reinforces the action of moving towards the cheese next time it is near it. It also learns to value the state it's in right before it gets the cheese, and will reinforce actions that lead to that state. In this way it slowly learns a path to the cheese. In the default case of a `5x5` world, after about `15` trials it should be able to do a pretty good job of finding the cheese. Once it learns this, you can move the cheese a little and observe it "looking" for the cheese in the location where it initially learned it to be.
    
    To get an immediate feel for the simulation, click the `Run` button.
    
    The color overlay on the world tiles illustrates these ideas by showing the learned values of locations (value is expected future reward). Locations are colored green proportional to how much value they have. Notice that as trials are run first the square nearest the cheese turns green (given how the network is wired, the agent is likely to move from that square to a rewarding square soon), then squares near that, and so on until a path backward to the agent's starting position is formed.
        
    A [time series](https://docs.simbrain.net/docs/plots/timeSeries.html) window is minimized that shows how `reward`, `value`, and `TD error` unfold as the simulation runs.
       
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
    
    1) `Trials`: Determines how many trials the simulation will run before stopping. A trial ends when it reaches the cheese. 
    2) `Discount Factor (gamma)`: Determines how "future oriented" the agent is. Range is from `0`-`1`.
        
        - A lower gamma makes the agent short-sighted; higher gamma leads to longer-term planning. With gamma near `1`, the agent learns to value chains of actions that lead to reward.
        
        - Tanaka et al. (2007) relate gamma to serotonin: "The activity of the ventral part of the striatum was correlated with reward prediction at shorter time scales, and this correlated activity was stronger at low serotonin levels. By contrast, the activity of 
        the dorsal part of the striatum was correlated with reward prediction at longer time scales, and this correlated activity was stronger at high serotonin levels."
        
    3) `Learning rate (Alpha)`: Determines how much the weights update at each time step. Doya (2007) suggests that this may be related to acetylcholine, which regulates some forms of plasticity.
    
    4) `Epsilon`: Determines the probability of taking a random action. `0` for no random actions; `1` for all random actions. Doya (2007) suggests that this may be related to noradrenaline, which regulates overall arousal.
    
    ## Time Series
    
    1) `Reward` (red time series): This increases when the agent is on top of the cheese.
    2) `Value` (green time series): This increases when the agent expects a reward.
    3) `TD Error` (blue time series): This is the signal mismatch between expected and received reward. Positive error increases value/action weights whereas negative error decreases them.
    
    # What to Do
    
    1) Click the `run` button in the control panel on the left side of the screen.
    2) Observe the agent's actions. It should figure out how to reach the cheese at the end of the run.
    3) Open the hidden time series plot that contains `reward`, `value`, and `TD error`.
    4) Now, keep training the agent. As you continue to run trials, it will increase `value`, make `reward` more frequent, and reduce `TD Error`.     
    5) After it has learned where the cheese is, you can keep training it, but move where the cheese is. Observe how it perseverates on old locations. 
    6) After it learns to find the cheese in the upper left, you can pull the cheese
    down a little, and run until it finds the new spot. You can hold the cheese "near" it to help it along and keep re-running the sim. 
    You can eventually make it learn to follow an arbitrary pattern to find the cheese.
    
    ## Experimenting With Parameter Values
    
    You can change the agent's learning behavior by changing the parameters of the simulation and then follow the steps above again to see the impacts of the changes on the simulation.
    
    - By making epsilon higher, like `0.8`, it should move randomly. Then try making it low, or take it all the way to `0`. 
        At `0` it will follow the same path every time, reflecting what it's learned.
    - Higher gamma encourages longer-term thinking. At `0` it only learns one step ahead, and it will never learn the full path. 
        As it approaches 1 it thinks more and more long term and will learn the path, but as gamma is higher it takes longer to learn. 
    
    # References
    
    Doya, K. (2007). [Reinforcement learning: Computational theory and biological mechanisms.](https://doi.org/10.2976/1.2732246/10.2976/1) _HFSP Journal_, _1_(1), 30–40.
    
    Sutton, R. S. (1995). [Generalization in Reinforcement Learning: Successful Examples Using Sparse Coarse Coding.](https://proceedings.neurips.cc/paper_files/paper/1995/hash/8f1d43620bc6bb580df6e80b0dc05c48-Abstract.html) _Neural Information Processing Systems_. MIT Press.
    
    Tanaka, S. C., Schweighofer, N., Asahi, S., Shishida, K., Okamoto, Y., Yamawaki, S., & Doya, K. (2007). [Serotonin Differentially Regulates Short- and Long-Term Prediction of Rewards in the Ventral and Dorsal Striatum.](https://doi.org/10.1371/journal.pone.0001333) _PLoS ONE_, _2_(12), e1333.
    
    # Credits
    
    Jonathon Vickrey
    
    [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
    
    Kanly Thao
    
    """.trimIndent()
    )


    // Lay everything out
    withGui {

        place(networkComponent,240, 10, 520, 600)
        place(odorWorldComponent, 760, 10, 500, 500)
        place(plot, 760, 590, 520, 300)
        (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).apply {
            worldPanel.canvas.layer.addChild(world.tileMap.layers.size, valueOverlay)
            zoomToFitSize(500, 500)
        }

        // Control panel
        createControlPanel("RL Controls", 10, 10) {

            val tfTrials = addTextField("Trials", "" + numTrials)
            val tfGamma = addTextField("Discount (gamma)", "" + gamma)
            val tfAlpha = addTextField("Alpha", "" + alpha)
            val tfEpsilon = addTextField("Epsilon", "" + epsilon)
            // Hyphens are just a hack to make sure the panel is big enough when trial numbers are shown
            val progressLabel = JLabel("Status: ------ Ready ------")
            addComponent(progressLabel)

            addButton("Run") {
                workspace.launch {
                    numTrials = tfTrials.text.toInt()
                    gamma = tfGamma.text.toDouble()
                    alpha = tfAlpha.text.toDouble()
                    epsilon = tfEpsilon.text.toDouble()

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
        }
    }

}
