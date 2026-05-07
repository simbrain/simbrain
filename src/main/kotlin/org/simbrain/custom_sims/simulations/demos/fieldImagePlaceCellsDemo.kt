package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.radialProbabilisticStyle
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.connect
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.DecayRule
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.place
import org.simbrain.util.widgets.FieldImagePanel
import org.simbrain.world.odorworld.OdorWorldPreferences
import org.simbrain.world.odorworld.behaviors.Pursue
import org.simbrain.world.odorworld.behaviors.Wander
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.GridSensor
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*

val fieldImagePlaceCellsDemo = newSim {

    workspace.clearWorkspace()

    val odorWorldComponent = addOdorWorldComponent("Place World")
    val odorWorld = odorWorldComponent.world.apply {
        tileMap = loadTileMap(File(OdorWorldPreferences.tileMapDirectory, "yulins_world.tmx"))
        wrapAround = true
        isObjectsBlockMovement = true
    }

    val numCols = 5
    val numRows = 5
    val cellWidth = (odorWorld.width / numCols).toInt()
    val cellHeight = (odorWorld.height / numRows).toInt()

    val placeLabels = listOf(
        "bench (NW)", "bench (NE)", "pond (NW)",     "pond (N)",    "forest",
        "bench (SW)", "bench (SE)", "pond (SW)",     "pond (S)",    "forest (S)",
        "path/tulip", "path",       "path",          "garden (NW)", "garden (NE)",
        "path",       "brown tent", "green tent",    "garden (W)",  "garden (E)",
        "SW corner",  "path (S)",   "path (S)/tulip", "garden (SW)", "garden (SE)"
    )

    val mouse = odorWorld.addEntity(48, 240, EntityType.Mouse).apply {
        name = "Mouse"
        heading = 0.0
        behavior = Pursue().apply {
            targetType = EntityType.Isopod
            maxSpeed = 1.5
            visionRange = 480.0
            maxTurn = 18.0
            wallWeight = 1.0
            leadTicks = 6.0
        }
    }

    val gridSensor = GridSensor(0, 0, cellWidth, cellHeight).apply {
        columns = numCols
        rows = numRows
        highlighterVisibility = false
    }
    mouse.addSensor(gridSensor)

    odorWorld.addEntity(240, 240, EntityType.Isopod).apply {
        name = "Isopod"
        behavior = Wander().apply {
            maxSpeed = 1.0
            maxTurn = 8.0
            driftDegreesPerTick = 8.0
        }
    }
    mouse.select()

    val networkComponent = addNetworkComponent("Place Cell Network")
    val network = networkComponent.network

    val placeCells = network.addNeuronCollection(numRows * numCols) {
        updateRule = DecayRule().apply {
            updateType = DecayRule.UpdateType.Relative
            decayFraction = 0.3
            lowerBound = 0.0
            upperBound = 1.0
            isClipped = true
        }
        upperBound = 1.0
        lowerBound = 0.0
    }
    placeCells.label = "Place Cells"
    placeCells.layout = GridLayout(62.0, 62.0, numCols)
    placeCells.applyLayout(0, 0)
    placeCells.neuronList.forEachIndexed { i, n -> n.label = placeLabels[i] }

    val localStrategy = radialProbabilisticStyle(radius = 75.0, probability = 1.0)
    val recurrent = network.connect(placeCells.neuronList, placeCells.neuronList, localStrategy)
    recurrent.forEach { it.forceSetStrength(0.1) }

    val bugReadoutMax = 1.8
    val bugReadout = network.addNeuronCollection(1) {
        upperBound = bugReadoutMax
        lowerBound = 0.0
    }
    bugReadout.label = "Object Readout"
    bugReadout.neuronList[0].label = "bug"
    bugReadout.applyLayout(0, 360)

    val bugSensor = mouse.addObjectSensor(EntityType.Isopod, 0.0, 0.0, 160.0).apply {
        label = "Bug"
        baseValue = bugReadoutMax
    }

    couplingManager.createCoupling(gridSensor, placeCells)
    with(couplingManager) {
        bugSensor couple bugReadout.neuronList[0]
    }

    addSidebarInfo(
        """
        # Field Image Demo (Place Cells)

        Second in the "field image" series. Instead of object sensors, a mouse
        carries a `GridSensor` (a coarse "place cell" tiling of the world). Each
        cell is one neuron, hand-labeled with the most prominent landmark at
        that location, e.g. `forest`, `pond (N)`, `garden (NW)`, `brown tent`.

        # Simulation Details

        ## Place Cells

        A `5 x 5` grid sensor on the mouse covers the world. Its 25 outputs feed
        a matching `5 x 5` `NeuronCollection`. At any moment the mouse occupies
        one cell, which gets activation `1.0` from the sensor. Labels are read
        off a fixed `5 x 5` table that was built by inspecting the world's
        landmarks (the `.tmx` file's tile types are too coarse to be useful
        here). Several cells share a label by design, so the field image can
        say things like "I'm somewhere in the Trees" rather than always
        pinpointing a coordinate.

        ## Local Recurrence

        Place cells are connected to one another using `DistanceBased`
        connectivity (a `radialProbabilisticStyle` with `radius = 60`, so each
        cell wires only to its four orthogonal neighbors in the layout grid).
        Weights are weak (`0.1`) so we get a small "penumbra" of activity
        around the active cell without runaway feedback. Combined with
        `DecayRule` (`decayFraction = 0.3`) the halo fades within a couple of
        steps once the mouse moves on.

        ## NPC Behavior

        A `Mouse` pursues a bug, implemented as an `Isopod` entity. The bug uses `Wander` rather than
        `Evade`, so it meanders rather than actively fleeing — this lets the
        mouse stay in close pursuit instead of stalling out at the edges of
        the world. The terrain (trees, ponds, fenced gardens) still blocks
        movement, so the chase falls into a small repertoire of paths and the
        same place-cell labels recur in the field image. The mouse is selected
        by default because the field image reflects the mouse's point of view.

        The field image also includes a `bug` readout from a simple object
        sensor. This is a small example of aggregating multiple neuron
        collections into one field display. The `bug` readout can become
        stronger than the place-cell activations, so it can dominate the field
        when the mouse gets close.

        # What to Do

        1. Press `Run`. The field image will show the mouse's current cell at
           the center, with neighboring cells dimly haloed around it.
        2. Watch how the active label changes as the mouse chases the bug.
           After a few laps the labels fall into a small recurring set.
        3. Adjust `Threshold` to widen or narrow the visible halo.
        4. Try right-clicking the mouse and pressing `Properties` to bump up
           `Pursue.maxSpeed`, or drag the bug to a new location to seed a
           different chase path.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        """.trimIndent(),
        width = 320
    )

    withGui {
        place(odorWorldComponent, 0, 0, 520, 589)
        place(networkComponent, 512, 1, 476, 582)

        val fieldPanel = FieldImagePanel(
            source = {
                placeCells.neuronList.map { (it.label ?: "?") to it.activation } +
                    bugReadout.neuronList.map { (it.label ?: "?") to it.activation }
            }
        ).apply {
            preferredSize = Dimension(517, 540)
            maxItems = 9
            threshold = 0.32
        }

        val thresholdSlider = JSlider(0, 100, 32).apply {
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
            setBounds(989, 5, 517, 574)
            isVisible = true
        }
        addInternalFrame(fieldFrame)

        Timer(50) { fieldPanel.repaint() }.start()
    }
}
