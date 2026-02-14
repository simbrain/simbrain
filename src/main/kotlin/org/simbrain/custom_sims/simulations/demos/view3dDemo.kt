package org.simbrain.custom_sims.simulations.demos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.util.SmellSource
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.View3DSensor
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JPanel

/**
 * Initial demo simulation for the View3DSensor - a pseudo-3D first-person view from an entity's perspective.
 *
 * The sensor uses raycasting to render:
 * - Textured floor from the tilemap
 * - World boundary walls (when wrapAround is disabled)
 * - Other entities as billboarded sprites
 *
 * Use arrow keys on the OdorWorld to move the mouse and see the 3D view update in real-time.
 */
val view3dDemo = newSim {

    workspace.clearWorkspace()

    // Create OdorWorld
    val odorWorldComponent = addOdorWorldComponent("Odor World")
    val odorWorld = odorWorldComponent.world

    // Configure tilemap with grass
    odorWorld.apply {
        tileMap = TileMap(16, 16)
        wrapAround = false  // Enable walls at world boundary
        isObjectsBlockMovement = false
    }

    // Fill tilemap with grass (tile ID 6 = Grass1)
    odorWorld.tileMap.layers.first().let { layer ->
        for (x in 0 until odorWorld.tileMap.width) {
            for (y in 0 until odorWorld.tileMap.height) {
                layer[x, y] = 6  // Grass tile
            }
        }
    }

    // Create mouse agent at center
    val mouse = odorWorld.addEntity(
        odorWorld.width / 2,
        odorWorld.height / 2,
        EntityType.Mouse
    ).apply {
        heading = 0.0
        name = "Agent"
    }

    // Add View3DSensor to the mouse
    val view3dSensor = View3DSensor().apply {
        label = "3D View"
        fov = 90.0
        viewDistance = 400.0
        outputWidth = 128
        outputHeight = 128
        horizonPosition = 0.5
        cameraWorldHeight = 16.0
        wallHeight = 2.0
        billboardSprites = true
    }
    mouse.addSensor(view3dSensor)

    // Add default movement effectors for keyboard control
    mouse.addDefaultEffectors()

    // Add some cheese entities around the world
    val cheesePositions = listOf(
        point(100, 100),
        point(400, 100),
        point(100, 400),
        point(400, 400),
        point(250, 200)
    )

    cheesePositions.forEach { pos ->
        odorWorld.addEntity(pos.x.toInt(), pos.y.toInt(), EntityType.Swiss).apply {
            smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        }
    }

    // Add a flower
    odorWorld.addEntity(300, 350, EntityType.Flower).apply {
        smellSource = SmellSource(doubleArrayOf(0.0, 1.0, 0.0, 0.0, 0.0, 0.0))
    }

    // Add a rotating entity (cow) to demonstrate billboard vs non-billboard rendering
    odorWorld.addEntity(200, 300, EntityType.Cow).apply {
        heading = 45.0  // Face northeast
        name = "Cow"
    }

    // GUI setup
    withGui {
        // Create a panel to display the 3D view
        val viewPanel = object : JPanel() {
            init {
                preferredSize = Dimension(256, 256)
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val image = view3dSensor.renderedImage
                if (image != null) {
                    // Scale the image to fit the panel
                    g.drawImage(image, 0, 0, width, height, null)
                } else {
                    g.drawString("No image yet", 10, height / 2)
                }
            }
        }

        // Create control panel with the 3D view display
        val controlPanel = createControlPanel("3D View", 5, 10) {
            addComponent(viewPanel)
            addSeparator()
            addLabel("Use arrow keys in OdorWorld")
            addLabel("to move the agent")
            addSeparator()
            addSlider("FOV", 30.0, 120.0, 90.0, 5.0) { value ->
                view3dSensor.fov = value
            }
            addSlider("View Distance", 100.0, 800.0, 400.0, 50.0) { value ->
                view3dSensor.viewDistance = value
            }
            addSlider("Horizon Position", 0.1, 0.9, 0.5, 0.1) { value ->
                view3dSensor.horizonPosition = value
            }
            addSlider("Camera Height", 4.0, 64.0, 16.0, 4.0) { value ->
                view3dSensor.cameraWorldHeight = value
            }
            addSlider("Wall Height", 0.5, 4.0, 2.0, 0.5) { value ->
                view3dSensor.wallHeight = value
            }
            addCheckBox("Wrap Around (no walls)", false) { checked ->
                odorWorld.wrapAround = checked
            }
            addCheckBox("Billboard Sprites", true) { checked ->
                view3dSensor.billboardSprites = checked
            }
        }

        // Place components
        place(odorWorldComponent) {
            location = point(280, 10)
            width = 550
            height = 550
        }

        // Update the view panel periodically using the control panel's coroutine scope
        controlPanel.launch(Dispatchers.Swing) {
            while (true) {
                viewPanel.repaint()
                delay(50)  // ~20 FPS
            }
        }
    }

    // Add documentation sidebar
    addSidebarInfo(
        """
        # 3D View Sensor Demo

        This simulation demonstrates the **View3DSensor**, which renders a pseudo-3D
        first-person view from an entity's perspective using classic raycasting techniques.

        ## What You See

        - **Floor**: Textured from the tilemap (grass tiles)
        - **Walls**: World boundary walls (disable "Wrap Around" to see them)
        - **Sprites**: Other entities (cheese, flowers) rendered as billboards
        - **Sky**: Simple gradient ceiling

        ## Sensor Properties

        Adjust these in the control panel:
        - **FOV**: Field of view (wider = more peripheral vision)
        - **View Distance**: How far the camera can see
        - **Horizon Position**: Where the horizon sits on screen
        - **Camera Height**: Vertical position of the viewpoint
        - **Wall Height**: Height of boundary walls (1.0 = horizon level)
        - **Wrap Around**: When disabled, walls appear at world boundaries
        - **Billboard Sprites**: When enabled, sprites always face the camera.
          When disabled, sprites face their entity's heading and appear
          foreshortened when viewed at angles (try this with the cow!)

        ## Output

        The sensor outputs `brightness`, `red`, `green`, `blue` arrays that can be
        coupled to neural networks for visual processing tasks.

        ## Technical Details

        Uses floor-casting for the ground texture and sprite rendering with z-buffering
        for proper occlusion. The rendering style is reminiscent of early 90s games.
        """.trimIndent()
    )
}
