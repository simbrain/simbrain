/**
 * Small caption showing an object's shape under tensors, convolution kernels, and weight matrices, so
 * adjacent shapes can be compared at a glance. Visibility follows [NetworkPreferences.showShapeCaptions].
 */
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import java.awt.Paint
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

/**
 * Unlike PText, whose bounds include the font's leading and descent, the bounds here hug the drawn glyphs.
 * Parents that pad their border around their children would otherwise show extra space under the caption.
 */
class ShapeCaption(text: String = "") : PNode() {

    var text: String = text
        set(value) {
            field = value
            recomputeBounds()
        }

    private val font = Theme.small

    private var textPaint: Paint = NetworkTheme.current.valueText

    /** Distance from the top of the bounds to the text baseline. */
    private var baseline = 0.0

    init {
        pickable = false
        recomputeBounds()
        refresh()
    }

    /** Re-apply theme color and the visibility preference. */
    fun refresh() {
        textPaint = NetworkTheme.current.valueText
        visible = NetworkPreferences.showShapeCaptions
        invalidatePaint()
    }

    private fun recomputeBounds() {
        if (text.isEmpty()) {
            baseline = 0.0
            setBounds(0.0, 0.0, 0.0, 0.0)
        } else {
            val layout = TextLayout(text, font, FontRenderContext(null, true, true))
            val glyphs = layout.bounds
            baseline = -glyphs.y
            setBounds(0.0, 0.0, layout.advance.toDouble(), glyphs.height)
        }
        invalidatePaint()
    }

    override fun paint(paintContext: PPaintContext) {
        if (text.isEmpty()) return
        val g2 = paintContext.graphics
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.font = font
        g2.paint = textPaint
        g2.drawString(text, 0f, baseline.toFloat())
    }
}
