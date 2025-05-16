package org.simbrain.custom_sims.simulations.braitenberg

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import org.simbrain.world.odorworld.sensors.SmellSensor

val avoider = newSim {
    // Clear workspace
    workspace.clearWorkspace()
    
    // Create network component
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    
    // Create neurons - from Network6.xml
    val leftSensor = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 1.0
            lowerBound = -1.0
        }
        location = point(41.1, 87.2)
        label = "Left sensor"
    }
    
    val rightSensor = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 1.0
            lowerBound = -1.0
        }
        location = point(159.7, 89.9)
        label = "Right Sensor"
    }
    
    val turnLeft = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 40.0
            lowerBound = -1.0
        }
        location = point(37.6, 11.2)
        label = "Turn Left"
    }
    
    val straight = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 10.0
            lowerBound = -1.0
            increment = 0.25
            activation = 0.5
            clamped = true
        }
        location = point(96.6, 10.7)
        label = "Straight"
    }
    
    val turnRight = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 40.0
            lowerBound = -1.0
        }
        location = point(159.8, 15.6)
        label = "Turn Right"
    }
    
    // Create synapses - from Network6.xml
    // Note that the connections in the avoider configuration are cross-coupled
    // Left sensor connects to right turn, and right sensor connects to left turn
    network.addSynapse(leftSensor, turnRight) {
        strength = 45.0
        upperBound = 100.0
        lowerBound = -10.0
    }
    
    network.addSynapse(rightSensor, turnLeft) {
        strength = 45.0
        upperBound = 100.0
        lowerBound = -10.0
    }
    
    // Create odor world component
    val odorWorldComponent = addOdorWorldComponent("OdorWorld")
    val odorWorld = odorWorldComponent.world
    
    // Configure odor world - from OdorWorld1.xml
    odorWorld.apply {
        wrapAround = true
        isObjectsBlockMovement = false
    }
    
    // Create mouse agent
    val mouse = odorWorld.addEntity(269, 127, EntityType.Mouse).apply {
        heading = 155.4
        name = "Agent_1"
    }
    
    // Add smell sensors at specific angles
    val leftSmellSensor = SmellSensor().apply {
        theta = 22.5
        radius = 50.0
        label = "Smell-Left"
    }
    mouse.addSensor(leftSmellSensor)
    
    val centerSmellSensor = SmellSensor().apply {
        theta = 0.0
        radius = 0.0
        label = "Smell-Center"
    }
    mouse.addSensor(centerSmellSensor)
    
    val rightSmellSensor = SmellSensor().apply {
        theta = -22.5
        radius = 50.0
        label = "Smell-Right"
    }
    mouse.addSensor(rightSmellSensor)
    
    // Add effectors to mouse
    mouse.addDefaultEffectors()
    
    // Get effector references
    val straightMovement = mouse.effectors[0] as StraightMovement
    val leftTurn = mouse.effectors[1] as Turning
    val rightTurn = mouse.effectors[2] as Turning
    
    // Configure effectors
    straightMovement.apply {
        label = "Go-straight"
        scalingFactor = 1.0
        amount = 2.75
    }
    
    leftTurn.apply {
        label = "Go-left"
        amount = 0.0
    }
    
    rightTurn.apply {
        label = "Go-right"
        amount = 0.0
    }
    
    // Add cheese objects with smell sources
    val swissCheese = odorWorld.addEntity(170, 309, EntityType.Swiss).apply {
        smellSource = SmellSource(doubleArrayOf(0.7, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)).apply {
            dispersion = 70.0
            decayFunction = GaussianDecayFunction()
        }
    }
    
    val goudaCheese = odorWorld.addEntity(153, 48, EntityType.Gouda).apply {
        smellSource = SmellSource(doubleArrayOf(0.7, 0.0, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0)).apply {
            dispersion = 70.0
            decayFunction = GaussianDecayFunction()
        }
    }
    
    val blueCheese = odorWorld.addEntity(112, 175, EntityType.BlueCheese).apply {
        smellSource = SmellSource(doubleArrayOf(0.7, 0.0, 0.0, 0.0, 0.3, 0.0, 0.0, 0.0)).apply {
            dispersion = 70.0
            decayFunction = GaussianDecayFunction()
        }
    }
    
    // Add documentation sidebar
    addSidebarInfo("""
        # Avoider

        [Braitenberg vehicles](http://en.wikipedia.org/wiki/Braitenberg_vehicle) are simple agents that move around in response to environmental stimuli. This simulation will demonstrate movement similar to vehicle 2a (the "coward" vehicle) from Braitenberg's book, "Vehicles: Experiments in Synthetic Psychology," which can be downloaded from the link below.

        ## Running This Simulation
        Simply press the play button on the top toolbar and observe the behavior of the mouse in the odor world window, while taking note of the activity of the neurons in the nework window. Feel free to grab the cheese and move them towards the mouse. The mouse should avoid the cheese. Press the stop button to stop the network from running.

        ## Relation to Braitenberg
        Note the connections are not the same as in Braitenberg's book. The mouse agent (vehicle) will orient to the left if its right sensor is activiated by a source on its right, moving away from the source, and if it senses a source on its left, the mouse will orient to the right and away from the source.

        ## Reference
        Braitenberg, V. (1986). [Vehicles: Experiments in synthetic psychology](http://www1.appstate.edu/~kms/classes/psy5150/Documents/Braitenberg1984.pdf). MIT press.

        ## Credits
        Jeff Yoshimi and Saraching Chao.
    """.trimIndent())
    
    // GUI layout
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
        
        place(odorWorldComponent) {
            location = point(400, 0)
            width = 400
            height = 435
        }
        odorWorldComponent.getDesktopComponentAs<OdorWorldDesktopComponent>().fitWorldToFrameSize()

    }
    
    // Create couplings - from contents.xml
    with(couplingManager) {
        // Neuron outputs to effectors
        turnRight couple rightTurn
        turnLeft couple leftTurn
        straight couple straightMovement
        
        // Sensors to neurons
        leftSmellSensor couple leftSensor
        rightSmellSensor couple rightSensor
    }
} 