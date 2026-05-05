package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.place
import org.simbrain.util.widgets.FieldImagePanel
import org.simbrain.world.odorworld.OdorWorldPreferences
import org.simbrain.world.odorworld.behaviors.Evade
import org.simbrain.world.odorworld.behaviors.Pursue
import org.simbrain.world.odorworld.behaviors.Wander
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.View3DSensor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.io.File
import javax.swing.*

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
    odorWorld.addEntity(60, 440, EntityType.Swiss).apply {
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

    val swissSensor = mouse.addObjectSensor(EntityType.Swiss, 50.0, 0.0, 200.0).apply { label = "Swiss" }
    val pansySensor = mouse.addObjectSensor(EntityType.Pansy, 50.0, 30.0, 200.0).apply { label = "Pansy" }
    val tulipSensor = mouse.addObjectSensor(EntityType.Tulip, 50.0, -30.0, 200.0).apply { label = "Tulip" }
    val fishSensor = mouse.addObjectSensor(EntityType.Fish, 50.0, 60.0, 200.0).apply { label = "Fish" }

    val networkComponent = addNetworkComponent("Sensor Network")
    val network = networkComponent.network

    val sensorNeurons = network.addNeuronCollection(4) {
        upperBound = 1.0
        lowerBound = 0.0
    }
    sensorNeurons.label = "Sensors"
    sensorNeurons.layout = GridLayout()
    sensorNeurons.applyLayout(0, 0)
    sensorNeurons.neuronList[0].label = "Swiss"
    sensorNeurons.neuronList[1].label = "Pansy"
    sensorNeurons.neuronList[2].label = "Tulip"
    sensorNeurons.neuronList[3].label = "Fish"

    with(couplingManager) {
        swissSensor couple sensorNeurons.neuronList[0]
        pansySensor couple sensorNeurons.neuronList[1]
        tulipSensor couple sensorNeurons.neuronList[2]
        fishSensor couple sensorNeurons.neuronList[3]
    }

    addSidebarInfo(
        """
        # Field Image Demo (Sensors)

        First in a planned series of "field image" simulations. This one is the
        simplest case: the field image just visualizes the mouse's sensor
        activations directly, so the labels you see (`Swiss`, `Pansy`, `Tulip`,
        `Fish`) map one-to-one onto what is currently being detected.

        Later simulations in the series will replace the sensor inputs with the
        latent activations of a recurrent or trained network, where the labels
        are learned rather than hand-coded.

        # Simulation Details

        ## Sensors

        The mouse from the `NPC Basic Demo` carries four `ObjectSensor`s
        (`Swiss`, `Pansy`, `Tulip`, `Fish`) at slightly different angles. Each is
        coupled to a single neuron in the `Sensors` collection.

        ## Field Image

        The `FieldImagePanel` (in `org.simbrain.util.widgets`) draws above-threshold
        nodes only. The most active node is centered, largest, and most saturated.
        Less active nodes are placed radially around it: smaller, more grey, and
        farther from the center as their activation drops. Nodes below the
        threshold are not drawn. Resize the window to scale the image.

        Because the panel just consumes a `() -> List<Pair<String, Double>>` of
        labels and activations, the same widget will be reused for the latent
        versions in the rest of the series.

        # What to Do

        1. Press `Run`. As the mouse hunts the cheeses and brushes past flowers
           or the fish, the corresponding label fades in and out of the field
           image.
        2. Drag a `Pansy` or `Tulip` close to the mouse to make that label win.
        3. Adjust `Threshold` to control how aggressively the field image hides
           quiet sensors (try `0.05` and `0.3`).

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        """.trimIndent(),
        width = 320,
        initiallyOpened = true
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
        val viewFrame = JInternalFrame("Mouse 3D View", true, true, true, true).apply {
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
        val fieldFrame = JInternalFrame("Field Image", true, true, true, true).apply {
            layout = BorderLayout()
            add(fieldPanel, BorderLayout.CENTER)
            add(controlBar, BorderLayout.SOUTH)
            setBounds(840, 0, 480, 500)
            isVisible = true
        }
        addInternalFrame(fieldFrame)

        view3dSensor.update(mouse)

        Timer(50) {
            viewPanel.repaint()
            fieldPanel.repaint()
        }.start()
    }
}
