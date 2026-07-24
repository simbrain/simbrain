package org.simbrain.world.odorworld.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.Theme
import org.simbrain.util.distanceTo
import org.simbrain.util.minus
import org.simbrain.util.toRadian
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.CouplingMenu
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.ProceduralEntityImages
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.getCurrentImage
import org.simbrain.world.odorworld.sensors.Sensor
import org.simbrain.world.odorworld.sensors.VisualizableEntityAttribute
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.util.stream.Collectors
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
            val bounds = boundsReference
            val g = paintContext.graphics
            ProceduralEntityImages.getBaseImage(entity)?.let { image ->
                g.translate(bounds.centerX, bounds.centerY)
                g.rotate(-entity.heading.toRadian())
                g.scale(bounds.width / image.width, bounds.height / image.height)
                g.drawImage(image, -image.width / 2, -image.height / 2, null)
                g.scale(image.width / bounds.width, image.height / bounds.height)
                g.rotate(entity.heading.toRadian())
                g.translate(-bounds.centerX, -bounds.centerY)
                return
            }

            val image = entity.getCurrentImage()

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
     * Overlay that draws per-candidate context-steering rays plus wall feeler hits.
     * Visible only when [OdorWorldEntity.showSteeringDebug] is true.
     */
    private inner class SteeringDebugNode : PNode() {
        init {
            pickable = false
            val r = 200.0
            setBounds(-r, -r, 2 * r, 2 * r)
        }

        override fun paint(paintContext: PPaintContext) {
            if (!entity.showSteeringDebug) return
            val info = entity.steeringDebug ?: return
            val g = paintContext.graphics
            val n = info.scores.size
            val originalStroke = g.stroke
            val originalColor = g.color
            val originalFont = g.font

            if (n > 0) {
                val maxAbs = info.scores.maxOf { abs(it) }.coerceAtLeast(1e-3)
                val baseLen = 8.0
                val rangeLen = 60.0

                for (k in 0 until n) {
                    val angle = info.headings[k]
                    val rad = angle.toRadian()
                    val dirX = cos(rad)
                    val dirY = -sin(rad)
                    val s = info.scores[k]
                    val len = baseLen + rangeLen * (abs(s) / maxAbs).coerceIn(0.0, 1.0)
                    val isChosen = angle == info.chosenHeading
                    g.color = when {
                        isChosen -> Color(255, 220, 0)
                        s >= 0 -> Color(0, 200, 0, 180)
                        else -> Color(220, 60, 60, 180)
                    }
                    g.stroke = BasicStroke(if (isChosen) 3f else 1.2f)
                    g.draw(Line2D.Double(0.0, 0.0, dirX * len, dirY * len))
                }

                g.color = Color(255, 0, 255, 200)
                g.stroke = BasicStroke(1.5f)
                for (k in 0 until n) {
                    val dist = info.obstacleDistances[k]
                    if (dist < info.feelerLength) {
                        val rad = info.headings[k].toRadian()
                        val hx = cos(rad) * dist
                        val hy = -sin(rad) * dist
                        g.draw(Ellipse2D.Double(hx - 2.5, hy - 2.5, 5.0, 5.0))
                    }
                }
            }

            // Status text (behavior decision + actual movement + collision/stuck flags)
            val intendedV = info.intendedSpeed
            val actualV = sqrt(info.actualDx * info.actualDx + info.actualDy * info.actualDy)
            val lines = mutableListOf<Pair<String, Color>>()
            if (info.behaviorNotes.isNotEmpty()) lines.add(info.behaviorNotes to Color.WHITE)
            lines.add("speed: %.2f → %.2f".format(intendedV, actualV) to Color.WHITE)
            if (info.collided) lines.add("COLLIDED" to Color(255, 120, 120))
            if (entity.wasStuckLastTick) lines.add("STUCK (no progress)" to Color(255, 120, 120))

            g.font = Theme.small
            val fm = g.fontMetrics
            var ty = entity.height / 2 + fm.ascent + 2
            for ((text, color) in lines) {
                val w = fm.stringWidth(text)
                val tx = -w / 2.0
                g.color = Color(0, 0, 0, 150)
                g.fillRect((tx - 2).toInt(), (ty - fm.ascent).toInt(), w + 4, fm.height)
                g.color = color
                g.drawString(text, tx.toFloat(), ty.toFloat())
                ty += fm.height
            }

            g.stroke = originalStroke
            g.color = originalColor
            g.font = originalFont
        }
    }

    private val steeringDebugNode = SteeringDebugNode()

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
        addChild(steeringDebugNode)
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
            sprite.repaint()
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
        steeringDebugNode.repaint()
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
