package org.simbrain.world.odorworld.gui

import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.util.Utils
import org.simbrain.util.math.SimbrainMath
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.TileSensor
import java.awt.Color
import java.awt.geom.GeneralPath
import java.awt.geom.Point2D

class TileSensorNode(override val sensor: TileSensor) : EntityAttributeNode(), NodeWithDispersion by DispersionNode(sensor) {
    /**
     * The shape of this node
     */
    private val shape: PPath

    /**
     * The text graphical object
     */
    private val labelText: PText

    /**
     * The text label location
     */
    private val labelBottomCenterLocation = Point2D.Float(0f, -5f)

    init {
        val squarePath = GeneralPath()
        squarePath.moveTo(-SENSOR_RADIUS.toFloat(), -SENSOR_RADIUS.toFloat())
        squarePath.lineTo(-SENSOR_RADIUS.toFloat(), SENSOR_RADIUS.toFloat())
        squarePath.lineTo(SENSOR_RADIUS.toFloat(), SENSOR_RADIUS.toFloat())
        squarePath.lineTo(SENSOR_RADIUS.toFloat(), -SENSOR_RADIUS.toFloat())
        squarePath.closePath()
        shape = PPath.Float(squarePath)
        pickable = false
        shape.setPickable(false)
        addChild(shape)
        labelText = PText()
        labelText.pickable = false
        labelText.font = labelText.font.deriveFont(9.0f)
        updateLabel()
        shape.addChild(labelText)
        drawDispersionCircleAround(shape)
        sensor.events.propertyChanged.on {
            updateLabel()
            drawDispersionCircleAround(shape)
        }
    }

    override fun update(entity: OdorWorldEntity) {
        shape.offset = sensor.computeRelativeLocation(entity)
        val saturation = SimbrainMath.rescale(
            sensor.currentValue, 0.0, sensor.baseValue,
            0.0, 1.0
        ).toFloat()
        shape.paint = Color.getHSBColor(maxColor, saturation, 1f)
    }

    fun updateLabel() {
        // TODO: If there is more than one sensor in one spot, labels are on top of each others
        if (sensor.isShowLabel) {
            labelText.text = Utils.getWrapAroundString(sensor.label, 10)
            labelText.setOffset(
                labelBottomCenterLocation.getX() - labelText.width / 2,
                labelBottomCenterLocation.getY() - labelText.height
            )
        }
        labelText.visible = sensor.isShowLabel
    }

    companion object {
        private const val SENSOR_RADIUS = 3
    }
}