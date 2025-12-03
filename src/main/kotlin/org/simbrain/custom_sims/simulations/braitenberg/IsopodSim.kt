package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import org.json.JSONObject
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.connect
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.SmellSensor

/**
 * A simulation of Isopod navigation. With Peter Hinow and Kaiden Schmidt.
 *
 * Run headless using:
 * gradle runSim -PsimName="Isopod Sim" -PoptionString='{"numTrials": 5, "speedInhibition": false}'
 *
 * Parameters:
 * - numTrials: Number of trials to run
 * - speedInhibition: Enable speed inhibition
 * - maxIterationsPerTrial: Max steps per trial
 * - hitRadius: Collision detection radius
 * - smellDispersion: Smell decay dispersion
 * - speed: Movement speed
 */
val isopodSim = newSim { optionString ->

    // Adjustable parameters for sim
    var defaultNumTrials = 5
    val maxIterationsPerTrial = 5000
    val hitRadius = 80
    val smellDispersion = 350.0
    val speed = 5.0

    // Other variables
    val log = StringBuilder()
    var trialNum = 0

    // Clear the workspace
    workspace.clearWorkspace()

    // ----- Network construction ------

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    var noiseSource = NormalDistribution(2.0, .9)

    val neuronLeftSensor = network.addNeuron {
        location = point(0, 100)
        upperBound = 100.0
        label = "Left"
        with(updateRule as LinearRule) {
            noiseGenerator = noiseSource
            addNoise = true
        }
    }
    val neuronRightSensor = network.addNeuron {
        location = point(100, 100)
        upperBound = 100.0
        label = "Right"
        with(updateRule as LinearRule) {
            noiseGenerator = noiseSource
            addNoise = true
        }
    }
    val neuronLeftTurning = network.addNeuron {
        location = point(0, 0)
        upperBound = 150.0
        label = "Turn left"
    }
    val neuronRightTurning = network.addNeuron {
        location = point(100, 0)
        upperBound = 150.0
        label = "Turn right"
    }
    val neuronStraight = network.addNeuron {
        location = point(50, 0)
        lowerBound = 0.0
        upperBound = 10.0
        label = "Straight"
        bias = speed
    }

    // Create the weights
    with(network) {
        connect(neuronLeftSensor, neuronLeftTurning, 10.0, 0.0, 50.0)
        connect(neuronRightSensor, neuronRightTurning, 10.0, 0.0, 50.0)
    }
    var leftSpeedWeight: Synapse? = null
    var rightSpeedWeight: Synapse? = null

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 210)
            width = 400
            height = 400
        }
    }

    // ----- Build 2d World ------

    val odorWorldComponent = addOdorWorldComponent("World")
    val odorWorld = odorWorldComponent.world

    // Object references we'll need later
    val straightMovement: Effector
    val turnLeft: Effector
    val turnRight: Effector
    var leftSensor: SmellSensor
    var rightSensor: SmellSensor
    val isopod: OdorWorldEntity

    var collision = false

    odorWorld.apply {

        wrapAround = false
        isObjectsBlockMovement = true

        // World size in pixels is implicitly set by this
        tileMap.updateMapSize(25, 25)
        tileMap.fill("water_1")

        // Body could be represented by a triangle or rhombus
        isopod = addEntity(centerLocation.x, centerLocation.y, EntityType.Isopod).apply {
            name = "isopod"
            heading = 90.0
            addDefaultEffectors()
            straightMovement = effectors[0]
            turnLeft = effectors[1]
            turnRight = effectors[2]
            isShowSensorsAndEffectors = false

            // Can add more smell sensors here
            // Options: new sensor in back; triangular array
            leftSensor = SmellSensor().apply {
                radius = 40.0
                theta = 45.0
                addSensor(this)
            }
            rightSensor = SmellSensor().apply {
                radius = 40.0
                theta = -45.0
                addSensor(this)
            }
            events.collided.on {
                if (it is OdorWorld && !collision) {
                    log.append("# Collided with wall\n")
                }
                collision = true
            }
            manualMovement.manualStraightMovementIncrement = 2.0
            manualMovement.manualMotionTurnIncrement = 2.0
        }

        suspend fun addFish(x: Double, y: Double) {
            odorWorld.addEntity(x, y, EntityType.Fish).apply {
                name = "Fish"
                // Smell value when agent is right on top of fish
                val maxVal = 1.1
                smellSource = SmellSource.createScalarSource(maxVal).apply {
                    // How the smell decays with distances
                    decayFunction = LinearDecayFunction()
                    decayFunction.peakDistance = 0.0
                    decayFunction.dispersion = smellDispersion
                    showDispersion = true
                }
                // A convenient way to show the hit radius. Not used as a sensor.
                addSensor(ObjectSensor().apply {
                    radius = 0.0
                    decayFunction.dispersion = hitRadius.toDouble()
                    showDispersion = true
                })
            }
        }

        // adding fish to four corners of the world
        val fishHalfWidth = EntityType.Fish.width / 2.0
        val fishHalfHeight = EntityType.Fish.height / 2.0

        addFish(odorWorld.width - fishHalfWidth, odorWorld.height - fishHalfHeight)
        addFish(fishHalfWidth, odorWorld.height - fishHalfHeight)
        addFish(fishHalfWidth, fishHalfHeight)
        addFish(odorWorld.width - fishHalfWidth, fishHalfHeight)

    }

    withGui {
        place(odorWorldComponent) {
            location = point(410, 10)
            width = 600
            height = 600
        }
        isopod.select()
    }

    // ----- Make Couplings ------

    with(couplingManager) {
        neuronStraight couple straightMovement
        neuronLeftTurning couple turnLeft
        neuronRightTurning couple turnRight
        leftSensor couple neuronLeftSensor
        rightSensor couple neuronRightSensor
    }

    fun resetIsopod() {
        isopod.location = odorWorld.centerLocation
        isopod.heading = UniformRealDistribution(0.0, 360.0).sampleDouble()
    }

    // Create the fish collision detection action once and add it to workspace
    val fishCollisionAction = updateAction("Found fish") {
        val foundFish = odorWorld.entityList
            .filter { it.entityType == EntityType.Fish }
            .any { fish -> fish.location.distance(isopod.location) < hitRadius }
        if (foundFish) {
            log.append("# Collided with fish\n")
            collision = true
        }
    }
    workspace.addUpdateAction(fishCollisionAction)

    fun logAgentState() {
        log.append("${isopod.x}, ${isopod.y}," +
                "${neuronLeftSensor.activation},${neuronRightSensor.activation}" +
                "${neuronLeftTurning.activation},${neuronRightTurning.activation}" +
                ",${neuronStraight.activation}\n")
    }

    //empty.tmx (413, 10, 600, 600)
    //Network (4, 187, 400, 400)
    //Control Panel (5, 10, 143, 173)

    withGui {
        isopod.select()
        createControlPanel("Control Panel", 130, 15) {

            suspend fun runTrials() {
                // Reset before starting trials
                log.clear() // Clear log to prevent memory accumulation
                
                var iteration = 0
                while (trialNum < defaultNumTrials) {
                    log.append("# Trial: ${trialNum + 1}\n")
                    resetIsopod()
                    log.append("# Heading: ${isopod.heading}\n")
                    workspace.iterateWhile {
                        if (!collision) {
                            logAgentState()
                        }
                        !collision && ++iteration < maxIterationsPerTrial
                    }
                    trialNum++
                    iteration = 0
                    collision = false
                }
                trialNum = 0
            }

            suspend fun useSpeedInhibition() {
                if (leftSpeedWeight == null) {
                    with(network) {
                        leftSpeedWeight = connect(neuronLeftSensor, neuronStraight, -1.0, -50.0, 50.0)
                        rightSpeedWeight = connect(neuronRightSensor, neuronStraight, -1.0, -50.0, 50.0)
                    }
                } else {
                    leftSpeedWeight?.delete()
                    leftSpeedWeight = null
                    rightSpeedWeight?.delete()
                    rightSpeedWeight = null
                }
            }

            addButton("Run one trial") {
                // Reset for single trial
                resetIsopod()
                log.clear() // Clear log to prevent memory accumulation
                
                var iteration = 0
                workspace.launch {
                    log.append("# Heading: ${isopod.heading}\n")
                    while (++iteration < maxIterationsPerTrial) {
                        workspace.iterateSuspend(1)
                        if (collision) {
                            break
                        } else {
                            logAgentState()
                        }
                    }
                    collision = false
                    
                    showSaveDialog("", "singleTrial.csv") {
                        writeText(log.toString())
                    }
                }
            }

            addTextField("Number of trials", "" + defaultNumTrials) {
                it.toIntOrNull()?.let { num ->
                    defaultNumTrials = num
                }
            }

            addButton("Run trials") {
                workspace.launch {
                    runTrials()
                    showSaveDialog("", "isopodData.csv") {
                        writeText(log.toString())
                    }
                }
            }

            // Uncomment below to create a custom button with a bespoke sim
            //addButton("Custom Sim") {
            //    log = "# Velocity = 1, stdev = .01\n"
            //    neuronStraight.bias = 1.0
            //    noiseSource = NormalDistribution(.01, .9)
            //    runTrials()
            //    log += "# Velocity = 2, stdev = .03\n"
            //    neuronStraight.bias = 2.0
            //    noiseSource = NormalDistribution(.03, .9)
            //    runTrials()
            //    showSaveDialog("", "isopodData.csv") {
            //        writeText(log)
            //    }
            //}

            addButton("Speed inhibition") {
                workspace.launch {
                    useSpeedInhibition()
                }
            }
        }

    }

    addSidebarInfo(
        """ 
        # Isopod Simulation
        
        This simulation explores the reactive behaviors in the isopod from sensorimotor connections. The isopod reacts to its environment, the fish stimuli, based on connections between 
        its sensors and actuators. The sensors detect the stimuli, which determine the strength and direction of movement, and the actuators control the actual movement of the vehicles.
        
        # Simulation Details
        
        In this simulation, the `Left` and `Right` neurons receive activation when the isopod detects stimuli. These activations are sent to the actuator neurons which controls the isopod's motor actions. The magnitude 
        of the weights indicate the strength of the connection between the detector neurons and the actuator neurons. The weight strength influences the magnitude of the isopod's action. For example, with a higher
        weight strength, it increases the likelihood and intensity of the action (e.g., turn left, turn right).
        
        Unlike the other Braitenberg simulations, this simulation has a `Speed inhibition` button which creates synaptic connections between the isopod's velocity to the sensory inputs, creating a connection between
        the `Left` and `Right` neurons to the `Straight` neuron. This allows the isopod to make more _accurate_ actions, speeding up in low-stimulus areas, and slowing down when near the fish.
        
        The graphs are showing multiple trials where we place the isopod in the center of the world, let it go, and see what it does. Each trial can either terminate in it obtaining food, hitting a wall, or the max
        trials running out. The bias controls its speed in these sims (prob something to change) and so generally these guys aren't finding the food, which is in the four corners.
        
        # What to Do
        
        Below is the general method to use this simulation:
        
        1) Click `Run one trial` to see how the isopod reacts to its environment. To run multiple trials, click `Run trials`.
        
        2) Observe the isopod's behavior.
        
        3) To make it have more naturalistic behaviors, click `Speed inhibition`. 
        
        4) Observe the difference in behaviors.

        5) Save the results.
        
        6) Analyze the results in a statistical computing environment.


        # References
    
        Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT press.
        
        Hotton, S., & Yoshimi, J. (2024). [_The Open Dynamics of Braitenberg Vehicles_](https://mitpress.mit.edu/9780262548199/the-open-dynamics-of-braitenberg-vehicles/). MIT Press.
        
        # Credits
        
        Jasmine Lau
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        Kanly Thao

        """.trimIndent()
    )

    // Parse optionString and run in headless mode if provided
    if (optionString?.isNotEmpty() == true) {
        val options = JSONObject(optionString)

        // Parse parameters
        val numTrials = options.optInt("numTrials", defaultNumTrials)
        val enableSpeedInhibition = options.optBoolean("speedInhibition", false)
        val maxSteps = options.optInt("maxIterationsPerTrial", maxIterationsPerTrial)

        // Apply speed inhibition if requested
        if (enableSpeedInhibition && leftSpeedWeight == null) {
            with(network) {
                leftSpeedWeight = connect(neuronLeftSensor, neuronStraight, -1.0, -50.0, 50.0)
                rightSpeedWeight = connect(neuronRightSensor, neuronStraight, -1.0, -50.0, 50.0)
            }
        }

        println("\n=== Isopod Simulation Started (Headless Mode) ===")
        println("Number of trials: $numTrials")
        println("Max iterations per trial: $maxSteps")
        println("Speed inhibition: $enableSpeedInhibition")
        println("Hit radius: $hitRadius")
        println("Smell dispersion: $smellDispersion")
        println("Speed: $speed")
        println("=" + "=".repeat(48))

        val csvData = StringBuilder()
        csvData.append("# Isopod Simulation Results\n")
        csvData.append("# Trials: $numTrials, MaxSteps: $maxSteps, SpeedInhibition: $enableSpeedInhibition\n")
        csvData.append("# HitRadius: $hitRadius, SmellDispersion: $smellDispersion, Speed: $speed\n")
        csvData.append("#\n")

        var successCount = 0
        var wallCollisionCount = 0
        var timeoutCount = 0

        for (trial in 1..numTrials) {
            csvData.append("# Trial: $trial\n")
            resetIsopod()
            csvData.append("# Heading: ${isopod.heading}\n")

            collision = false
            var iteration = 0
            var outcome = "timeout"

            while (!collision && iteration < maxSteps) {
                workspace.iterateSuspend(1)

                // Log state before checking collision
                csvData.append("${isopod.x}, ${isopod.y}," +
                        "${neuronLeftSensor.activation},${neuronRightSensor.activation}," +
                        "${neuronLeftTurning.activation},${neuronRightTurning.activation}" +
                        ",${neuronStraight.activation}\n")

                iteration++
            }

            // Determine outcome based on collision type
            if (collision) {
                // Check if we hit a fish or wall
                val hitFish = odorWorld.entityList
                    .filter { it.entityType == EntityType.Fish }
                    .any { fish -> fish.location.distance(isopod.location) < hitRadius }

                if (hitFish) {
                    outcome = "fish"
                    successCount++
                } else {
                    outcome = "wall"
                    wallCollisionCount++
                }
            } else {
                timeoutCount++
            }

            println("Trial $trial: Steps=$iteration, Outcome=$outcome")
            collision = false
        }

        println("\n" + "=".repeat(50))
        println("=== Simulation Complete ===")
        println("Total trials: $numTrials")
        println("Fish collisions: $successCount (${String.format("%.1f", successCount.toDouble() / numTrials * 100)}%)")
        println("Wall collisions: $wallCollisionCount (${String.format("%.1f", wallCollisionCount.toDouble() / numTrials * 100)}%)")
        println("Timeouts: $timeoutCount (${String.format("%.1f", timeoutCount.toDouble() / numTrials * 100)}%)")
        println("=".repeat(50))

        // Export to CSV
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(java.util.Date())
        val outputDir = java.io.File("simulation_outputs")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val filename = "simulation_outputs/isopod_sim_$timestamp.csv"
        java.io.File(filename).writeText(csvData.toString())
        println("\nData exported to: $filename\n")
    }

}

