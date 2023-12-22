package org.simbrain.util

import org.simbrain.util.math.SimbrainMath
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.*
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.math.floor
import kotlin.math.min

fun Double.toSimbrainColor(range: ClosedFloatingPointRange<Double>, coolHue: Float = 2/3f, hotHue: Float = 0.0f) = clip(range).let {
    if (it < 0) {
        Color.HSBtoRGB(coolHue, SimbrainMath.rescale(-it, 0.0, -range.start, 0.0, 1.0).toFloat(), 1.0f)
    } else {
        Color.HSBtoRGB(hotHue, SimbrainMath.rescale(it, 0.0, range.endInclusive, 0.0, 1.0).toFloat(), 1.0f)
    }
}

fun Float.toSimbrainColor() = clip(-1.0f..1.0f).let {
    if (it < 0) Color.HSBtoRGB(2/3f, -it, 1.0f) else Color.HSBtoRGB(0.0f, it, 1.0f)
}

fun Double.toSimbrainColor() = toFloat().toSimbrainColor()

/**
 * Convert array of float values to array of RGB color values.
 */
fun FloatArray.toSimbrainColor() = map { it.toSimbrainColor() }.toIntArray()

/**
 * Convert array of double values to array of RGB color values.
 */
fun DoubleArray.toSimbrainColor() = map { it.toSimbrainColor() }.toIntArray()

/**
 * Converts a double array to matrix representation (as a Buffered Image) with a specified width and height, in pixels.
 *
 * Width * height must be less than the array size.
 */
fun DoubleArray.toSimbrainColorImage(width: Int, height: Int) = IntArray(width * height).let {
    System.arraycopy(toSimbrainColor(), 0, it, 0, min(size, width * height))
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(it, width * height), null)
    BufferedImage(colorModel, raster, false, null)
}

/**
 * Converts a float array to a matrix image as in [DoubleArray.toSimbrainColorImage]
 */
fun FloatArray.toSimbrainColorImage(width: Int, height: Int) = IntArray(width * height).let {
    System.arraycopy(toSimbrainColor(), 0, it, 0, min(size, width * height))
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(it, width * height), null)
    BufferedImage(colorModel, raster, false, null)
}

/**
 * Converts a 2d float array to a "square" buffered image.
 */
fun Array<FloatArray>.toSimbrainColorImage() = flattenArray(this).toSimbrainColorImage(first().size, size)

/**
 * Converts a 2d int array to  a "square" buffered image, using 24-bit RGB.
 */
fun IntArray.toRGBImage(width: Int, height: Int): BufferedImage {
    val colorModel: ColorModel = DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(this, size), null)
    return BufferedImage(colorModel, raster, false, null)
}

/**
 * Converts a float array to a gray scale image of a specified height and width in pixels.
 *
 * TODO: Does not work with scale command
 */
fun FloatArray.toGrayScaleImage(width: Int, height: Int): BufferedImage {
    if (this.size != width * height) {
        throw IllegalArgumentException("Size of FloatArray ($size) does not match the dimensions ($width x $height) provided.")
    }

    val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    val raster = image.raster

    this.forEachIndexed { index, value ->
        val intValue = (value * 255).coerceIn(0f, 255f).toInt()
        val x = index % width
        val y = index / width
        raster.setSample(x, y, 0, intValue)
    }

    return image
}

private val cache = HashMap<Pair<Int, Int>, BufferedImage>()

/**
 * Creates a transparent image with specified pixels.
 */
fun transparentImage(width: Int, height: Int) = cache[width to height] ?: run {
    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            .also { cache[width to height] = it }
}

/**
 * Convert an array of booleans to a matrix of pixels, transparent for false, and "color" for true.
 */
fun BooleanArray.toOverlay(width: Int, height: Int, color: Color ): BufferedImage {
    val colorModel: ColorModel = DirectColorModel(
        32, 0xff0000, 0x00ff00, 0x0000ff, 0xff shl 24
    )
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val dataBuffer =  this.map { if (it) color.rgb else 0}.toIntArray()
    val raster = Raster.createWritableRaster(sampleModel, DataBufferInt(dataBuffer, size), null)
    return BufferedImage(colorModel, raster, false, null)
}

