package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.place
import org.simbrain.util.updateAction
import org.simbrain.util.widgets.FieldImagePanel
import org.simbrain.world.odorworld.OdorWorldPreferences
import org.simbrain.world.odorworld.behaviors.Evade
import org.simbrain.world.odorworld.behaviors.Pursue
import org.simbrain.world.odorworld.behaviors.Wander
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.View3DSensor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.io.File
import javax.swing.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

val fieldImageDemo = newSim {

    workspace.clearWorkspace()

    val odorWorldComponent = addOdorWorldComponent("Field World")
    val odorWorld = odorWorldComponent.world.apply {
        tileMap = loadTileMap(File(OdorWorldPreferences.tileMapDirectory, "yulins_world.tmx"))
        wrapAround = true
        isObjectsBlockMovement = true
    }

    val mouse = odorWorld.addEntity(60, 200, EntityType.Mouse).apply {
        name = "Mouse"
        heading = 0.0
        behavior = Pursue().apply {
            targetType = EntityType.Swiss
            maxSpeed = 1.5
            visionRange = 400.0
        }
    }

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

    odorWorld.addEntity(440, 200, EntityType.Swiss).apply {
        name = "Swiss A"
        behavior = Evade().apply {
            threatType = EntityType.Mouse
            maxSpeed = 2.0
            visionRange = 300.0
        }
    }
    odorWorld.addEntity(180, 440, EntityType.Swiss).apply {
        name = "Swiss B"
        behavior = Evade().apply {
            threatType = EntityType.Mouse
            maxSpeed = 2.0
            visionRange = 300.0
        }
    }
    odorWorld.addEntity(440, 440, EntityType.Fish).apply {
        name = "Fish"
        behavior = Wander().apply { maxSpeed = 1.2 }
    }
    odorWorld.addEntity(220, 200, EntityType.Pansy).apply { name = "Pansy" }
    odorWorld.addEntity(220, 380, EntityType.Tulip).apply { name = "Tulip" }

    val networkComponent = addNetworkComponent("Sensor Network")
    val network = networkComponent.network

    val sensorNeurons = network.addNeuronCollection(4) {
        upperBound = 1.0
        lowerBound = 0.0
        clamped = true
    }
    sensorNeurons.label = "Sensors"
    sensorNeurons.layout = GridLayout()
    sensorNeurons.applyLayout(0, 0)
    sensorNeurons.neuronList[0].label = "Swiss"
    sensorNeurons.neuronList[1].label = "Pansy"
    sensorNeurons.neuronList[2].label = "Tulip"
    sensorNeurons.neuronList[3].label = "Fish"

    val objectClassReadouts = listOf(
        EntityType.Swiss to sensorNeurons.neuronList[0],
        EntityType.Pansy to sensorNeurons.neuronList[1],
        EntityType.Tulip to sensorNeurons.neuronList[2],
        EntityType.Fish to sensorNeurons.neuronList[3]
    )

    fun projectedVisionActivation(entity: OdorWorldEntity): Double {
        val cameraPos = view3dSensor.computeAbsoluteLocation(mouse)
        var dx = entity.x - cameraPos.x
        var dy = entity.y - cameraPos.y

        if (odorWorld.wrapAround) {
            if (dx > odorWorld.width / 2) dx -= odorWorld.width
            else if (dx < -odorWorld.width / 2) dx += odorWorld.width
            if (dy > odorWorld.height / 2) dy -= odorWorld.height
            else if (dy < -odorWorld.height / 2) dy += odorWorld.height
        }

        val heading = Math.toRadians(mouse.heading)
        val dirX = cos(heading)
        val dirY = -sin(heading)
        val planeScale = tan(Math.toRadians(view3dSensor.fov / 2))
        val planeX = -dirY * planeScale
        val planeY = dirX * planeScale
        val invDet = 1.0 / (planeX * dirY - dirX * planeY)
        val transformX = invDet * (dirY * dx - dirX * dy)
        val transformY = invDet * (-planeY * dx + planeX * dy)

        if (transformY <= 0.1 || transformY >= view3dSensor.viewDistance) return 0.0

        val screenWidth = view3dSensor.outputWidth
        val screenHeight = view3dSensor.outputHeight
        val spriteScreenX = ((screenWidth / 2.0) * (1 + transformX / transformY)).toInt()
        val spriteWidth = ((entity.entityType.width * screenHeight) / transformY).toInt()
            .coerceIn(1, screenWidth * 4)
        val drawStartX = (spriteScreenX - spriteWidth / 2).coerceIn(0, screenWidth)
        val drawEndX = (spriteScreenX + spriteWidth / 2).coerceIn(0, screenWidth)

        if (drawStartX >= drawEndX) return 0.0

        return (1.0 - transformY / view3dSensor.viewDistance).coerceIn(0.0, 1.0)
    }

    /**
     * Iterate through world entities and compute a plausible sensor activation for each object class
     * relative to the 3D vision sensor. This directly primes the sensor neurons for the field image.
     */
    fun updateSensorNeuronsFromVisibleObjects() {
        objectClassReadouts.forEach { (entityType, neuron) ->
            neuron.activation = odorWorld.entityList
                .asSequence()
                .filter { it != mouse && it.entityType == entityType }
                .maxOfOrNull(::projectedVisionActivation) ?: 0.0
        }
    }

    workspace.addUpdateAction(updateAction("Update visible object readout") {
        updateSensorNeuronsFromVisibleObjects()
    })

    addSidebarInfo(
        """
        # Field Image Demo (Sensors)

        First in a planned series of "field image" simulations. This one is the
        simplest case: the field image visualizes a direct readout of object
        classes visible in the mouse's 3D camera, so the labels you see
        (`Swiss`, `Pansy`, `Tulip`, `Fish`) map one-to-one onto what is
        currently in view.

        Later simulations in the series will replace the sensor inputs with the
        latent activations of a recurrent or trained network, where the labels
        are learned rather than hand-coded.

        # Simulation Details

        ## Toy Vision Readout

        The mouse from the `NPC Basic Demo` carries a `View3DSensor`, and the
        four neurons in the `Sensors` collection are set directly from that
        camera geometry. On each update, objects are projected into the same
        camera frustum used by the `Mouse 3D View`; if a `Swiss`, `Pansy`,
        `Tulip`, or `Fish` would appear on screen, the corresponding neuron is
        activated according to its distance from the camera.

        The readout is currently type-level rather than object-level. There is
        one `Swiss` neuron, not separate neurons for `Swiss A` and `Swiss B`, so
        multiple visible objects of the same type are collapsed into a single
        label using the strongest visible activation. The two `Swiss` objects
        are placed far apart to make this limitation less distracting during
        ordinary runs.

        This is intentionally not a general-purpose vision model. It is a
        simple, hand-coded object-class readout for a toy demo, chosen so the
        field image matches the rendered 3D view.
        It is not meant to be a plausible cognitive model. The goal is to start
        experimenting with how this kind of graphical display could be used to
        show a simple, momentary field of salient contents.

        # What to Do

        Press `Run` and watch as the agent moves around. A plausible "field of
        consciousness" is shown in the field panel, with labels appearing and
        fading as objects enter and leave the mouse's 3D view. The mouse and
        several objects use NPC behaviors, so the scene produces decent motion
        to watch without manual setup. Adjust `Threshold` on the field display
        to control how much activity is allowed into the field.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        """.trimIndent(),
        width = 320
    )

    withGui {
        place(odorWorldComponent, 0, 0, 480, 500)
        place(networkComponent, 480, 0, 360, 250)

        val viewPanel = object : JPanel() {
            init {
                preferredSize = Dimension(360, 250)
                minimumSize = Dimension(120, 120)
                background = Color.BLACK
            }
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val image = view3dSensor.renderedImage
                if (image != null) g.drawImage(image, 0, 0, width, height, null)
            }
        }
        val viewFrame = GenericJInternalFrame("Mouse 3D View", true, true, true, true).apply {
            layout = BorderLayout()
            add(viewPanel, BorderLayout.CENTER)
            setBounds(480, 250, 360, 250)
            isVisible = true
        }
        addInternalFrame(viewFrame)

        val fieldPanel = FieldImagePanel(
            source = { sensorNeurons.neuronList.map { (it.label ?: "?") to it.activation } }
        ).apply {
            preferredSize = Dimension(480, 460)
        }

        val thresholdSlider = JSlider(0, 100, 10).apply {
            majorTickSpacing = 25
            minorTickSpacing = 5
            paintTicks = true
            paintLabels = true
            addChangeListener {
                fieldPanel.threshold = value / 100.0
                fieldPanel.repaint()
            }
        }
        val controlBar = JPanel(BorderLayout(6, 0)).apply {
            border = BorderFactory.createEmptyBorder(2, 8, 2, 8)
            add(JLabel("Threshold"), BorderLayout.WEST)
            add(thresholdSlider, BorderLayout.CENTER)
        }
        val fieldFrame = GenericJInternalFrame("Field Image", true, true, true, true).apply {
            layout = BorderLayout()
            add(fieldPanel, BorderLayout.CENTER)
            add(controlBar, BorderLayout.SOUTH)
            setBounds(840, 0, 480, 500)
            isVisible = true
        }
        addInternalFrame(fieldFrame)

        view3dSensor.update(mouse)
        updateSensorNeuronsFromVisibleObjects()

        Timer(50) {
            viewPanel.repaint()
            fieldPanel.repaint()
        }.start()
    }
}
