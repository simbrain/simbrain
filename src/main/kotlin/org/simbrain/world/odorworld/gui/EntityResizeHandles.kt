/**
 * Corner handles that resize odor world entities by dragging. While Alt (Option on macOS) is held, or always if
 * [org.simbrain.world.odorworld.OdorWorldPreferences.alwaysShowResizeHandles] is on, each selected [EntityNode] shows
 * four handles just outside its selection outline, kept at a constant screen size whatever the zoom. Dragging any corner sets
 * [OdorWorldEntity.scale] about the entity's center, so its location never changes, and applies the same ratio to
 * every selected entity. The scale math lives in [ResizeScaling] so it can be tested without a canvas.
 */
package org.simbrain.world.odorworld.gui

import org.piccolo2d.PCamera
import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.extras.handles.PHandle
import org.piccolo2d.extras.util.PNodeLocator
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PDimension
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.event.InputEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.beans.PropertyChangeListener
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Pure scale math for a resize drag, in world coordinates. */
internal object ResizeScaling {

    /** Smallest scale an entity can be dragged to, matching the minimum of [OdorWorldEntity.scale]. */
    const val MIN_SCALE = 0.05

    /** Step that Shift snaps scales to. */
    const val STEP = 0.25

    /** How close, in screen pixels, the dragged corner must come to its native-size position to snap there. */
    const val NATIVE_SNAP_PIXELS = 6.0

    /**
     * The scale that puts the dragged corner where the cursor is, before snapping or limits. [cornerOffset] is the
     * grabbed corner relative to the entity center at the start of the drag, [grabOffset] the cursor relative to the
     * center at the start, and [cursorOffset] the cursor relative to the center now. Only motion along the
     * center-to-corner diagonal counts, and measuring from the center lets the drag survive the entity moving.
     */
    fun rawScale(startScale: Double, cornerOffset: Point2D, grabOffset: Point2D, cursorOffset: Point2D): Double {
        val lengthSquared = cornerOffset.x * cornerOffset.x + cornerOffset.y * cornerOffset.y
        if (lengthSquared == 0.0) return startScale
        val dx = cursorOffset.x - grabOffset.x
        val dy = cursorOffset.y - grabOffset.y
        val ratio = 1 + (dx * cornerOffset.x + dy * cornerOffset.y) / lengthSquared
        return startScale * ratio
    }

    /**
     * Snaps [scale] to a multiple of [STEP] when [stepped], and otherwise to 1.0 when the dragged corner is within
     * [NATIVE_SNAP_PIXELS] of where it sits at native size. [nativeCornerDistance] is the center-to-corner distance at
     * scale 1.0 in world units and [viewScale] the camera's world-to-screen factor.
     */
    fun snap(scale: Double, stepped: Boolean, nativeCornerDistance: Double, viewScale: Double): Double {
        if (stepped) {
            return max(STEP, (scale / STEP).roundToInt() * STEP)
        }
        val screenDistanceFromNative = abs(scale - 1.0) * nativeCornerDistance * viewScale
        return if (screenDistanceFromNative < NATIVE_SNAP_PIXELS) 1.0 else scale
    }

    /**
     * The range a common ratio can take so that every entity stays between [MIN_SCALE] and the largest scale that
     * still fits in a [worldWidth] by [worldHeight] world. Each entry of [entities] is a start scale with the native
     * width and height.
     */
    fun ratioLimits(
        entities: List<Triple<Double, Double, Double>>,
        worldWidth: Double,
        worldHeight: Double
    ): ClosedFloatingPointRange<Double> {
        var low = 0.0
        var high = Double.MAX_VALUE
        for ((startScale, nativeWidth, nativeHeight) in entities) {
            if (startScale <= 0.0) continue
            low = max(low, MIN_SCALE / startScale)
            val maxScale = max(MIN_SCALE, min(worldWidth / nativeWidth, worldHeight / nativeHeight))
            high = min(high, maxScale / startScale)
        }
        if (high < low) high = low
        return low..high
    }
}

/** A corner of an entity's box, as signs of the offset from the center in world coordinates (y down). */
internal enum class ResizeCorner(val signX: Int, val signY: Int, val cursorType: Int) {
    TOP_LEFT(-1, -1, Cursor.NW_RESIZE_CURSOR),
    TOP_RIGHT(1, -1, Cursor.NE_RESIZE_CURSOR),
    BOTTOM_LEFT(-1, 1, Cursor.SW_RESIZE_CURSOR),
    BOTTOM_RIGHT(1, 1, Cursor.SE_RESIZE_CURSOR)
}

