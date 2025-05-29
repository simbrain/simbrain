package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.connect
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.showSaveDialog
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.updater.updateAction
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.SmellSensor

/**
 * A simulation of Isopod navigation. With Peter Hinow and Kaiden Schmidt.
 */
val isopodSim = newSim {

    // Adjustable parameters for sim
    var defaultNumTrials = 5
    val maxIterationsPerTrial = 5000
    val hitRadius = 80

    // Other variables
    var log = ""
    var trialNum = 0

    // Clear the workspace
    workspace.clearWorkspace()

    // ----- Network construction ------

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val noiseSource = NormalDistribution(1.0, .9)

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
        label = "Turn Left"
    }
    val neuronRightTurning = network.addNeuron {
        location = point(100, 0)
        upperBound = 150.0
        label = "Turn Right"
    }
    val neuronStraight = network.addNeuron {
        location = point(50, 0)
        lowerBound = 0.0
        upperBound = 10.0
        label = "Straight"
        bias = 5.0
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
            location = point(4, 187)
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
                    log += "# Collided with wall\n"
                }
                collision = true
            }
            manualMovement.manualStraightMovementIncrement = 2.0
            manualMovement.manualMotionTurnIncrement = 2.0
        }

        fun addFish(x: Double, y: Double) {
            odorWorld.addEntity(x, y, EntityType.Fish).apply {
                name = "Fish"
                // Smell value when agent is right on top of fish
                val maxVal = 1.1
                smellSource = SmellSource.createScalarSource(maxVal).apply {
                    // How the smell decays with distances
                    decayFunction = LinearDecayFunction()
                    decayFunction.peakDistance = 0.0
                    decayFunction.dispersion = 350.0
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
            location = point(413, 10)
            width = 600
            height = 600
        }
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

    workspace.addUpdateAction(updateAction("Found fish") {
        val foundFish = odorWorld.entityList
            .filter { it.entityType == EntityType.Fish }
            .any { fish -> fish.location.distance(isopod.location) < hitRadius }
        if (foundFish) {
            log += "# Collided with fish\n"
            collision = true
        }
    })

    fun logAgentState() {
        log += "${isopod.x}, ${isopod.y}," +
                "${neuronLeftSensor.activation},${neuronRightSensor.activation}" +
                "${neuronLeftTurning.activation},${neuronRightTurning.activation}" +
                ",${neuronStraight.activation}\n"
    }

    //empty.tmx (413, 10, 600, 600)
    //Network (4, 187, 400, 400)
    //Control Panel (5, 10, 143, 173)

    withGui {
        createControlPanel("Control Panel", 5, 10) {

            addButton("Run one trial") {
                resetIsopod()
                log = ""
                var iteration = 0
                workspace.launch {
                    log += "# Heading: ${isopod.heading}\n"
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
                        writeText(log)
                    }
                }
            }

            addTextField("Number of trials", "" + defaultNumTrials) {
                it.toIntOrNull()?.let { num ->
                    defaultNumTrials = num
                }
            }

            addButton("Run trials") {
                log = ""
                var iteration = 0
                while (trialNum < defaultNumTrials) {
                    log += "# Trial: ${trialNum + 1}\n"
                    resetIsopod()
                    log += "# Heading: ${isopod.heading}\n"
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
                showSaveDialog("", "multipleTrials.csv") {
                    writeText(log)
                }
            }

            addButton("Speed inhibition") {
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
        }

    }

    addSidebarInfo(
        """ 
            # Introduction
            The Braitenberg Isopod Simulation explores the reactive behaviors in agent, the isopod, from sensorimotor connections. The isopod reacts to its environment, the fish stimuli, based on connections between its sensors and actuators. The sensors detect the stimuli, which determine the strength and direction of movement, and the actuators control the actual movement of the vehicles.
            
            The graphs are showing multiple trials where we place the isopod in the center of the world, let it go, and see what it does. Each trial can either terminate in it obtaining food, hitting a wall, or the max trials running out. The bias controls its speed in these sims (prob something to change)  and so generally these guys aren’t finding the food, which is in the four corners.
            # What to Do
            1. Click "Run one trial" to see how the isopod reacts to its environment. One trial of this simulation consists of the isopod moving in the "empty.tmx" window, as it moves towards the fish. The trial ends either when the isopod reaches the bounds of the window or the fish.
            2. The weights in the "Network" window changes according to the isopod's actions. 
                - The "Left" and "Right" weights are the inputs that detect the stimuli in the environment of the "empty.tmx" window.
                - The "Turn Left", "Straight", and "Turn Right" weights are the outputs of the isopod's motor actions, steering it.
                - The magnitude of the weights indicate the strength of the connection and influence the output has on the isopod's actions, with a higher weight increasing the likelihood and intensity of the action.
            3. "Run trials" to observe the agent in its environment.
            4. Click "Speed inhibition" to link the speed to the sensory input, creating a connection between "Left", "Right", and "Straight". 
                - This allows for the isopod to make more "accurate" actions, speeding up in low-stimulus areas, and slowing down when near the fish. 
        """.trimIndent()
    )

    // addDocViewer("Test", "Braitenberg.html").apply {
    //     place(this) {
    //         location = point(145, 421)
    //         width = 400
    //         height = 330
    //     }
    // }

}
