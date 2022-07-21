package org.simbrain.world.odorworld.gui

import org.piccolo2d.nodes.PPath
import org.simbrain.world.odorworld.sensors.WithDispersion
import java.awt.BasicStroke
import java.awt.Color

interface NodeWithDispersion {
    val sensor: WithDispersion
    var dispersionCircle: PPath

    fun drawDispersionCircleAround(shape: PPath)
}

class DispersionNode(override val sensor: WithDispersion): NodeWithDispersion {
    override var dispersionCircle: PPath = makeDispersionCircle()

    private fun makeDispersionCircle(): PPath = with(sensor.decayFunction) {
        PPath.createEllipse(
            -dispersion / 2,
            -dispersion / 2,
            dispersion,
            dispersion
        ).apply {
            paint = null
            stroke = BasicStroke(
                1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0f, floatArrayOf(3f), 0f
            )
            strokePaint = Color.gray
        }
    }

    override fun drawDispersionCircleAround(shape: PPath) {
        shape.removeChild(dispersionCircle)
        if (sensor.showDispersion) {
            dispersionCircle = makeDispersionCircle()
            shape.addChild(dispersionCircle)
        }
    }
}