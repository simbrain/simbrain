package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.connect
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.showSaveDialog
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.SmellSensor

/**
 * A simulation of Isopod navigation. With Peter Hinow and Kaiden Schmidt.
 */
val isopodSim = newSim {

    workspace.clearWorkspace()

    // Adjustible parameters for sim
    var numTrials = 5
    var trialNum = 1
    val maxIterationsPerTrial = 5000

    var log = ""

    // ----- Network construction ------

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val noiseSource = NormalDistribution(1.0, .9)

    val neuronLeftSensor = network.addNeuron {
        location = point(0, 100)
        upperBound = 10.0
        label = "Left"
        with (updateRule as LinearRule) {
            noiseGenerator = noiseSource
            addNoise = true
        }
    }
    val neuronRightSensor = network.addNeuron {
        location = point(100, 100)
        upperBound = 10.0
        label = "Right"
        with (updateRule as LinearRule) {
            noiseGenerator = noiseSource
            addNoise = true
        }
    }
    val neuronLeftTurning = network.addNeuron {
        location = point(0, 0)
        upperBound = 10.0
        label = "Turn Left"
    }
    val neuronRightTurning = network.addNeuron {
        location = point(100, 0)
        upperBound = 10.0
        label = "Turn Right"
    }
    val neuronStraight = network.addNeuron {
        location = point(50, 0)
        upperBound = 10.0
        label = "Straight"
        (neuronDataHolder as BiasedScalarData).bias = 10.0
    }

    connect(neuronLeftSensor, neuronLeftTurning, 4.0)
    connect(neuronRightSensor, neuronRightTurning, 4.0)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(180, 10)
            width = 400
            height = 400
        }
    }

    // ----- Build 2d World ------

    val odorWorldComponent = addOdorWorldComponent()
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
        isObjectsBlockMovement = false

        tileMap = TileMap(20, 20)
        tileMap.fill(2)

        // Body could be represented by a triangle or rhombus
        isopod = addEntity(300, 300, EntityType.MOUSE).apply {
            name = "isopod"
            heading = 90.0
            addDefaultEffectors()
            straightMovement = effectors[0]
            turnLeft = effectors[1]
            turnRight = effectors[2]

            // Can add more smell sensors here
            // Options: new sensor in back; triangular array
            leftSensor = SmellSensor().apply {
                radius = 40.0
                theta = Math.PI / 4
                addSensor(this)
            }
            rightSensor = SmellSensor().apply {
                radius = 40.0
                theta = -Math.PI / 4
                addSensor(this)
            }
            events.onCollided {
                if (it is OdorWorldEntity) {
                    log += "Collided with ${it.name}\n"
                } else {
                    log += "Collided with wall\n"
                }
                collision = true
            }
            manualMovement.manualStraightMovementIncrement = 2.0
            manualMovement.manualMotionTurnIncrement = 2.0
        }

        fun addFish(x: Int, y: Int) {
            odorWorld.addEntity(x, y, EntityType.FISH).apply {
                name = "Fish"
                smellSource = SmellSource.createScalarSource(1).apply {
                    dispersion = 300.0
                }
            }
        }

        addFish(590, 590)
        addFish(10, 590)
        addFish(10, 10)
        addFish(590, 10)

        workspace.addUpdateAction(updateAction("Track location") {
            log += "${isopod}\n"
        })

    }

    withGui {
        place(odorWorldComponent) {
            location = point(590, 10)
            (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).setGuiSizeToWorldSize()
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
        isopod.location = point(300,300)
        isopod.heading = UniformRealDistribution(0.0,360.0).sampleDouble()
    }

    withGui {
        createControlPanel("Control Panel", 5, 10) {

            val numTrialsTF = addTextField("Number of trials", "" + numTrials)

            addButton("Run all trials") {
                var iteration = 0
                numTrials = Integer.parseInt(numTrialsTF.text)
                log = "Trial: $trialNum\n"
                resetIsopod()
                log += "Heading: ${isopod.heading}\n"
                while(trialNum < numTrials) {
                    while (++iteration < maxIterationsPerTrial && !collision) {
                        workspace.simpleIterate()
                    }
                    trialNum++
                    collision = false
                }
                trialNum = 0
                showSaveDialog("", "trialData.txt") {
                    writeText(log)
                }
            }
        }

    }

    // addDocViewer("Test", "Braitenberg.html").apply {
    //     place(this) {
    //         location = point(145, 421)
    //         width = 400
    //         height = 330
    //     }
    // }

}
