package org.simbrain.util.piccolo

import com.formdev.flatlaf.extras.FlatSVGIcon
import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import java.awt.Graphics2D

/**
 * Paints a classpath SVG icon onto the Piccolo canvas. The SVG document renders through the
 * camera's live transform, so the icon stays vector-crisp at any zoom, and the global FlatLaf
 * color filter applies at paint time, so it recolors with light/dark theme switches for free.
 */
class SvgIconNode(resource: String, size: Double) : PNode() {

    private val icon = FlatSVGIcon(resource, size.toInt(), size.toInt())

    init {
        setBounds(0.0, 0.0, size, size)
        pickable = false
    }

    override fun paint(paintContext: PPaintContext) {
        val g2 = paintContext.graphics.create() as Graphics2D
        try {
            g2.translate(x, y)
            icon.paintIcon(null, g2, 0, 0)
        } finally {
            g2.dispose()
        }
    }
}
