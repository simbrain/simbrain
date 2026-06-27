package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.NetworkTheme
import org.simbrain.util.blend
import java.awt.geom.GeneralPath

enum class ArrowDirection { LEFT, RIGHT, UP, DOWN }

/**
 * Create a clickable triangular arrow button.
 *
 * @param direction which way the arrow points
 * @param size side length of the triangle in pixels
 * @param onClick callback invoked on click
 * @return a [PNode] container with the arrow shape and input listener
 */
fun createArrowButton(direction: ArrowDirection, size: Double = 8.0, onClick: () -> Unit): PNode {
    val padding = 3.0
    val totalW = size + padding * 2
    val totalH = size + padding * 2

    val cx = totalW / 2
    val cy = totalH / 2
    val hs = size / 2

    val shape = GeneralPath().apply {
        when (direction) {
            ArrowDirection.LEFT -> {
                moveTo((cx - hs).toFloat(), cy.toFloat())
                lineTo((cx + hs).toFloat(), (cy - hs).toFloat())
                lineTo((cx + hs).toFloat(), (cy + hs).toFloat())
            }
            ArrowDirection.RIGHT -> {
                moveTo((cx + hs).toFloat(), cy.toFloat())
                lineTo((cx - hs).toFloat(), (cy - hs).toFloat())
                lineTo((cx - hs).toFloat(), (cy + hs).toFloat())
            }
            ArrowDirection.UP -> {
                moveTo(cx.toFloat(), (cy - hs).toFloat())
                lineTo((cx - hs).toFloat(), (cy + hs).toFloat())
                lineTo((cx + hs).toFloat(), (cy + hs).toFloat())
            }
            ArrowDirection.DOWN -> {
                moveTo(cx.toFloat(), (cy + hs).toFloat())
                lineTo((cx - hs).toFloat(), (cy - hs).toFloat())
                lineTo((cx + hs).toFloat(), (cy - hs).toFloat())
            }
        }
        closePath()
    }

    // Fill the triangle with theme colors read fresh on every paint, so it tracks light/dark — both
    // its resting and hover state — and recolors on a live theme switch instead of caching colors.
    val arrowPath = object : PPath.Float(shape) {
        var hovered = false
        override fun paint(paintContext: PPaintContext) {
            paintContext.graphics.paint = if (hovered) {
                NetworkTheme.current.valueText
            } else {
                blend(NetworkTheme.current.valueText, NetworkTheme.current.canvasBackground, 0.5)
            }
            paintContext.graphics.fill(pathReference)
        }
    }
    arrowPath.stroke = null

    val container = PNode()
    container.setBounds(0.0, 0.0, totalW, totalH)
    container.addChild(arrowPath)

    container.addInputEventListener(object : PBasicInputEventHandler() {
        override fun mouseClicked(event: PInputEvent) {
            event.isHandled = true
            onClick()
        }
        override fun mouseEntered(event: PInputEvent) {
            arrowPath.hovered = true
            arrowPath.invalidatePaint()
        }
        override fun mouseExited(event: PInputEvent) {
            arrowPath.hovered = false
            arrowPath.invalidatePaint()
        }
    })

    return container
}
