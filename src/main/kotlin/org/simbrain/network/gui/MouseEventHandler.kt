package org.simbrain.network.gui

import org.piccolo2d.PCamera
import org.piccolo2d.PLayer
import org.piccolo2d.PNode
import org.piccolo2d.event.PDragSequenceEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PNodeFilter
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.topLeftLocation
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.*
import org.simbrain.util.ResourceManager.smallIconSize
import org.simbrain.util.piccolo.SelectionMarquee
import org.simbrain.util.piccolo.firstScreenElement
import org.simbrain.util.piccolo.screenElements
import java.awt.*
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BaseMultiResolutionImage
import java.awt.image.BufferedImage

class MouseEventHandler(val networkPanel: NetworkPanel) : PDragSequenceEventHandler() {

    private enum class Mode { SELECTION, PAN, DRAG }

    private var mode = Mode.DRAG

    private var priorSelection = setOf<ScreenElement>()

    private lateinit var marqueeStartPosition: Point2D

    private lateinit var marqueeEndPosition: Point2D

    /**
     * For undo / redo
     */
    private lateinit var startLocations: List<Point2D>

    /**
     * Red line that shows what the delta for the [PlacementManager] will be.
     */
    private var placementManagerDelta: PPath? = null

    /**
     * Stores the original autoZoom state to restore after dragging.
     */
    private var previousAutoZoomState: Boolean = true

    private val PInputEvent.isPanKeyDown get() = if (Utils.isMacOSX()) isMetaDown else isControlDown

    private val selectionMarquee by lazy {
        with(marqueeStartPosition) { SelectionMarquee(x.toFloat(), y.toFloat()) }.also {
            networkPanel.canvas.layer.addChild(it)
            it.visible = false
        }
    }

    init {
        // Only handle events in selection mode
        eventFilter = object : PInputEventFilter() {
            override fun acceptsEvent(event: PInputEvent, type: Int): Boolean {

                if (event.isPopupTrigger) return false
                // Allow lower-level listeners (e.g. per-pixel handlers on neuron-array / weight-matrix images)
                // to suppress the canvas-level selection by calling event.setHandled(true).
                if (event.isHandled) return false

                val mouseCursor = networkPanel.mouseCursor
                return mouseCursor == MouseCursor.Selection || mouseCursor == MouseCursor.Pan && super.acceptsEvent(event, type)
            }
        }
    }

    override fun mouseClicked(event: PInputEvent?) {
        super.mouseClicked(event)
        event?.position?.let {
            if (event.pickedNode.firstScreenElement == null) {
                networkPanel.network.placementManager.lastClickedLocation = it
            }
        }
    }

    /**
     * Handles beginnings of drag as well as single-click events.
     */
    override fun startDrag(event: PInputEvent) {

        super.startDrag(event)

        previousAutoZoomState = networkPanel.autoZoom

        val pickedNode: PNode? = event.pickedNode
        pickedNode?.firstScreenElement?.let { pickedScreenElement ->
            mode = Mode.DRAG
            networkPanel.autoZoom = false
            // Toggle selection
            if (event.isShiftDown) {
                networkPanel.selectionManager.toggle(pickedScreenElement)
            }
            // Required so that clicking to drag does not de-select all other nodes
            if (pickedScreenElement !in networkPanel.selectionManager.selection) {
                if (!event.isShiftDown) {
                    networkPanel.selectionManager.set(pickedScreenElement)
                }
            }
        }

        priorSelection = networkPanel.selectionManager.selection.toMutableSet()
        startLocations = networkPanel.selectionManager
            .filterSelectedModels<LocatableModel>()
            .map{ it.location.copy() }.toList()
        marqueeStartPosition = event.position
        marqueeEndPosition = event.position
        selectionMarquee.reset()

        if (event.isPanKeyDown || networkPanel.mouseCursor == MouseCursor.Pan) {
            mode = Mode.PAN
            networkPanel.autoZoom = false
        } else if (pickedNode is PCamera) {
            if (!event.isShiftDown) networkPanel.selectionManager.clear()
            mode = Mode.SELECTION
        }
    }

