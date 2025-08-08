package org.simbrain.custom_sims.simulations.braitenberg

import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.util.getDesktopComponentAs
import org.simbrain.util.graphicalUpperBound
import org.simbrain.util.place
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.fitWorldToFrameSize
import java.awt.geom.Point2D
import kotlin.math.max

/**
 * Testing
 */
val braitenbergGame = newSim {

    var leftWeight = 1.0
    var rightWeight = 1.0
    var velocity = .1

    workspace.clearWorkspace()

    val oc = addOdorWorldComponent("Obstacle Course")
    //oc.world.isObjectsBlockMovement = false
    oc.world.isUseCameraCentering = false
    oc.world.addEntity(257, 191, EntityType.Poison)
    oc.world.addEntity(323, 286, EntityType.Poison)
    oc.world.addEntity(398, 335, EntityType.Poison)
    oc.world.addEntity(500, 184, EntityType.Swiss)

    val vehicle1 = oc.world.createVehicle("Vehicle 1", EntityType.Circle, EntityType.Swiss, Point2D.Double(194.0, 407.0))

    withGui {
        place(vehicle1.networkComponent, 53, 282, 359, 327)
        place(oc, 462, 19, 600, 600)
        oc.getDesktopComponentAs<OdorWorldDesktopComponent>().fitWorldToFrameSize()
    }

    withGui {
        createControlPanel("Control Panel", 64, 38) {
            // Update neuron and weight bounds to reasonable values given weight values
            fun updateBounds(w1: Double, w2: Double) {
                val bound = graphicalUpperBound(max(w1, w2))
                vehicle1.leftSynapse.upperBound = bound
                vehicle1.rightSynapse.upperBound = bound
                vehicle1.leftSynapse.lowerBound = -bound
                vehicle1.rightSynapse.lowerBound = -bound
                vehicle1.leftTurn.upperBound = bound
                vehicle1.rightTurn.upperBound = bound
                vehicle1.leftTurn.lowerBound = -bound
                vehicle1.rightTurn.lowerBound = -bound
            }
            fun updateVehicle() {
                vehicle1.leftSynapse.strength = leftWeight
                vehicle1.rightSynapse.strength = rightWeight
                vehicle1.straight.activation = velocity
                updateBounds(leftWeight, rightWeight)
            }
            addSlider("Left weight", -10.0, 10.0, 1.0, .01) {
                leftWeight = it
                updateVehicle()
            }
            addSlider("Right weight", -10.0, 10.0, 1.0, .01) {
                rightWeight = it
                updateVehicle()
            }
            addSlider("Velocity", -10.0, 10.0, .02, .01) {
                velocity = it
                updateVehicle()
            }
            updateVehicle()

        }
    }

    addSidebarInfo(
    """     
    # Introduction

    [Braitenberg vehicles](http://en.wikipedia.org/wiki/Braitenberg_vehicle) are simple agents that move around in response to environmental stimuli. This simulation simulates a Braitenberg vehicle as a controllable
    agent that can be tweaked to avoid dangerous obstacles and pursue a target.      

    # Simulation Details
    
    In this simulation, the Braitenberg vehicle is controlled through a control panel rather than being a preset Braitenberg vehicle (e.g., the `Pursuer`/`Avoider` simulations). In the odor world,
    there are `4` objects, `3` of which are obstacles that the agent has to avoid or go around to get the target (e.g., cheese).
    
    The agent in this simulation, can have both characteristics of the _aggressive vehicle_ and the _coward vehicle_, which were explored in the `Pursuer` and `Avoider` simulations independently. This is as a result
    of the agent's parameters (e.g., weight strengths and velocity) being changed in real-time as the simulation runs.
    
    # What to Do
    
     In this simulation similar to the other Braitenberg simulations, simply press the `play` button on the top toolbar for the simulation to run. Below are the steps:
        
    1) While it runs, tweak the parameters of the agent to avoid the obstacles and get to the cheese.
    
    2) Continue doing so until the agent has approached the cheese.
    
    3) Press the `stop` button to stop the network from running.

    # References
        
    Braitenberg, V. (1986). [_Vehicles: Experiments in synthetic psychology_](https://mitpress.mit.edu/9780262521123/vehicles/). MIT press.
        
    Hotton, S., & Yoshimi, J. (2024). [_The Open Dynamics of Braitenberg Vehicles_](https://mitpress.mit.edu/9780262548199/the-open-dynamics-of-braitenberg-vehicles/). MIT Press.

    # Credits
    
    [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
    
    Kanly Thao
            
    """.trimIndent()
    )

}





