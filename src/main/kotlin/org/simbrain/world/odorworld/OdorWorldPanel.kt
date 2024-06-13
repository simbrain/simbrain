package org.simbrain.world.odorworld

import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventListener
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.gui.CustomToolBar
import org.simbrain.util.*
import org.simbrain.util.piccolo.*
import org.simbrain.world.odorworld.dialogs.EntityDialog
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.gui.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.*
import java.util.Timer
import javax.swing.*
import kotlin.math.min
import kotlin.math.pow

/**
 * **OdorWorldPanel** represent the OdorWorld.
 */
class OdorWorldPanel(
    val odorWorldComponent: OdorWorldComponent,
    val world: OdorWorld
) : JPanel() {

    /**
     * The Piccolo PCanvas.
     */
    val canvas = OdorWorldCanvas()

    /**
     * Selection model.
     */
    val selectionManager = WorldSelectionManager()

    /**
     * Provisional interface for selecting tiles.
     */
    private var tileSelectionBox: PNode? = null
    private var tileSelectionModel: Rectangle? = null

    /**
     * Timer to update entity animations.
     */
    private var animationTimer: Timer? = null

    /**
     * Timer used for updating entity position when they are manually moved. Allows timing of
     * manualy movement to be consistent whether the workspace is running or not.
     */
    private var movementTimer: Timer? = null

    /**
     * Used for a mask that allows multiple movements to be applied at once.
     * Lower four bits are used for U,D,L,R.
     */
    private var manualMovementKeyState: Byte = 0

    /**
     * List corresponding to the layers of a tmx file.
     */
    private var layerImageList: List<PImage?>

    val odorWorldActions: OdorWorldActions = OdorWorldActions(this)

    /**
     * The current zoom level of the canvas.
     *
     * For example:
     * 0.5 means the canvas is rendered (zoomed out) at 0.5 of its normal size, and 2 means it is rendered (zoomed in) to twice its size
     *
     * The setter rescales the canvas.
     */
    var scalingFactor: Double
        get() = canvas.camera.viewScale
        set(scalingFactor) {
            val currentScalingFactor = canvas.camera.viewScale
            val scalingFactorRatio = scalingFactor / currentScalingFactor
            canvas.scale(scalingFactorRatio)
        }

    fun debugToolTips() {
        if (tileSelectionModel != null) {
            if (!tileSelectionModel!!.contains(world.lastClickedPosition)) {
                canvas.layer.removeChild(tileSelectionBox)
                tileSelectionModel = null
                tileSelectionBox = null
            }
        }

        if (tileSelectionBox == null) {
            val tileCoordinateX = (world.lastClickedPosition.x / world.tileMap.tileWidth).toInt()
            val tileCoordinateY = (world.lastClickedPosition.y / world.tileMap.tileHeight).toInt()
            tileSelectionModel = Rectangle(
                tileCoordinateX * world.tileMap.tileWidth,
                tileCoordinateY * world.tileMap.tileHeight,
                world.tileMap.tileWidth,
                world.tileMap.tileHeight
            )
            tileSelectionBox = PPath.createRectangle(
                tileSelectionModel!!.getX(),
                tileSelectionModel!!.getY(),
                tileSelectionModel!!.getWidth(),
                tileSelectionModel!!.getHeight()
            ).apply {
                strokePaint = Color.ORANGE
                setPaint(null)
            }
            canvas.layer.addChild(tileSelectionBox)
        }
    }

    /**
     * Extend PCanvas for custom handling of tooltips
     */
    inner class OdorWorldCanvas : PCanvas() {
        init {
            // Paradoxically, low quality rendering produces sharper quality for rendering pixel images
            // use nearest neighbor instead of bi-linear
            setDefaultRenderQuality(PPaintContext.LOW_QUALITY_RENDERING)
            animatingRenderQuality = PPaintContext.LOW_QUALITY_RENDERING
            interactingRenderQuality = PPaintContext.LOW_QUALITY_RENDERING
        }

        override fun getToolTipText(event: MouseEvent): String {
            val allNode = ArrayList((canvas.layer.allNodes as Collection<PNode?>).stream().toList())
            allNode.reverse()

            val firstNode = allNode.stream()
                .filter { it: PNode? ->
                    if (it is PImage) {
                        return@filter true
                    } else if (it is EntityNode) {
                        return@filter it.getFullBounds().contains(camera.localToView(event.point))
                    }
                    false
                }
                .findFirst()

            if (firstNode.isEmpty) {
                return ""
            }

            if (firstNode.get() is PImage) {
                val selectedTiles = getTileStack(event.point) ?: return ""
                val sb = StringBuilder("Tile Ids: ")
                selectedTiles.stream().filter { obj: Tile? -> Objects.nonNull(obj) }
                    .forEach { tile: Tile -> sb.append(" (" + tile.id + ")") }

                return sb.toString()
            } else if (firstNode.get() is EntityNode) {
                return (firstNode.get() as EntityNode).entity.toString()
            }
            return ""
        }

        /**
         * Change the current zoom level up or down by the scaling factor.
         *
         * For example:
         * 1.1 zooms in by ~10%
         * 0.9 zooms out by ~10%
         */
        fun scale(scalingFactor: Double) {
            val canvasCenter: Point2D = camera.bounds.center
            val (x, y) = camera.localToView(canvasCenter)
            val newWidth = camera.viewBounds.width / scalingFactor
            val newHeight = camera.viewBounds.height / scalingFactor
            val newX = x - newWidth / 2
            val newY = y - newHeight / 2
            canvas.setViewBounds(Rectangle2D.Double(newX, newY, newWidth, newHeight))
        }

        fun setViewBounds(bounds: Rectangle2D) {
            val (x, y, w, h) = bounds
            val newWidth = min(w, world.width)
            val newHeight = min(h, world.height)
            val newX = x.coerceIn(0.0, world.width - newWidth)
            val newY = y.coerceIn(0.0, world.height - newHeight)
            canvas.camera.setViewBoundsNoOverflow(
                Rectangle2D.Double(
                    newX,
                    newY,
                    newWidth,
                    newHeight
                )
            )
        }
    }

    /**
     * Construct a world, set its background color.
     *
     * @param world the frame in which this world is rendered
     */
    init {
        ToolTipManager.sharedInstance().registerComponent(canvas)

        layout = BorderLayout()
        this.add("Center", canvas)
        add("North", createMainToolBar())

        canvas.isFocusable = true

        // Add tile map
        layerImageList = world.tileMap.createImageList()
        canvas.layer.addChildren(layerImageList)

        // Remove default event handlers
        val panEventHandler: PInputEventListener = canvas.panEventHandler
        val zoomEventHandler: PInputEventListener = canvas.zoomEventHandler
        canvas.removeInputEventListener(panEventHandler)
        canvas.removeInputEventListener(zoomEventHandler)

        // Add key bindings
        OdorWorldKeyBindings.addBindings(this)

        // Mouse events
        canvas.addInputEventListener(WorldMouseHandler(this, world))
        canvas.addInputEventListener(WorldContextMenuEventHandler(this, world))

        world.events.entityAdded.on(swingDispatcher) { e: OdorWorldEntity? ->
            val node = EntityNode(e!!)
            canvas.layer.addChild(node)
            selectionManager.clear()
            selectionManager.add(node)
        }
        world.events.updated.on(swingDispatcher, wait = true) { this.centerCameraToSelectedEntity() }
        world.events.frameAdvanced.on(swingDispatcher) {
            canvas.layer.childrenReference
                .filterIsInstance<EntityNode>()
                .forEach { it.advance() }
            repaint()
        }
        world.events.animationStopped.on(swingDispatcher) {
            // When movement is stopped use the "static" animation so we don't show entities in strange
            // intermediate states
            canvas.layer.childrenReference
                .filterIsInstance<EntityNode>()
                .forEach { it.resetToStaticFrame() }
            repaint()
        }

        // Full tile map update
        world.events.tileMapChanged.on(swingDispatcher) {
            world.tileMap.events.layersChanged.on(swingDispatcher) {
                renderAllLayers(world)
            }
            world.tileMap.events.mapSizeChanged.on(swingDispatcher) {
                world.events.tileMapChanged.fire()
            }

            // Single layer update
            world.tileMap.events.layerImageChanged.on(
                wait = true, dispatcher = swingDispatcher
            ) { oldImage: PImage?, newImage: PImage? ->
                val index = canvas.layer.indexOfChild(oldImage)
                canvas.layer.removeChild(oldImage)
                if (index != -1) {
                    canvas.layer.addChild(index, newImage)
                } else {
                    canvas.layer.addChild(newImage)
                }
            }
            renderAllLayers(world)
        }

        world.events.worldStarted.on(null, true) {
            if (movementTimer != null) {
                movementTimer?.cancel()
                movementTimer = null
            }
            if (animationTimer == null) {
                animationTimer = Timer().apply {
                    schedule(object : TimerTask() {
                        override fun run() {
                            world.events.frameAdvanced.fire()
                        }
                    }, 50, 50)
                }
            }
        }

        world.events.worldStopped.on(null, true, Runnable {
            movementTimer = Timer().apply {
                schedule(object : TimerTask() {
                    override fun run() {
                        manualMovementUpdate()
                    }
                }, 10, 10)
            }
            if (animationTimer != null) {
                animationTimer?.cancel()
                animationTimer = null
            }
        })

        canvas.addInputEventListener(object : PBasicInputEventHandler() {
            override fun mouseWheelRotated(event: PInputEvent) {
                val swingEvent = (event.sourceSwingEvent as MouseWheelEvent)
                val newScale = 1.1.pow(swingEvent.preciseWheelRotation)
                canvas.scale(1 / newScale)
            }
        })

        movementTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    manualMovementUpdate()
                }
            }, 10, 10)
        }

        world.events.tileMapChanged.fire()

        canvas.setViewBounds(Rectangle2D.Double(0.0, 0.0, world.width, world.height))

        // Repaint whenever window is opened or changed.
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(arg0: ComponentEvent) {
                scalingFactor = scalingFactor // force invoke setter
            }
        })

        odorWorldActions.createSelectAllAction()
    }

    private fun renderAllLayers(world: OdorWorld) {
        canvas.layer.removeAllChildren()
        layerImageList = world.tileMap.createImageList()
        canvas.layer.addChildren(layerImageList)
        for (oe in world.entityList) {
            val node = EntityNode(oe)
            canvas.layer.addChild(node)
        }
        repaint()
    }

    private fun centerCameraToSelectedEntity() {
        if (!world.isUseCameraCentering) {
            repaint()
            return
        }

        if (selectedEntityModels.isNotEmpty()) {
            canvas.setViewBounds(
                Rectangle2D.Double(
                    firstSelectedEntityModel!!.x - canvas.camera.viewBounds.width / 2,
                    firstSelectedEntityModel!!.y - canvas.camera.viewBounds.height / 2,
                    canvas.camera.viewBounds.width,
                    canvas.camera.viewBounds.height
                )
            )
            repaint()
        }
    }

    fun getTileStack(point: Point2D?): List<Tile> {
        return world.tileMap.getTileStackAtPixel(point!!)
    }

    fun getTile(point: Point2D?): Tile? {
        val tileStack = world.tileMap.getTileStackAtPixel(
            point!!
        )
        return if (tileStack == null) {
            null
        } else {
            tileStack[0]
        }
    }

    fun manualMovementUpdate() {
        val entityNode = firstSelectedEntityNode
        if (entityNode != null && isManualMovementMode) {
            val entity = entityNode.entity
            entity.applyMovement()
            entityNode.advance()
            centerCameraToSelectedEntity()
        }
    }

    /**
     * Show the PNode debugging tool.
     */
    fun showPNodeDebugger() {
        val sgb = SceneGraphBrowser(
            canvas.root
        )
        val dialog = StandardDialog()
        dialog.contentPane = sgb
        dialog.title = "Piccolo Scenegraph Browser"
        dialog.isModal = false
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }


    val selectedEntityNodes: List<EntityNode>
        get() =
            selection
                .map {
                    if (it is EntityNode) {
                        it
                    } else {
                        it.parent
                    }
                }
                .filterIsInstance<EntityNode>()

    val selectedEntityModels: List<OdorWorldEntity>
        get() = selectedEntityNodes
            .map { it.entity }

    val firstSelectedEntityNode: EntityNode?
        get() = selectedEntityNodes.firstOrNull()

    val firstSelectedRotatingEntity: OdorWorldEntity?
        get() = selectedEntityModels.firstOrNull { it.entityType.isRotating }

    val firstSelectedEntityModel: OdorWorldEntity?
        get() {
            if (firstSelectedEntityNode != null) {
                return firstSelectedEntityNode!!.entity
            }
            return null
        }

    /**
     * Delete all current selected entities.
     */
    fun deleteSelectedEntities() {
        selectedEntityModels.forEach { it.delete() }
    }

    /**
     * Popup menu when not clicking on entity. On an entity see [EntityNode.createContextMenu]
     */
    fun getContextMenu() = JPopupMenu().apply {
        add(JMenuItem(odorWorldActions.addAgentAction()))
        add(JMenuItem(odorWorldActions.addEntityAction()))
        addSeparator()
        add(JMenuItem(odorWorldActions.addTileAction))
        add(JMenuItem(odorWorldActions.fillLayerAction))
        add(odorWorldActions.createChooseLayerMenu(world))
        add(odorWorldActions.editLayersAction)
        addSeparator()
        add(JMenuItem(odorWorldActions.showWorldPrefsAction()))
    }

    fun clearSelection() {
        selectionManager.clear()
    }

    var selection: MutableSet<PNode>
        get() = selectionManager.selection
        set(elements) {
            selectionManager.clear()
            selectionManager.addAll(elements)
        }

    /**
     * Return true if the specified element is selected.
     *
     * @param element element
     * @return true if the specified element is selected
     */
    fun isSelected(element: Any?): Boolean {
        return selectionManager.isSelected(element)
    }


    /**
     * Toggle the selected state of the specified element; if it is selected,
     * remove it from the selection, if it is not selected, add it to the
     * selection.
     *
     * @param element element
     */
    fun toggleSelection(element: PNode) {
        if (isSelected(element)) {
            selectionManager.remove(element)
        } else {
            selectionManager.add(element)
        }
    }

    private fun getManualMovementStateMask(key: String): Byte {
        var mask: Byte = 0
        if ("w".equals(key, ignoreCase = true)) {
            mask = 1
        } else if ("s".equals(key, ignoreCase = true)) {
            mask = 2
        } else if ("a".equals(key, ignoreCase = true)) {
            mask = 4
        } else if ("d".equals(key, ignoreCase = true)) {
            mask = 8
        }
        return mask
    }

    fun setManualMovementKeyState(key: String, state: Boolean) {
        val mask = getManualMovementStateMask(key)
        manualMovementKeyState = if (state) {
            (manualMovementKeyState.toInt() or mask.toInt()).toByte()
        } else {
            (manualMovementKeyState.toInt() and mask.toInt().inv()).toByte()
        }
    }

    fun getManualMovementState(key: String): Boolean {
        val mask = getManualMovementStateMask(key)
        return (manualMovementKeyState.toInt() and mask.toInt()) > 0
    }

    private val isManualMovementMode: Boolean
        get() = manualMovementKeyState > 0

    private fun createMainToolBar() = CustomToolBar().apply {
        add(odorWorldActions.addAgentAction())
        add(odorWorldActions.addEntityAction())
        addSeparator()
        add(odorWorldActions.resetZoomAction())
        add(odorWorldActions.zoomInAction())
        add(odorWorldActions.zoomOutAction())
    }

    fun editSelectedEntities() {
        selectedEntityNodes.firstOrNull()?.let {
            EntityDialog(it.entity).apply { title = "Edit ${it.entity.name}" }.display()
        }
    }
}

