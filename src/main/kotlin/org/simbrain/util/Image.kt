package org.simbrain.util

import java.awt.Color
import java.awt.image.*

fun FloatArray.toRGB() = map { value ->
    value.clip(-1.0f..1.0f).let {
        if (it < 0) Color.HSBtoRGB(2/3f, -it, 1.0f) else Color.HSBtoRGB(0.0f, it, 1.0f)
    }
}.toIntArray()

fun FloatArray.toImage(width: Int, height: Int) = toRGB().let {
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(it, it.size), null)
    BufferedImage(colorModel, raster, false, null)
}