package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.piccolo.SimbrainImage
import java.awt.BasicStroke
import java.awt.image.BufferedImage

class ImageBox(val width: Int, val height: Int, thickness: Float) : PNode() {

    init {
        setBounds(0.0, 0.0, width.toDouble(), height.toDouble())
    }

    var image: BufferedImage? = null
        set(image) {
            field = image
            pImage.image = image!!
            pImage.setBounds(0.0, 0.0, width.toDouble(), height.toDouble())
        }



    val box = PPath.createRectangle(0.0, 0.0, width.toDouble(), height.toDouble())!!
        .apply {
            stroke = BasicStroke(thickness).also { strokePaint = NetworkPreferences.weightMatrixBoundaryColor }
        }
        .also { addChild(it) }

    private val pImage = SimbrainImage().also {
        it.setBounds(0.0, 0.0, width.toDouble(), height.toDouble())
        addChild(it)
    }

    /** Re-read the boundary color from preferences (e.g. after a light/dark theme switch). */
    fun updateBorderColorFromPreferences() {
        box.strokePaint = NetworkPreferences.weightMatrixBoundaryColor
    }

}