/**
 * The four resize handles of one [EntityNode], parented to its sprite so they follow it as it moves or changes size.
 * They are sized from the camera's view scale and resized whenever it changes, so call [detach] when the entity is
 * deselected or the node goes away.
 */
internal class EntityResizeHandles(
    private val entityNode: EntityNode,
    private val body: PNode,
    private val panel: OdorWorldPanel
) {
    private val camera: PCamera get() = panel.canvas.camera

    private val handles = ResizeCorner.entries.map { ResizeHandle(it) }

    private val viewListener = PropertyChangeListener { handles.forEach { it.updateSize() } }

    init {
        handles.forEach { body.addChild(it) }
        camera.addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, viewListener)
    }

    fun detach() {
        camera.removePropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, viewListener)
        handles.forEach { it.release() }
        body.removeChildren(handles)
    }

    /** True while one of these handles is being dragged. */
    val isDragging: Boolean get() = handles.any { it.isDragging }

    private val viewScale: Double get() = camera.viewScale.takeIf { it > 0 } ?: 1.0

    /**
     * Places a handle's center outside the [body]'s corner, past the selection outline by half the visible handle,
     * so the handle's inner corner meets the outline's corner and the body stays free to grab.
     */
    private inner class CornerLocator(private val corner: ResizeCorner) : PNodeLocator(body) {
        private fun outward(extent: Double) = extent * SELECTION_OUTLINE_EXTENSION + VISIBLE_SIZE / 2 / viewScale

        override fun locateX(): Double {
            val b = body.boundsReference
            return b.centerX + corner.signX * (b.width / 2 + outward(b.width))
        }

        override fun locateY(): Double {
            val b = body.boundsReference
            return b.centerY + corner.signY * (b.height / 2 + outward(b.height))
        }
    }

    private inner class ResizeHandle(private val corner: ResizeCorner) : PHandle(CornerLocator(corner)) {

        private var hovered = false
        private var drag: ResizeDrag? = null

        val isDragging: Boolean get() = drag != null

        init {
            stroke = null
            addInputEventListener(object : PBasicInputEventHandler() {
                init {
                    eventFilter = PInputEventFilter(InputEvent.BUTTON1_MASK).apply { acceptsAlreadyHandledEvents = true }
                }

                override fun mouseEntered(event: PInputEvent) {
                    if (!hovered) {
                        hovered = true
                        panel.canvas.pushCursor(Cursor.getPredefinedCursor(corner.cursorType))
                    }
                }

                override fun mouseExited(event: PInputEvent) {
                    if (hovered && drag == null) {
                        hovered = false
                        panel.canvas.popCursor()
                    }
                }

                override fun mouseClicked(event: PInputEvent) {
                    if (event.clickCount == 2) resetToNativeSize()
                }
            })
            updateSize()
        }

        fun updateSize() {
            val hitSize = (HIT_SIZE / viewScale).toFloat()
            reset()
            append(Rectangle2D.Float(0f, 0f, hitSize, hitSize), false)
            relocateHandle()
        }

        override fun paint(paintContext: PPaintContext) {
            val g = paintContext.graphics
            val b = boundsReference
            val visible = VISIBLE_SIZE / viewScale
            val square = Rectangle2D.Double(b.centerX - visible / 2, b.centerY - visible / 2, visible, visible)
            val previousStroke = g.stroke
            // White rather than the themed canvas color: the world's tiles do not follow the theme, and white reads
            // against any tile art
            g.color = Color.WHITE
            g.fill(square)
            g.color = NetworkTheme.current.selectionHandle
            g.stroke = BasicStroke((1.5 / viewScale).toFloat())
            g.draw(square)
            g.stroke = previousStroke
        }

        override fun startHandleDrag(localPoint: Point2D, event: PInputEvent) {
            drag = ResizeDrag(entityNode.entity, corner, event)
        }

        override fun dragHandle(localDimension: PDimension, event: PInputEvent) {
            drag?.update(event)
        }

        override fun endHandleDrag(localPoint: Point2D, event: PInputEvent) {
            drag?.finish()
            drag = null
            if (hovered && !fullBoundsReference.contains(globalToParent(event.position))) {
                hovered = false
                panel.canvas.popCursor()
            }
            // Alt may have been released during the drag; hide the handles once this event is done with them
            SwingUtilities.invokeLater { entityNode.refreshResizeHandles() }
        }

        private fun globalToParent(point: Point2D): Point2D = parent?.globalToLocal(Point2D.Double(point.x, point.y)) ?: point

        /** Ends any hover or drag in progress before the handle is removed. */
        fun release() {
            drag?.finish()
            drag = null
            if (hovered) {
                hovered = false
                panel.canvas.popCursor()
            }
        }
    }

    private fun resetToNativeSize() {
        val entities = panel.selectedEntityModels.ifEmpty { listOf(entityNode.entity) }
        if (entities.any { it.scale != 1.0 }) {
            entities.forEach { it.scale = 1.0 }
            panel.odorWorldComponent.markChanged()
        }
    }

    /**
     * One resize gesture, started on [corner] of [grabbed]. Every selected entity, captured at the start, is scaled by
     * the ratio the grabbed entity's scale moved through.
     */
    private inner class ResizeDrag(
        private val grabbed: OdorWorldEntity,
        corner: ResizeCorner,
        start: PInputEvent
    ) {
        private val startScales: Map<OdorWorldEntity, Double> =
            (panel.selectedEntityModels + grabbed).associateWith { it.scale }

        private val grabbedStartScale = grabbed.scale

        private val cornerOffset = Point2D.Double(corner.signX * grabbed.width / 2, corner.signY * grabbed.height / 2)

        private val grabOffset = start.position.let { Point2D.Double(it.x - grabbed.x, it.y - grabbed.y) }

        private val nativeCornerDistance = grabbed.entityType.let {
            sqrt(it.width.toDouble() * it.width + it.height.toDouble() * it.height) / 2
        }

        private val ratioRange = ResizeScaling.ratioLimits(
            startScales.map { (entity, scale) ->
                Triple(scale, entity.entityType.width.toDouble(), entity.entityType.height.toDouble())
            },
            grabbed.world.width,
            grabbed.world.height
        )

        private val readout = object : PText() {
            override fun paint(paintContext: PPaintContext) {
                super.paint(paintContext)
                val g = paintContext.graphics
                g.color = UIManager.getColor("ToolTip.borderColor") ?: UIManager.getColor("Component.borderColor") ?: Color.GRAY
                val b = boundsReference
                g.draw(Rectangle2D.Double(b.x, b.y, b.width - 1, b.height - 1))
            }
        }.apply {
            font = Theme.small
            textPaint = UIManager.getColor("ToolTip.foreground") ?: Color.BLACK
            paint = UIManager.getColor("ToolTip.background") ?: Color.WHITE
            pickable = false
        }

        private var changed = false

        init {
            panel.isDraggingEntity = true
            camera.addChild(readout)
            updateReadout(start)
        }

        fun update(event: PInputEvent) {
            val cursorOffset = event.position.let { Point2D.Double(it.x - grabbed.x, it.y - grabbed.y) }
            val raw = ResizeScaling.rawScale(grabbedStartScale, cornerOffset, grabOffset, cursorOffset)
            val snapped = ResizeScaling.snap(raw, event.isShiftDown, nativeCornerDistance, viewScale)
            val ratio = (snapped / grabbedStartScale).coerceIn(ratioRange)
            startScales.forEach { (entity, startScale) ->
                val newScale = startScale * ratio
                if (entity.scale != newScale) {
                    entity.scale = newScale
                    changed = true
                }
            }
            updateReadout(event)
        }

        private fun updateReadout(event: PInputEvent) {
            val w = grabbed.width.roundToInt()
            val h = grabbed.height.roundToInt()
            readout.text = " %.2f×   %d × %d ".format(grabbed.scale, w, h)
            val p = event.canvasPosition
            readout.setOffset(p.x + 14, p.y + 14)
        }

        fun finish() {
            readout.removeFromParent()
            panel.isDraggingEntity = false
            if (changed) panel.odorWorldComponent.markChanged()
        }
    }

    companion object {
        /** Side of the drawn square, in screen pixels. */
        const val VISIBLE_SIZE = 8.0

        /** Side of the area that grabs the handle, in screen pixels; larger than the square so it is easy to hit. */
        const val HIT_SIZE = 12.0

        /** How far the selection outline sits outside the body, as a fraction of its size; see [org.simbrain.network.gui.nodes.NodeHandle]. */
        const val SELECTION_OUTLINE_EXTENSION = 0.075
    }
}
