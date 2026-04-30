package org.simbrain.custom_sims.simulations.demos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.place
import org.simbrain.world.odorworld.OdorWorldPreferences
import org.simbrain.world.odorworld.behaviors.Evade
import org.simbrain.world.odorworld.behaviors.Pursue
import org.simbrain.world.odorworld.behaviors.Wander
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.View3DSensor
import java.awt.Dimension
import java.awt.Graphics
import java.io.File
import javax.swing.JPanel

val npcSteeringDemo = newSim {

    workspace.clearWorkspace()

    val odorWorldComponent = addOdorWorldComponent("NPC Basic Demo")
    val odorWorld = odorWorldComponent.world.apply {
        tileMap = loadTileMap(File(OdorWorldPreferences.tileMapDirectory, "yulins_world.tmx"))
        wrapAround = true
        isObjectsBlockMovement = true
    }

    val movingEntities = mutableListOf<org.simbrain.world.odorworld.entities.OdorWorldEntity>()

    // Mouse: pursues Swiss cheese at max speed 1.5
    val mouse = odorWorld.addEntity(60, 200, EntityType.Mouse).apply {
        name = "Mouse"
        heading = 0.0
        behavior = Pursue().apply {
            targetType = EntityType.Swiss
            maxSpeed = 1.5
            visionRange = 400.0
        }
    }
    movingEntities.add(mouse)

    // Add 3D view sensor to the mouse
    val view3dSensor = View3DSensor().apply {
        label = "Mouse View"
        fov = 90.0
        viewDistance = 400.0
        outputWidth = 200
        outputHeight = 200
        horizonPosition = 0.5
        cameraWorldHeight = 16.0
        wallHeight = 2.0
        billboardSprites = true
    }
    mouse.addSensor(view3dSensor)

    // Two Swiss cheeses: evade the Mouse at max speed 2.0
    movingEntities.add(odorWorld.addEntity(440, 200, EntityType.Swiss).apply {
        name = "Swiss A"
        behavior = Evade().apply {
            threatType = EntityType.Mouse
            maxSpeed = 2.0
            visionRange = 300.0
        }
    })
    movingEntities.add(odorWorld.addEntity(60, 440, EntityType.Swiss).apply {
        name = "Swiss B"
        behavior = Evade().apply {
            threatType = EntityType.Mouse
            maxSpeed = 2.0
            visionRange = 300.0
        }
    })

    // Wandering Fish — no target/threat, just roams around obstacles
    movingEntities.add(odorWorld.addEntity(440, 440, EntityType.Fish).apply {
        name = "Fish"
        behavior = Wander().apply {
            maxSpeed = 1.2
        }
    })

    // Static obstacles (no behavior). Their AABBs block movement and show up as
    // magenta hits in the steering debug overlay of nearby moving agents.
    odorWorld.addEntity(220, 200, EntityType.Pansy).apply { name = "Pansy" }
    odorWorld.addEntity(220, 380, EntityType.Tulip).apply { name = "Tulip" }

    addSidebarInfo(
        """
        # NPC Steering Demo

        A Mouse pursues two Swiss cheeses while they evade it. A Fish wanders
        independently. A Pansy and a Tulip sit still as obstacles.

        # What to Do

        Press the play button. Watch the Mouse track down the cheeses, the cheeses
        flee in different directions, and the Fish meander around the obstacles
        without bumping them. To simplify the setup, delete some entities by
        selecting them and pressing `Delete`.

        Use the control panel to toggle the **steering debug overlay** and see how
        each agent is making decisions. You can also toggle tilemap layer visibility
        to simplify the view.

        # Reading the Debug Overlay

        When enabled, each agent draws:

        - **Colored rays** from its center, one per candidate heading.
            - **Green** = a direction the behavior likes (positive score).
            - **Red** = a direction it dislikes (negative score).
            - **Yellow + thicker** = the chosen heading this tick.
        - **Magenta dots** mark where the obstacle feeler ray hits a wall or entity.
        - **Status text** under the agent shows the active behavior and speed info.

        # Things to Try

        - Drag a cheese close to the mouse — watch it react.
        - Drag the Pansy or Tulip into the cheese's escape path.
        - Open an entity's property dialog (right-click → Properties) and tweak
          behavior parameters like `Max Speed`, `Vision Range`, or `Num Rays`.

        # How It Works

        Each behavior uses **context steering**: every tick it samples N candidate
        headings around the agent, scores each by interest minus danger, and picks
        the best. See `world/odorworld/behaviors/`.
        """.trimIndent(),
        width = 350,
        initiallyOpened = true
    )

    withGui {
        place(odorWorldComponent, 0, 0, 550, 550)

        val viewPanel = object : JPanel() {
            init {
                preferredSize = Dimension(474, 544)
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val image = view3dSensor.renderedImage
                if (image != null) {
                    g.drawImage(image, 0, 0, width, height, null)
                } else {
                    g.drawString("No image yet", 10, height / 2)
                }
            }
        }

        createControlPanel("Control Panel", 11, 537) {
            addCheckBox("Show steering debug", false) { enabled ->
                movingEntities.forEach {
                    it.showSteeringDebug = enabled
                    if (enabled) it.behavior.update(it)
                    it.events.updated.fire()
                }
            }
            addCheckBox("Show tilemap", true) { visible ->
                odorWorld.tileMap.layers.forEach { it.visible = visible }
            }
        }

        val view3dPanel = createControlPanel("Mouse 3D View", 546, 5) {
            addComponent(viewPanel)
        }

        view3dSensor.update(mouse)

        view3dPanel.launch(Dispatchers.Swing) {
            view3dPanel.pack()
            viewPanel.repaint()
            while (true) {
                viewPanel.repaint()
                delay(50)
            }
        }
    }
}
