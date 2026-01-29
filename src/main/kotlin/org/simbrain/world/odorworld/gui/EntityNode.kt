package org.simbrain.world.odorworld.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.distanceTo
import org.simbrain.util.minus
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.CouplingMenu
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.getCurrentImage
import org.simbrain.world.odorworld.sensors.Sensor
import org.simbrain.world.odorworld.sensors.VisualizableEntityAttribute
import java.awt.geom.Point2D
import java.util.stream.Collectors
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

/**
 * Piccolo representation of an [OdorWorldEntity].
 */
class EntityNode(
    val entity: OdorWorldEntity
) : PNode(), NodeWithDispersion by DispersionNode(entity) {

    /**
     * Simple sprite node that renders the entity's current image.
     * Uses the shared image utilities from OdorWorldUtils.
     */
    private inner class EntitySprite : PNode() {
        init {
            updateBounds()
        }

        fun updateBounds() {
            val w = entity.entityType.width.toDouble()
            val h = entity.entityType.height.toDouble()
            setBounds(-w / 2, -h / 2, w, h)
        }

        override fun paint(paintContext: PPaintContext) {
            val g = paintContext.graphics
            val image = entity.getCurrentImage()
            val bounds = boundsReference

            val imgW = image.width.toDouble()
            val imgH = image.height.toDouble()

            g.translate(bounds.x, bounds.y)
            g.scale(bounds.width / imgW, bounds.height / imgH)
            g.drawImage(image, 0, 0, null)
            g.scale(imgW / bounds.width, imgH / bounds.height)
            g.translate(-bounds.x, -bounds.y)
        }
    }

    /**
     * Sprite node for rendering the entity image.
     */
    private val sprite = EntitySprite()

    /**
     * Represents path taken by the agent, if [OdorWorldEntity.isShowTrail] is turned on
     */
    var trail: PPath = PPath.createPolyline(arrayOf(Point2D.Float(entity.x.toFloat(),entity.y.toFloat()))).apply {
        paint = null
        pickable = false
    }

    /**
     * A map from [VisualizableEntityAttribute] (model) to [EntityAttributeNode] (view).
     */
    private val visualizableAttributeMap: MutableMap<VisualizableEntityAttribute, EntityAttributeNode?> = HashMap()

    /**
     * Construct an entity node with a back-ref to parent.
     *
     * @param world  parent world
     * @param entity represented entity
     */
    init {
        addChild(sprite)
        updateEntityAttributeModel()
        setOffset(entity.x, entity.y)
        entity.events.deleted.on(dispatcher = Dispatchers.Swing) { removeFromParent() }
        entity.events.moved.on(dispatcher = Dispatchers.Swing) { update() }
        entity.events.typeChanged.on(dispatcher = Dispatchers.Swing) { _, _ ->
            sprite.updateBounds()
            sprite.repaint()
        }
        entity.events.trailVisibilityChanged.on(dispatcher = Dispatchers.Swing) { new, _ ->
            if (new) {
                trail = PPath.createPolyline(arrayOf(Point2D.Float(entity.x.toFloat(),entity.y.toFloat()))).apply {
                    paint = null
                }
                addChild(trail)
            } else {
                removeChild(trail)
            }
        }
        entity.events.trailCleared.on(dispatcher = Dispatchers.Swing) {
            removeChild(trail)
            trail = PPath.createPolyline(arrayOf(Point2D.Float(entity.x.toFloat(),entity.y.toFloat()))).apply {
                paint = null
            }
            addChild(trail)
        }

        fun updateSensorsEffectorsVisibility() {
            visualizableAttributeMap.values.forEach { it?.visible = entity.isShowSensorsAndEffectors }
        }

        updateSensorsEffectorsVisibility()
        entity.events.propertyChanged.on(dispatcher = Dispatchers.Swing) {
            updateSensorsEffectorsVisibility()
        }

        entity.events.updated.on(dispatcher = Dispatchers.Swing) { update() }
        entity.events.sensorAdded.on(dispatcher = Dispatchers.Swing) { s: Sensor? ->
            if (s is VisualizableEntityAttribute) {
                val toAdd = s as VisualizableEntityAttribute
                addAttribute(toAdd)
            }
        }
        entity.events.effectorAdded.on(dispatcher = Dispatchers.Swing) { e: Effector? ->
            if (e is VisualizableEntityAttribute) {
                val toAdd = e as VisualizableEntityAttribute
                addAttribute(toAdd)
            }
        }
        entity.events.sensorRemoved.on(dispatcher = Dispatchers.Swing) { s: Sensor? ->
            if (s is VisualizableEntityAttribute) {
                val toRemove = s as VisualizableEntityAttribute
                removeAttribute(toRemove)
            }
        }
        entity.events.effectorRemoved.on(dispatcher = Dispatchers.Swing) { e: Effector? ->
            if (e is VisualizableEntityAttribute) {
                val toRemove = e as VisualizableEntityAttribute
                removeAttribute(toRemove)
            }
        }
        entity.world.events.worldStarted.on(dispatcher = Dispatchers.Swing) {
            if (entity.isShowTrail && !entity.drawTrailWithoutRunningWorkspace) {
                trail.moveTo(entity.x, entity.y)
            }
        }
        drawDispersionCircleAround(this)
        entity.events.propertyChanged.on(dispatcher = Dispatchers.Swing) {
            drawDispersionCircleAround(this)
        }

        if (entity.isShowTrail) {
            addChild(trail)
        }
    }

    /**
     * Add an [VisualizableEntityAttribute].
     *
     * @param attribute the attribute to add
     */
    private fun addAttribute(attribute: VisualizableEntityAttribute) {
        val node = EntityAttributeNode.getNode(attribute)
        node.visible = entity.isShowSensorsAndEffectors
        visualizableAttributeMap[attribute] = node
        addChild(visualizableAttributeMap[attribute])
        node.update(entity)
    }

    /**
     * Remove an [VisualizableEntityAttribute]
     *
     * @param attribute the attribute to remove
     */
    private fun removeAttribute(attribute: VisualizableEntityAttribute) {
        removeChild(visualizableAttributeMap[attribute])
        visualizableAttributeMap.remove(attribute)
    }

    /**
     * Update the position of the model neuron based on the global coordinates of this pnode.
     */
    fun pushViewPositionToModel() {
        val p = this.globalTranslation
        entity.x = p.x
        entity.y = p.y
    }

    fun startTrailAtCurrentLocation() {
        trail.moveTo(entity.x, entity.y)
    }

    /**
     * Sync all visualizable entity attributes to this node. Should only be called on initialization or deserialization
     */
    private fun updateEntityAttributeModel() {
        val visualizableEntityAttributeList = entity.sensors.stream()
            .filter { obj: Sensor? -> VisualizableEntityAttribute::class.java.isInstance(obj) }
            .map { obj: Sensor? -> VisualizableEntityAttribute::class.java.cast(obj) }
            .collect(Collectors.toList())
        visualizableEntityAttributeList.addAll(
            entity.effectors.stream()
                .filter { obj: Effector? -> VisualizableEntityAttribute::class.java.isInstance(obj) }
                .map { obj: Effector? -> VisualizableEntityAttribute::class.java.cast(obj) }
                .collect(Collectors.toList())
        )
        for (vp in visualizableEntityAttributeList) {
            var currentEntityAttributeNode: EntityAttributeNode?
            if (!visualizableAttributeMap.containsKey(vp)) {
                currentEntityAttributeNode = EntityAttributeNode.getNode(vp)
                addChild(currentEntityAttributeNode)
                visualizableAttributeMap[vp] = currentEntityAttributeNode
            } else {
                currentEntityAttributeNode = visualizableAttributeMap[vp]
            }
            currentEntityAttributeNode!!.update(entity)
        }
    }

    /**
     * Update all visualizable attribute nodes.
     */
    private fun updateAttributesNodes() {
        visualizableAttributeMap.values.forEach { it?.update(entity) }
    }

    private fun update() {
        updateAttributesNodes()
        val isCrossingBorder = !entity.world.contains(entity.location - entity.velocity)
        setOffset(entity.x, entity.y)
        // Repaint sprite to show updated heading/animation frame
        sprite.repaint()
        if (entity.isShowTrail && (SimbrainDesktop.workspace.updater.isRunning || entity.drawTrailWithoutRunningWorkspace)) {
            if (isCrossingBorder) {
                trail.moveTo(entity.x, entity.y)
            }
            if (entity.location distanceTo trail.path.currentPoint > 0.25) { // don't add points too close to each other
                trail.lineTo(entity.x, entity.y)
            }
        }
        if (entity.isShowTrail) {
            trail.setOffset(-entity.x, -entity.y)
        }

    }

    /**
     * Advance the entity's animation frame based on velocity.
     */
    fun advance() {
        entity.advanceAnimation()
        sprite.repaint()
    }

    /**
     * Reset the entity's animation to the first frame (static pose).
     */
    fun resetToStaticFrame() {
        entity.resetAnimation()
        sprite.repaint()
    }

    fun createContextMenu(odorWorldPanel: OdorWorldPanel) = JPopupMenu().apply {
        add(odorWorldPanel.odorWorldActions.showPropertyDialogAction)
        add(odorWorldPanel.odorWorldActions.deleteSelectedAction())
        addSeparator()
        add(JMenuItem(odorWorldPanel.odorWorldActions.toggleTrailAction(entity)))
        addSeparator()
        add(
            SimbrainDesktop.actionManager.createCoupledDataWorldAction(
            name = "Record Locations",
            entity.getProducer(OdorWorldEntity::locationArray),
            sourceName = "${entity.id ?: "Entity"} Location",
            numCols = 2
        ))
        addSeparator()
        val couplingMenu = CouplingMenu(odorWorldPanel.odorWorldComponent, entity)
        couplingMenu.setCustomName("Create couplings")
        add(couplingMenu)
    }

    override fun getBounds(): PBounds {
        return sprite.bounds
    }
}
