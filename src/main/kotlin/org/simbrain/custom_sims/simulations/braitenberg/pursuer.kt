package org.simbrain.custom_sims.simulations.braitenberg

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import org.simbrain.world.odorworld.sensors.SmellSensor

val pursuer = newSim {
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
        location = point(34.0, 102.6)
        label = "Left sensor"
    }
    
    val rightSensor = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 1.0
            lowerBound = -1.0
        }
        location = point(165.0, 102.6)
        label = "Right Sensor"
    }
    
    val turnRight = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 150.0
            lowerBound = -1.0
        }
        location = point(165.0, 11.6)
        label = "Turn Right"
    }
    
    val straight = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 10.0
            lowerBound = -1.0
            activation = 0.5
            clamped = true
        }
        location = point(99.0, 11.6)
        label = "Straight"
    }
    
    val turnLeft = network.addNeuron {
        updateRule = LinearRule().apply {
            upperBound = 150.0
            lowerBound = -1.0
        }
        location = point(34.0, 11.6)
        label = "Turn Left"
    }
    
    // Create synapses - from Network6.xml
    network.addSynapse(leftSensor, turnLeft) {
        strength = 10.0
        upperBound = 200.0
        lowerBound = -10.0
    }
    
    network.addSynapse(rightSensor, turnRight) {
        strength = 10.0
        upperBound = 200.0
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
    val mouse = odorWorld.addEntity(157, 293, EntityType.Mouse).apply {
        heading = 81.56
        name = "Agent_1"
    }
    
    // Add smell sensors at specific angles
    val leftSmellSensor = SmellSensor().apply {
        theta = 22.5
        radius = 50.0
        label = "Smell-Left"
    }
    mouse.addSensor(leftSmellSensor)
    
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
    }
    
    leftTurn.apply {
        label = "Go-left"
    }
    
    rightTurn.apply {
        label = "Go-right"
    }
    
    // Add cheese (target)
    val cheese = odorWorld.addEntity(174, 87, EntityType.Swiss).apply {
        name = "Entity_2"
        smellSource = SmellSource(doubleArrayOf(0.7, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)).apply {
            dispersion = 200.0
            decayFunction = LinearDecayFunction()
        }
    }
    
    // Add documentation sidebar
    addSidebarInfo(
        """
        # Pursuer

        This simulation is similar to the avoider simulation relating to [Braitenberg vehicles](http://en.wikipedia.org/wiki/Braitenberg_vehicle). Braitenbeg vehicles are simple agents that move around in response to environmental stimuli. 

        Unlike the avoider simulation in which the mouse agent (vehicle) orients away from the source, this simulation will demonstrate movement similar to vehicle 2b (the "aggressive" vehicle) from Braitenberg's book (see link below).

        The mouse will move to the left if its left sensor is activiated by a source on its left, heading straight towards the source. And if it senses a source on its right, the mouse will move to the right and straight towards the source. 

        ## Running This Simulation
        Simply press the play button on the top toolbar and observe the behavior of the mouse in the odor world window.
        
        Move the cheese around and watch the mouse chase it. Take note of the activity of the neurons in the nework window. Press the stop button to stop the network from running.

        ### References
        
        1) Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT press.
            
        2) Hotton, S., & Yoshimi, J. (2024). [_The Open Dynamics of Braitenberg Vehicles_](https://mitpress.mit.edu/9780262548199/the-open-dynamics-of-braitenberg-vehicles/). MIT Press.

        ### Credits
        
        Jeff Yoshimi and Saraching Chao.
        
        """.trimIndent()
    )
    
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