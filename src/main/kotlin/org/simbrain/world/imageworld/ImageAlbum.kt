package org.simbrain.world.imageworld

import org.simbrain.util.copy
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JOptionPane
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * ImageAlbum stores a list of static images and lets you load, advance through them etc.
 *
 * @author Tim Shea
 */
class ImageAlbum : ImageSource, AttributeContainer, EditableObject {
    /**
     * A list of buffered images that can be stepped through.
     */
    private val _frames: MutableList<BufferedImage> = ArrayList()

    val frames: List<BufferedImage> = Collections.unmodifiableList(_frames)

    /**
     * Current frame being shown.
     */
    var frameIndex: Int = 0
        private set

    /**
     * Store RGB channel data for consumer updates.
     * channels[0] = red, channels[1] = green, channels[2] = blue
     */
    private var channels: Array<DoubleArray> = Array(3) { DoubleArray(0) }

    /**
     * Construct a new StaticImageSource.
     */
    constructor() : super()

    constructor(filename: String, currentImage: BufferedImage) : super(currentImage)

    /**
     * Load an image from a file and update the current image.
     *
     * @param filename the file to load.
     * @throws IOException upon failure to read the requested file
     */
    suspend fun loadImage(filename: String) {
        _frames.clear()
        if (filename.isEmpty()) {
            setCurrentImage(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB))
        } else {
            val image = ImageIO.read(File(filename))
            setCurrentImage(image)
        }
    }

    /**
     * Load a set of image.
     *
     * @param files the images to load
     */
    suspend fun loadImages(files: Array<File>) {
        val list: MutableList<BufferedImage> = ArrayList()
        for (file in files) {
            try {
                val read = ImageIO.read(file)
                if (read != null) {
                    list.add(read)
                } else {
                    JOptionPane.showMessageDialog(null, String.format("Could not parse %s", file.name))
                    System.err.printf("Could not parse %s", file.name)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        _frames.clear()
        _frames.addAll(list)
        setCurrentImage(_frames[0])
    }

    fun writeCurrentImageToFile(destination: File) {
        ImageIO.write(currentImage, "png", destination)
    }

    fun writeAllImagesToFile(destination: File, fileNamePrefix: String) {
        assert(destination.isDirectory) { "Destination must be a directory" }
        for (i in _frames.indices) {
            ImageIO.write(_frames[i], "png", File(destination, "${fileNamePrefix}$i.png"))
        }
    }

    /**
     * Add a new image to the album and set the current frame to it.
     */
    suspend fun addImage(image: BufferedImage) {
        _frames.add(image)
        frameIndex = _frames.size - 1
        setCurrentImage(image)
        events.imageUpdate.fire().await()
    }

    /**
     * Create image from a provided image icon.
     *
     * @param imageIcon the image icon
     */
    suspend fun loadImage(imageIcon: ImageIcon) {
        val image = BufferedImage(imageIcon.iconWidth, imageIcon.iconHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.drawImage(imageIcon.image, 0, 0, null)
        graphics.dispose()
        setCurrentImage(image)
        events.imageUpdate.fire()
    }

    /**
     * Update the current image to the next image in the frame list.
     */
    suspend fun nextFrame() {
        saveCurrentFrame()
        frameIndex = (frameIndex + 1) % _frames.size
        setCurrentImage(_frames[frameIndex])
    }

    /**
     * Update the current image to the previous image in the frame list.
     */
    suspend fun previousFrame() {
        saveCurrentFrame()
        frameIndex = (frameIndex + _frames.size - 1) % _frames.size
        setCurrentImage(_frames[frameIndex])
    }

    /**
     * Returns number of frames in the album
     */
    val numFrames: Int
        get() = _frames.size

    /**
     * Set album to frame aat provided index.
     */
    suspend fun setFrame(frameIndex: Int) {
        if (frameIndex >= 0 && frameIndex < _frames.size) {
            saveCurrentFrame()
            this.frameIndex = frameIndex
            setCurrentImage(_frames[frameIndex])
        }
    }

    suspend fun reset(width: Int, height: Int) {
        _frames.clear()
        frameIndex = 0
        val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        _frames.add(newImage)
        channels = Array(3) { DoubleArray(width * height) }
        setCurrentImage(newImage, true)
    }

    /**
     * Add the current image world image to the album.
     */
    suspend fun takeSnapshot() {
        val snapshot = currentImage.copy()
        addImage(snapshot)
    }

    fun saveCurrentFrame() {
        val snapshot = currentImage.copy()
        _frames[frameIndex].data = snapshot.data
    }

    suspend fun deleteCurrentImage() {
        if (_frames.isEmpty()) {
            return
        }
        if (_frames.size == 1) {
            reset(currentImage.width, currentImage.height)
            return
        }
        _frames.removeAt(frameIndex)
        frameIndex = (frameIndex + _frames.size - 1) % _frames.size
        setCurrentImage(_frames[frameIndex])
    }

    /**
     * Ensure frames[0] exists and has the correct dimensions to fit the incoming data.
     * Creates a square image with side length = ceil(sqrt(values.size)).
     * Returns false if values is empty (nothing to resize for).
     */
    private fun resizeToFit(values: DoubleArray): Boolean {
        if (values.isEmpty()) {
            return false
        }

        val length = ceil(sqrt(values.size.toDouble())).toInt()

        val needsResize = frames.isEmpty() ||
                _frames[0].width != length ||
                _frames[0].height != length

        if (needsResize) {
            val newImage = BufferedImage(length, length, BufferedImage.TYPE_INT_RGB)
            if (frames.isEmpty()) {
                _frames.add(newImage)
            } else {
                _frames[0] = newImage
            }
            currentImage = newImage
            channels = Array(3) { DoubleArray(length * length) }
            events.resize.fire()
        }
        return true
    }

    /**
     * Update frames[0] pixel data from the channels arrays.
     */
    private fun updateImageFromChannels() {
        val image = _frames[0]
        val rgbArray = IntArray(image.width * image.height)

        for (i in rgbArray.indices) {
            val r = (channels[0][i] * 255.0).toInt().coerceIn(0, 255)
            val g = (channels[1][i] * 255.0).toInt().coerceIn(0, 255)
            val b = (channels[2][i] * 255.0).toInt().coerceIn(0, 255)
            rgbArray[i] = (r shl 16) or (g shl 8) or b
        }

        image.setRGB(0, 0, image.width, image.height, rgbArray, 0, image.width)
    }

    @Consumable
    fun setBrightness(values: DoubleArray) {
        if (!resizeToFit(values)) return
        // Copy to all three channels for grayscale
        System.arraycopy(values, 0, channels[0], 0, values.size)
        System.arraycopy(values, 0, channels[1], 0, values.size)
        System.arraycopy(values, 0, channels[2], 0, values.size)
        updateImageFromChannels()
        events.imageUpdate.fire()
    }

    @Consumable
    fun setRed(values: DoubleArray) {
        if (!resizeToFit(values)) return
        System.arraycopy(values, 0, channels[0], 0, values.size)
        updateImageFromChannels()
        events.imageUpdate.fire()
    }

    @Consumable
    fun setGreen(values: DoubleArray) {
        if (!resizeToFit(values)) return
        System.arraycopy(values, 0, channels[1], 0, values.size)
        updateImageFromChannels()
        events.imageUpdate.fire()
    }

    @Consumable
    fun setBlue(values: DoubleArray) {
        if (!resizeToFit(values)) return
        System.arraycopy(values, 0, channels[2], 0, values.size)
        updateImageFromChannels()
        events.imageUpdate.fire()
    }

    /**
     * Accept interleaved RGB activations in HWC order: [r₀₀, g₀₀, b₀₀, r₀₁, g₀₁, b₀₁, ...].
     * The image must already be the correct size (e.g. set via [reset]).
     */
    @Consumable
    fun setRgbActivations(values: DoubleArray) {
        val image = if (frames.isEmpty()) return else _frames[0]
        val pixelCount = image.width * image.height
        val len = minOf(values.size / 3, pixelCount)
        for (i in 0 until len) {
            channels[0][i] = values[i * 3]
            channels[1][i] = values[i * 3 + 1]
            channels[2][i] = values[i * 3 + 2]
        }
        updateImageFromChannels()
        events.imageUpdate.fire()
    }

    override val id: String
        get() = "Image album"
}
