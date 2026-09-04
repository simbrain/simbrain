package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.sensors.View3DSensor
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.RenderingHints
import java.awt.Graphics2D
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * First-person renders from inside a 5x5 maze at three positions: looking down a corridor, facing a wall at close
 * range, and the same corridor with flat walls. Verifies maze walls, brick texture, cell seams and fog.
 */
class View3DMazeSnapshot : UiSnapshotDef {

    override val name = "view3d-maze"

    override fun build(): Component {
        val component = OdorWorldComponent("Maze")
        val world = component.world
        world.wrapAround = false
        world.isObjectsBlockMovement = false
        world.generateMaze(5, 5, cellSizeInTiles = 2, seed = 42L)
        val (mouse, cheese) = runBlocking {
            val mouse = world.addEntity(288, 288, EntityType.Mouse).apply {
                heading = 90.0
                movementMode = MovementMode.GRID
            }
            val cheese = world.addEntity(288, 160, EntityType.Swiss)
            mouse to cheese
        }

        fun renderView(heading: Double, textured: Boolean, label: String): JPanel {
            val sensor = View3DSensor().apply {
                outputWidth = 160
                outputHeight = 120
                viewDistance = 400.0
                textureWalls = textured
            }
            mouse.heading = heading
            sensor.update(mouse)
            val image = sensor.renderedImage!!
            return JPanel().apply {
                layout = java.awt.BorderLayout()
                add(JLabel(label, SwingConstants.CENTER), java.awt.BorderLayout.NORTH)
                add(object : JPanel() {
                    override fun paintComponent(g: Graphics) {
                        super.paintComponent(g)
                        (g as Graphics2D).setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                        )
                        g.drawImage(image, 0, 0, width, height, null)
                    }
                }.apply { preferredSize = Dimension(320, 240) }, java.awt.BorderLayout.CENTER)
            }
        }

        val openHeadings = listOf(90.0, 0.0, 180.0, 270.0)
        val corridorHeading = openHeadings.first { heading ->
            val cell = world.cellAt(mouse.location)
            world.maze!!.isOpen(cell.first, cell.second, org.simbrain.world.odorworld.GridDirection.fromHeading(heading))
        }
        val wallHeading = openHeadings.first { heading ->
            val cell = world.cellAt(mouse.location)
            !world.maze!!.isOpen(cell.first, cell.second, org.simbrain.world.odorworld.GridDirection.fromHeading(heading))
        }

        return JPanel(GridLayout(1, 3, 8, 8)).apply {
            add(renderView(corridorHeading, true, "Corridor, textured (cheese ahead)"))
            add(renderView(wallHeading, true, "Facing a wall, textured"))
            add(renderView(corridorHeading, false, "Corridor, flat"))
            preferredSize = Dimension(1000, 280)
        }
    }
}
