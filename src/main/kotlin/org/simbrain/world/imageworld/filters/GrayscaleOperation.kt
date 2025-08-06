package org.simbrain.world.imageworld.filters

import org.simbrain.util.propertyeditor.CopyableObject
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp

/**
 * An image operation that converts images to grayscale.
 */
class GrayscaleOperation : ImageOperation() {

    @Transient
    private val colorConvertOp = ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null)

    init {
        name = "Grayscale"
    }

    override fun applyOperation(input: BufferedImage): BufferedImage {
        return colorConvertOp.filter(input, null)
    }

    override fun copy(): ImageOperation {
        val copy = GrayscaleOperation()
        copy.enabled = enabled
        copy.name = name
        return copy
    }

    override fun getTypeList(): List<Class<out CopyableObject>>? {
        return listOf(
            ResizeOperation::class.java,
            GrayscaleOperation::class.java,
            ThresholdOperation::class.java,
            EdgeDetectionFilter::class.java,
            GaborFilter::class.java
        )
    }
} 