    override fun drag(event: PInputEvent) {
        super.drag(event)
        when (mode) {
            Mode.PAN -> pan(event)
            Mode.SELECTION -> select(event)
            Mode.DRAG -> dragItems(event)
        }
        marqueeEndPosition = event.position
    }


    override fun endDrag(event: PInputEvent) {
        super.endDrag(event)
        if (mode == Mode.SELECTION) {
            selectionMarquee.visible = false
        } else {
            dragItems(event)
            priorSelection = setOf()

            val models = networkPanel.selectionManager
                .filterSelectedModels<LocatableModel>()
            val startLocations = startLocations.toList()
            val endLocations = models.map { it.location }.toList()
            networkPanel.undoManager.addUndoableAction(
                description = "Move items",
                undo = {
                    models.zip(startLocations).forEach { (m, l) -> m.location = l }
                    networkPanel.network.events.zoomToFitPage.fire()
                },
                redo = {
                    models.zip(endLocations).forEach { (m, l) -> m.location = l }
                    networkPanel.network.events.zoomToFitPage.fire()
                }
            )
            // Reset the anchor point in the placement manager
            val topLeft = networkPanel.selectionManager.filterSelectedModels<LocatableModel>().topLeftLocation
            val pm = networkPanel.network.placementManager

            // Only reset the delta if alt/option key is down
            if (event.pickedNode != null && event.isAltDown) {
                event.pickedNode.firstScreenElement?.model.let {
                    if (it is LocatableModel) {
                        pm.customOffset = topLeft - (pm.customOffsetAnchor?.location ?: point(0, 0))
                    }
                }
            }
        }
        networkPanel.canvas.layer.removeChild(placementManagerDelta)

        networkPanel.autoZoom = previousAutoZoomState
    }

    /**
     * Pans the camera in response to the pan event provided. (From the source code for PanEventHandler. Note that
     * "autopan"--from that class--is not being used. Not sure what is being lost by not using it.)
     *
     * @param event contains details about the drag used to translate the view
     * @author Jesse Grosjean
     */
    private fun pan(event: PInputEvent) {
        val camera = event.camera!!
        if (camera.viewBounds.contains(event.position)) {
            with(event.delta) { camera.translateView(width, height) }
        }
    }

    private fun select(event: PInputEvent) {
        val bound = PBounds(rectangle(marqueeStartPosition, event.position)).apply {
            selectionMarquee.globalToLocal(this)
            selectionMarquee.reset() // todo: better way?
            selectionMarquee.append(Rectangle2D.Double(x, y, width, height), false)
            selectionMarquee.visible = true
        }

        val selectedNodes = networkPanel.canvas.layer.root.getAllNodes(
            BoundsFilter(bound), null
        ).filterIsInstance<ScreenElement>()

        val finalSelection = if (event.isShiftDown) {
            (priorSelection + selectedNodes) - (priorSelection intersect selectedNodes)
        } else {
            selectedNodes
        }
        networkPanel.selectionManager.set(finalSelection)
    }

    /**
     * Search through what's clicked on, upwards through parents, to find the first draggable item, and then
     * drag that. See [screenElements].
     */
    private fun dragItems(event: PInputEvent) {
        val delta = event.position - marqueeEndPosition
        val draggableElements = networkPanel.selectionManager.selection.map { it.screenElements.firstOrNull(ScreenElement::isDraggable) }
        draggableElements.forEach { it?.offset(delta.x, delta.y) }

        val placementManager = networkPanel.network.placementManager
        val selectionManager = networkPanel.selectionManager
        val customOffsetAnchor = placementManager.customOffsetAnchor

        // Show placementManagerDelta for placement manager
        if (event.isAltDown && customOffsetAnchor?.let { selectionManager.selectedModels.contains(it) } == false) {
            val topLeft = selectionManager.filterSelectedModels<LocatableModel>().topLeftLocation
            networkPanel.canvas.layer.removeChild(placementManagerDelta)
            placementManagerDelta = PPath.createLine(
                topLeft.x,
                topLeft.y,
                customOffsetAnchor.location.x,
                customOffsetAnchor.location.y
            ).apply {
                this.stroke = PPath.DEFAULT_STROKE
                this.strokePaint = Color.red
            }
            networkPanel.canvas.layer.addChild(placementManagerDelta)
        }
    }

