/**
 * Model and Swing panel for plotting directed transitions between visited landmarks.
 * It is simulation-scoped for now, while kept separate for eventual reuse as a component.
 */
package org.simbrain.custom_sims.simulations.cogsci

import org.simbrain.util.UserParameter
import java.awt.*
import javax.swing.JPanel

/** Stores observed landmark nodes and directed transition counts. */
class LandmarkGraph {
    private val nodeLabels = mutableListOf<String>()
    private val transitionCounts = mutableMapOf<Pair<String, String>, Int>()
    private var lastLandmark: String? = null
    private var activeLandmark: String? = null

    /** Landmark labels in the order they were first visited. */
    val nodes: List<String>
        get() = nodeLabels

    /** Directed landmark transitions and their observed counts. */
    val edges: Map<Pair<String, String>, Int>
        get() = transitionCounts

    /** Landmark currently inside the graph-recognition radius, if any. */
    val currentLandmark: String?
        get() = activeLandmark

    /** Record a landmark observation and, on a change, its directed transition. */
    fun observe(landmark: String?) {
        if (landmark == null) {
            activeLandmark = null
            return
        }

        if (landmark !in nodeLabels) nodeLabels += landmark
        if (landmark == activeLandmark) return

        lastLandmark?.let { previous ->
            if (previous != landmark) {
                val edge = previous to landmark
                transitionCounts[edge] = (transitionCounts[edge] ?: 0) + 1
            }
        }
        lastLandmark = landmark
        activeLandmark = landmark
    }

    /** Forget all observed landmarks and transition counts. */
    fun reset() {
        nodeLabels.clear()
        transitionCounts.clear()
        lastLandmark = null
        activeLandmark = null
    }
}

/** Draws the landmark graph and highlights the landmark currently being observed. */
class EnvironmentGraphPanel(
    private val graph: LandmarkGraph,
    private val landmarkOrder: List<String>
) : JPanel() {

    /** Radius used to render graph nodes; exposed for a future settings dialog. */
    @UserParameter(
        label = "Node radius",
        description = "Radius of landmark nodes in the environment graph",
        minimumValue = 1.0,
        maximumValue = 100.0,
        increment = 1.0
    )
    var nodeRadius = 24.0

    init {
        preferredSize = Dimension(398, 300)
        background = Color.WHITE
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val centerX = width / 2.0
        val centerY = height / 2.0
        val radius = minOf(width, height) * 0.36
        val positions = landmarkOrder.mapIndexed { index, label ->
            val angle = -Math.PI / 2.0 + 2.0 * Math.PI * index / landmarkOrder.size
            label to java.awt.geom.Point2D.Double(
                centerX + radius * kotlin.math.cos(angle),
                centerY + radius * kotlin.math.sin(angle)
            )
        }.toMap()

        graph.edges.forEach { (edge, count) ->
            val start = positions[edge.first] ?: return@forEach
            val end = positions[edge.second] ?: return@forEach
            drawArrow(g, start.x, start.y, end.x, end.y, count)
        }

        val fontMetrics = g.fontMetrics
        graph.nodes.forEach { label ->
            val point = positions[label] ?: return@forEach
            val isCurrent = label == graph.currentLandmark
            val radius = nodeRadius
            g.color = if (isCurrent) Color(255, 224, 156) else Color(231, 242, 255)
            g.fillOval(
                (point.x - radius).toInt(),
                (point.y - radius).toInt(),
                (radius * 2).toInt(),
                (radius * 2).toInt()
            )
            g.color = if (isCurrent) Color(190, 115, 20) else Color(55, 94, 145)
            g.stroke = BasicStroke(if (isCurrent) 3f else 2f)
            g.drawOval(
                (point.x - radius).toInt(),
                (point.y - radius).toInt(),
                (radius * 2).toInt(),
                (radius * 2).toInt()
            )
            g.color = Color.DARK_GRAY
            val textWidth = fontMetrics.stringWidth(label)
            g.drawString(label, (point.x - textWidth / 2).toInt(), (point.y + fontMetrics.ascent / 2).toInt())
        }

        g.dispose()
    }

    private fun drawArrow(g: Graphics2D, x1: Double, y1: Double, x2: Double, y2: Double, count: Int) {
        val dx = x2 - x1
        val dy = y2 - y1
        val distance = kotlin.math.hypot(dx, dy)
        if (distance == 0.0) return
        val ux = dx / distance
        val uy = dy / distance
        val startInset = nodeRadius + 3.0
        val endInset = nodeRadius + 5.0
        val startX = x1 + ux * startInset
        val startY = y1 + uy * startInset
        val endX = x2 - ux * endInset
        val endY = y2 - uy * endInset

        g.color = Color(110, 110, 110)
        g.stroke = BasicStroke(1.5f)
        g.drawLine(startX.toInt(), startY.toInt(), endX.toInt(), endY.toInt())

        val arrowSize = 8.0
        val leftX = endX - ux * arrowSize - uy * arrowSize / 2
        val leftY = endY - uy * arrowSize + ux * arrowSize / 2
        val rightX = endX - ux * arrowSize + uy * arrowSize / 2
        val rightY = endY - uy * arrowSize - ux * arrowSize / 2
        g.fillPolygon(
            intArrayOf(endX.toInt(), leftX.toInt(), rightX.toInt()),
            intArrayOf(endY.toInt(), leftY.toInt(), rightY.toInt()),
            3
        )

        val labelX = ((startX + endX) / 2).toInt()
        val labelY = ((startY + endY) / 2).toInt()
        g.color = Color(70, 70, 70)
        g.drawString(count.toString(), labelX + 4, labelY - 4)
    }
}
