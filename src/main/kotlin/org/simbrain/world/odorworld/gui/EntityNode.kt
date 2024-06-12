package org.simbrain.world.odorworld.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.simbrain.util.*
import org.simbrain.util.piccolo.Animations
import org.simbrain.util.piccolo.RotatingSprite
import org.simbrain.util.piccolo.Sprite
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.CouplingMenu
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.OdorWorldResourceManager
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.RotatingEntityManager
import org.simbrain.world.odorworld.sensors.Sensor
import org.simbrain.world.odorworld.sensors.VisualizableEntityAttribute
import java.awt.geom.Point2D
import java.util.stream.Collectors
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import kotlin.math.absoluteValue

/**
 * Piccolo representation of an [OdorWorldEntity].
 */
class EntityNode(
    val entity: OdorWorldEntity
) : PNode(), NodeWithDispersion by DispersionNode(entity) {

    /**
     * Sprite representing this entity.
     */
    lateinit var sprite: Sprite

    /**
     * Represents path taken by the agent, if [OdorWorldEntity.isShowTrail] is turned on
     */
    var trail: PPath = PPath.createPolyline(arrayOf(Point2D.Float(entity.x.toFloat(),entity.y.toFloat()))).apply {
        paint = null
    }

    /**
     * For advancing animation proportional to velocity.
     */
    private var frameCounter = 0.0

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
        updateImage()
        updateEntityAttributeModel()
        setOffset(entity.x, entity.y)
        entity.events.deleted.on { e: OdorWorldEntity? -> removeFromParent() }
        entity.events.moved.on(Dispatchers.Swing) { update() }
        entity.events.typeChanged.on { o: EntityType?, n: EntityType? -> updateImage() }
        entity.events.trailVisibilityChanged.on { new, _ ->
            if (new) {
                trail = PPath.createPolyline(arrayOf(Point2D.Float(entity.x.toFloat(),entity.y.toFloat()))).apply {
                    paint = null
                }
                addChild(trail)
            } else {
                removeChild(trail)
            }
        }
        entity.events.trailCleared.on {
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
        entity.events.propertyChanged.on {
            updateSensorsEffectorsVisibility()
        }

        entity.events.updated.on(Dispatchers.Swing) { update() }
        entity.events.sensorAdded.on { s: Sensor? ->
            if (s is VisualizableEntityAttribute) {
                val toAdd = s as VisualizableEntityAttribute
                addAttribute(toAdd)
            }
        }
        entity.events.effectorAdded.on { e: Effector? ->
            if (e is VisualizableEntityAttribute) {
                val toAdd = e as VisualizableEntityAttribute
                addAttribute(toAdd)
            }
        }
        entity.events.sensorRemoved.on { s: Sensor? ->
            if (s is VisualizableEntityAttribute) {
                val toRemove = s as VisualizableEntityAttribute
                removeAttribute(toRemove)
            }
        }
        entity.events.effectorRemoved.on { e: Effector? ->
            if (e is VisualizableEntityAttribute) {
                val toRemove = e as VisualizableEntityAttribute
                removeAttribute(toRemove)
            }
        }
        entity.world.events.worldStarted.on {
            trail.moveTo(entity.x, entity.y)
        }
        drawDispersionCircleAround(this)
        entity.events.propertyChanged.on {
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

    /**
     * Initialize the image associated with the object. Only called when changing the image.
     */
    private fun updateImage() {
        if (::sprite.isInitialized) {
            removeChild(sprite)
        }
        when (entity.entityType) {
            EntityType.SWISS, EntityType.GOUDA, EntityType.POISON, EntityType.BELL, EntityType.FLOWER, EntityType.TULIP, EntityType.PANSY, EntityType.FLAX, EntityType.FISH -> sprite =
                Sprite(
                    OdorWorldResourceManager.getStaticImage(
                        entity.entityType.description + ".gif"
                    )
                )

            EntityType.CANDLE -> sprite = Sprite(OdorWorldResourceManager.getStaticImage("Candle.png"))
            EntityType.DANDELIONS -> sprite = Sprite(OdorWorldResourceManager.getStaticImage("Dandelions.png"))
            EntityType.GERANIUMS -> sprite = Sprite(OdorWorldResourceManager.getStaticImage("Geraniums.png"))
            EntityType.BLUECHEESE -> sprite = Sprite(OdorWorldResourceManager.getStaticImage("Bluecheese.gif"))
            EntityType.MOUSE -> sprite = RotatingSprite(RotatingEntityManager.getMouse())
            EntityType.CIRCLE -> sprite = RotatingSprite(
                Animations.createAnimation(
                    OdorWorldResourceManager.getBufferedImage("circle.png")
                )
            )

            EntityType.AMY, EntityType.ARNO, EntityType.BOY, EntityType.COW, EntityType.GIRL, EntityType.JAKE, EntityType.LION, EntityType.STEVE, EntityType.SUSI -> sprite =
                RotatingSprite(
                    RotatingEntityManager.getRotatingTileset(
                        entity.entityType.name, 8
                    )
                )

            EntityType.ISOPOD -> sprite = RotatingSprite(RotatingEntityManager.getRotatingTileset("isopod", 2))
            else -> {}
        }
        addChild(sprite)
        visualizableAttributeMap.values.forEach { it?.raiseToTop() }
        updateAttributesNodes()
        if (entity.isRotating) {
            (sprite as RotatingSprite?)!!.updateHeading(entity.heading)
        }
    }

    private fun update() {
        if (entity.isRotating) {
            (sprite as RotatingSprite?)!!.updateHeading(entity.heading)
        }
        updateAttributesNodes()
        setOffset(entity.x, entity.y)
        if (entity.isShowTrail && SimbrainDesktop.workspace.updater.isRunning) {
            fun isCrossingBroder(): Boolean {
                val delta = (trail.path.currentPoint - entity.location).magnitude.absoluteValue
                val velocity = entity.speed.absoluteValue
                return delta > velocity + 0.5 // add a little bit of tolerance
            }
            if (isCrossingBroder()) {
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
     * Advancing animation frame based on the velocity of the entity.
     */
    fun advance() {
        frameCounter += entity.speed / 5
        var i = 0
        while (i < frameCounter) {
            sprite.advance()
            i++
        }
        frameCounter -= i.toDouble()
    }

    /**
     * Set the sprite to frame where the entity is standing still.
     */
    fun resetToStaticFrame() {
        sprite.resetToStaticFrame()
    }

    fun createContextMenu(odorWorldPanel: OdorWorldPanel) = JPopupMenu().apply {
        add(odorWorldPanel.odorWorldActions.showPropertyDialogAction)
        add(odorWorldPanel.createAction(name = "Delete entity") {
            entity.delete()
        })
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