/**
 * Scale the size of the image by the provided factor.
 *
 * TODO: Use parent's color model
 */
fun BufferedImage.scale(factor: Double) = BufferedImage(
    floor(width*factor).toInt(),
    floor(height*factor).toInt(),
    BufferedImage.TYPE_INT_ARGB)
    .let {
    AffineTransformOp(AffineTransform().apply { scale(factor, factor) }, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
            .filter(this, it)!!
}

/**
 * Rescale the image to a specified height and width in pixels.
 *
 * TODO: Use parent's color model
 */
fun BufferedImage.scale(w: Int, h: Int) = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).let {
    AffineTransformOp(AffineTransform().also { it.scale(w.toDouble() / width, h.toDouble() / height) },
            AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(this, it)!!
}

fun Int.toColor(): Color {
    return Color(this)
}

/**
 * Display an image in a panel.
 */
fun BufferedImage.display() {
    JPanel().apply {
        add(JLabel(ImageIcon(this@display)))
    }.displayInDialog()
}


fun Color.toHSB() = FloatArray(3) { 0.0f }.let { Color.RGBtoHSB(red, green, blue, it) }

fun HSBInterpolate(fromColor: FloatArray, toColor: FloatArray, steps: Int): List<Color> {
    val difference = toColor - fromColor
    // hue is a flattened circle of length 1
    if (difference[0] > 0.5) {
        difference[0] = difference[0] - 1
    } else if (difference[0] < -0.5) {
        difference[0] = difference[0] + 1
    }
    val (h, s, b) = fromColor
    val (dh, ds, db) = difference / steps.toFloat()
    return (0 until steps).map {
        Color.getHSBColor((1 + h + it * dh) % 1.0f, s + it * ds, b + it * db)
    }
}

/**
 * Returns a color at value "t" (between 0 and 1) between fromColor and tColor
 */
fun HSBInterpolate(fromColor: FloatArray, toColor: FloatArray, t: Double): Color {
    val tclipped = t.clip(0.0, 1.0).toFloat()
    val difference = toColor - fromColor
    // hue is a flattened circle of length 1
    if (difference[0] > 0.5) {
        difference[0] = difference[0] - 1
    } else if (difference[0] < -0.5) {
        difference[0] = difference[0] + 1
    }
    val (h, s, b) = fromColor
    val (dh, ds, db) = difference * tclipped
    return Color.getHSBColor((1 + h + dh) % 1.0f, s + ds, b + db)
}

fun BufferedImage.copy(): BufferedImage {
    try {
        val result = BufferedImage(width, height, type)
        copyData(result.raster)
        return result
    } catch (e: Exception) {
        System.err.println("Failed to copy an image of size $width x $height and type $type")
        return FloatArray(width * height).toSimbrainColorImage(width, height)
    }
}

fun Color.invert() = Color(255 - red, 255 - green, 255 - blue)

/**
 * Used to get decent upper bounds from a given value, e.g. 820 -> 900
 */
fun graphicalUpperBound(value: Double): Double {
    return when {
        value < 100 -> floor(value / 10) * 10 + 10
        value < 1000 -> floor(value / 100) * 100 + 100
        value < 10000 -> floor(value / 1000) * 1000 + 1000
        else -> floor(value / 10000) * 10000 + 10000
    }
}


fun main() {
    println(graphicalUpperBound(820.0))
    println(graphicalUpperBound(1220.0))
    println(graphicalUpperBound(18020.0))
    // val arr = UniformRealDistribution(0.0,1.0).sampleDouble(100)
    // val intArray =  UniformIntegerDistribution().apply {
    //     floor = 0
    //     ceil = 2.0.pow(24.0).toInt()
    // }.sampleInt(100)
    // val boolArray = intArray.map { it % 2 == 1 }.toBooleanArray()
    // arr.toSimbrainColorImage(10,10).scale(100, 100).display()
    // arr.toSimbrainColorImage(10,10).scale(20.0).display()
    // intArray.toRGBImage(10,10).scale(10.0).display()
    // arr.toFloatArray().toGrayScaleImage(10,10).display()
    // boolArray.toOverlay(10,10, Color.yellow).scale(10.0).display()
}