    /**
     * A filter that determines whether a given pnode is selectable or not. Bounds are updated as the lasso tool is
     * dragged.
     */
    private class BoundsFilter(val bound: PBounds) : PNodeFilter {

        override fun accept(node: PNode): Boolean {
            val boundsIntersects = when (node) {
                is ScreenElement -> node.isIntersecting(bound)
                else -> node.globalBounds.intersects(bound)
            }
            return node.pickable && boundsIntersects && node !is PLayer && node !is PCamera && node !is SelectionMarquee
        }

        override fun acceptChildrenOf(node: PNode) =
            (node.childrenPickable || node is PCamera || node is PLayer) && node !is SelectionMarquee
    }

    sealed class MouseCursor {
        val centerPoint = point(9, 9)
        abstract val cursor: Cursor
        object Selection : MouseCursor() {
            override val cursor: Cursor get() = Cursor.getDefaultCursor()
        }
        object Wand : MouseCursor() {
            /**
             * Current wand color. Updated when wand action selection changes.
             */
            var wandColor: Color = Color(255, 230, 0, 220)
                private set

            /**
             * Current wand radius in pixels. This is the actual radius used for
             * both the cursor display and the affected area.
             */
            var wandRadius: Int = 40
                private set

            /**
             * Updates the wand cursor with new color and radius.
             * Call this when the selected wand action changes.
             */
            fun update(color: Color, radius: Int) {
                wandColor = color
                wandRadius = radius.coerceAtLeast(10)
                cursor = createWandCursor(wandColor, wandRadius)
            }

            override var cursor: Cursor = createWandCursor(wandColor, wandRadius)
                private set

            /**
             * Creates a wand cursor with the given color and radius.
             * Supports HiDPI/Retina displays using MultiResolutionImage.
             */
            private fun createWandCursor(color: Color, radius: Int): Cursor {
                val baseSize = radius + 4

                // Create 1x image
                val image1x = createWandImage(color, baseSize, 1.0)

                // Create 2x image for HiDPI
                val image2x = createWandImage(color, baseSize, 2.0)

                // Use MultiResolutionImage for automatic HiDPI support
                val multiResImage = BaseMultiResolutionImage(image1x, image2x)

                val center = baseSize / 2
                val hotspot = Point(center, center)
                return Toolkit.getDefaultToolkit().createCustomCursor(multiResImage, hotspot, "wand")
            }

            /**
             * Renders the wand cursor image at the given scale.
             */
            private fun createWandImage(color: Color, baseSize: Int, scale: Double): BufferedImage {
                val size = (baseSize * scale).toInt()
                val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
                val g2 = image.createGraphics()

                // High quality rendering hints
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

                val padding = 2.0 * scale
                val circleSize = (size - padding * 2).coerceAtLeast(4.0)

                // Use Ellipse2D for precise shapes
                val circle = java.awt.geom.Ellipse2D.Double(padding, padding, circleSize, circleSize)

                // Draw filled circle with action color (semi-transparent)
                g2.color = Color(color.red, color.green, color.blue, 160)
                g2.fill(circle)

                // Draw crisp border ring
                g2.color = Color(40, 40, 40)
                g2.stroke = BasicStroke((1.5 * scale).toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g2.draw(circle)

                g2.dispose()
                return image
            }
        }
        object Pan : MouseCursor() {
            override val cursor: Cursor = Toolkit.getDefaultToolkit()
                    .createCustomCursor(ResourceManager.getImage("menu_icons/Hand.png")
                        .getScaledInstance(smallIconSize, smallIconSize, Image.SCALE_SMOOTH), centerPoint, "pan")
        }
    }
}