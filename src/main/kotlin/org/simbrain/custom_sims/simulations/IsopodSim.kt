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
import org.simbrain.util.toCsvString
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

    // x coordinate, y coordinate
    // TODO success v failure (0/1), time of success vs. failure
    val trialData = mutableListOf<DoubleArray>()

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
        (neuronDataHolder as BiasedScalarData).bias = 2.0
    }

    connect(neuronLeftSensor, neuronLeftTurning, 4.0)
    connect(neuronRightSensor, neuronRightTurning, 4.0)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(145, 10)
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

    var fishCollision = false

    odorWorld.apply {

        wrapAround = false
        isObjectsBlockMovement = false

        tileMap = TileMap(20, 20)
        tileMap.fill(2)

        // Body could be represented by a triangle or rhombus
        isopod = addEntity(300, 300, EntityType.MOUSE).apply {
            heading = 90.0
            addDefaultEffectors()
            straightMovement = effectors[0]
            turnLeft = effectors[1]
            turnRight = effectors[2]

            // Can add more smell sensors here
            // Options: new sensor in back; triangular array
            leftSensor = SmellSensor(this).apply {
                radius = 40.0
                theta = Math.PI / 4
                addSensor(this)
            }
            rightSensor = SmellSensor(this).apply {
                radius = 40.0
                theta = -Math.PI / 4
                addSensor(this)
            }
            manualStraightMovementIncrement = 2.0
            manualMotionTurnIncrement = 2.0
        }

        fun addFish(x: Int, y: Int) {
            odorWorld.addEntity(x, y, EntityType.FISH).apply {
                smellSource = SmellSource.createScalarSource(1).apply {
                    dispersion = 300.0
                }
                onCollide {
                    fishCollision = true
                    // TODO: Magic numbers for fish collision
                    trialData.add(doubleArrayOf(-1.0, 1.0))
                }
            }
        }

        addFish(590, 590)
        addFish(10, 590)
        addFish(10, 10)
        addFish(590, 10)

        workspace.addUpdateAction(updateAction("Track location") {
            trialData.add(isopod.location);
        })

    }

    withGui {
        place(odorWorldComponent) {
            location = point(530, 10)
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

    withGui {
        createControlPanel("Control Panel", 5, 10) {
            addButton("Run one trial") {
                var iteration = 0
                trialData.clear()
                while (++iteration < 1000 && !fishCollision) {
                    workspace.iterate()
                }
                fishCollision = false
                // println("TrialData: ${trialData}")
                showSaveDialog("", "trialData.csv") {
                    writeText(trialData.toCsvString())
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