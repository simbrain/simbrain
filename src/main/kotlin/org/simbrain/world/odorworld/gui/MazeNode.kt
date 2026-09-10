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
import org.simbrain.world.odorworld.WallSegment
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D

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

    private var cachedSegments: List<WallSegment>? = null

    private var wallPath = Path2D.Double()

    /**
     * One path for all walls, rebuilt only when the maze hands out a different segment list, since this paints
     * on every canvas repaint.
     */
    private fun wallPath(segments: List<WallSegment>): Path2D {
        if (segments !== cachedSegments) {
            wallPath = Path2D.Double().apply {
                for (segment in segments) {
                    moveTo(segment.x1, segment.y1)
                    lineTo(segment.x2, segment.y2)
                }
            }
            cachedSegments = segments
        }
        return wallPath
    }

    override fun paint(paintContext: PPaintContext) {
        val maze = world.maze ?: return
        val g = paintContext.graphics as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.stroke = wallStroke
        g.color = wallColor
        g.draw(wallPath(maze.wallSegments(world.gridCellPixelSize)))
    }

    /**
     * Walls sit on the tile map, not the look and feel background, so the color does not follow the theme.
     */
    private val wallColor = Color(45, 45, 45)
}
