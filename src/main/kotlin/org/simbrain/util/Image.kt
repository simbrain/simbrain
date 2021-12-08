package org.simbrain.util

import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.*

fun FloatArray.toSimbrainColor() = map { value ->
    value.clip(-1.0f..1.0f).let {
        if (it < 0) Color.HSBtoRGB(2/3f, -it, 1.0f) else Color.HSBtoRGB(0.0f, it, 1.0f)
    }
}.toIntArray()

fun DoubleArray.toSimbrainColor() = map { value ->
    value.clip(-1.0..1.0).let {
        if (it < 0) Color.HSBtoRGB(2/3f, (-it).toFloat(), 1.0F) else Color.HSBtoRGB(0.0f, it.toFloat(), 1.0f)
    }
}.toIntArray()

fun DoubleArray.toSimbrainColorImage(width: Int, height: Int) = toSimbrainColor().let {
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(it, it.size), null)
    BufferedImage(colorModel, raster, false, null)
}

fun FloatArray.toSimbrainColorImage(width: Int, height: Int) = toSimbrainColor().let {
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(it, it.size), null)
    BufferedImage(colorModel, raster, false, null)
}

fun Array<FloatArray>.toSimbrainColorImage() = flattenArray(this).toSimbrainColorImage(first().size, size)

fun IntArray.toRGBImage(width: Int, height: Int): BufferedImage {
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(this, size), null)
    return BufferedImage(colorModel, raster, false, null)
}

fun FloatArray.toGrayScaleImage(width: Int, height: Int) = this
        .map { (it.clip(0.0f, 1.0f) * 255).toInt().toByte() }
        .toByteArray()
        .let {
            val colorModel = DirectColorModel(8, 0xff, 0xff, 0xff)
            val sampleModel = colorModel.createCompatibleSampleModel(width, height)
            val raster = Raster.createWritableRaster(sampleModel, DataBufferByte(it, it.size), null)
            BufferedImage(colorModel, raster, false, null)
        }

private val cache = HashMap<Pair<Int, Int>, BufferedImage>()

fun transparentImage(width: Int, height: Int) = cache[width to height] ?: run {
    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            .also { cache[width to height] = it }
}

fun BufferedImage.scale(factor: Double) = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).let {
    AffineTransformOp(AffineTransform().apply { scale(factor, factor) }, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
            .filter(this, it)!!
}

fun BufferedImage.scale(w: Int, h: Int) = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).let {
    AffineTransformOp(AffineTransform().also { it.scale(w.toDouble() / width, h.toDouble() / height) },
            AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(this, it)!!
}