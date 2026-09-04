/**
 * Piccolo overlay that draws an [OdorWorld]'s maze walls above the tile layers. Owned by [OdorWorldPanel], which
 * keeps it between the tile images and the entity nodes; it repaints itself on maze changes.
 */
package org.simbrain.world.odorworld.gui

import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.Dispatchers
import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.world.odorworld.OdorWorld
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Line2D

class MazeNode(private val world: OdorWorld) : PNode() {

    private val wallStroke = BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

    init {
        pickable = false
        childrenPickable = false
        refresh()
        world.events.mazeChanged.on(Dispatchers.Swing) { refresh() }
        world.events.tileMapChanged.on(Dispatchers.Swing) { refresh() }
    }

    private fun refresh() {
        setBounds(0.0, 0.0, world.width, world.height)
        visible = world.maze != null
        invalidatePaint()
    }

    override fun paint(paintContext: PPaintContext) {
        val maze = world.maze ?: return
        val g = paintContext.graphics as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.stroke = wallStroke
        g.color = wallColor
        val line = Line2D.Double()
        for (segment in maze.wallSegments(world.gridCellPixelSize)) {
            line.setLine(segment.x1, segment.y1, segment.x2, segment.y2)
            g.draw(line)
        }
    }

    /**
     * Walls sit on the tile map, not the look and feel background, so the color does not follow the theme.
     */
    private val wallColor = Color(45, 45, 45)
}
