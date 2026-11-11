/**
 * A bordered image drawn on a connector's arrow, such as a weight matrix or a convolution kernel. Its size can
 * change after creation, which connectors use to fit the image to the space between their layers.
 */
package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.piccolo.SimbrainImage
import java.awt.BasicStroke
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage

class ImageBox(width: Double, height: Double, thickness: Float) : PNode() {

    var image: BufferedImage? = null
        set(image) {
            field = image
            pImage.image = image!!
            pImage.setBounds(0.0, 0.0, bounds.width, bounds.height)
        }

    val box = PPath.createRectangle(0.0, 0.0, width, height)!!
        .apply {
            stroke = BasicStroke(thickness).also { strokePaint = NetworkPreferences.weightMatrixBoundaryColor }
        }
        .also { addChild(it) }

    private val pImage = SimbrainImage().also { addChild(it) }

    init {
        resize(width, height)
    }

    /** Draw the image and its border at [width] by [height]. The image is stretched to fill the new size. */
    fun resize(width: Double, height: Double) {
        setBounds(0.0, 0.0, width, height)
        box.reset()
        box.append(Rectangle2D.Double(0.0, 0.0, width, height), false)
        pImage.setBounds(0.0, 0.0, width, height)
    }

    /** Re-read the boundary color from preferences (e.g. after a light/dark theme switch). */
    fun updateBorderColorFromPreferences() {
        box.strokePaint = NetworkPreferences.weightMatrixBoundaryColor
    }

}
