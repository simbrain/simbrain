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
    @Consumable
    fun loadImage(filename: String) {
        _frames.clear()
        if (filename.isEmpty()) {
            currentImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        } else {
            val image = ImageIO.read(File(filename))
            currentImage = image
        }
    }

    /**
     * Load a set of image.
     *
     * @param files the images to load
     */
    fun loadImages(files: Array<File>) {
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
        currentImage = _frames[0]
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
    fun addImage(image: BufferedImage) {
        _frames.add(image)
        frameIndex = _frames.size - 1
        currentImage = image
    }

    /**
     * Create image from a provided image icon.
     *
     * @param imageIcon the image icon
     */
    fun loadImage(imageIcon: ImageIcon) {
        val image = BufferedImage(imageIcon.iconWidth, imageIcon.iconHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.drawImage(imageIcon.image, 0, 0, null)
        graphics.dispose()
        currentImage = image
        events.imageUpdate.fire()
    }

    /**
     * Update the current image to the next image in the frame list.
     */
    fun nextFrame() {
        saveCurrentFrame()
        frameIndex = (frameIndex + 1) % _frames.size
        currentImage = _frames[frameIndex]
    }

    /**
     * Update the current image to the previous image in the frame list.
     */
    fun previousFrame() {
        saveCurrentFrame()
        frameIndex = (frameIndex + _frames.size - 1) % _frames.size
        currentImage = _frames[frameIndex]
    }

    /**
     * Returns number of frames in the album
     */
    val numFrames: Int
        get() = _frames.size

    /**
     * Set album to frame aat provided index.
     */
    fun setFrame(frameIndex: Int) {
        if (frameIndex >= 0 && frameIndex < _frames.size) {
            saveCurrentFrame()
            currentImage = _frames[frameIndex]
        }
    }

    fun reset(width: Int, height: Int) {
        _frames.clear()
        frameIndex = 0
        setCurrentImage(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), true)
    }

    /**
     * Add the current image world image to the album.
     */
    fun takeSnapshot() {
        val snapshot = currentImage.copy()
        addImage(snapshot)
    }

    fun saveCurrentFrame() {
        val snapshot = currentImage.copy()
        _frames[frameIndex].data = snapshot.data
    }

    fun deleteCurrentImage() {
        if (_frames.size == 0) {
            return
        }
        if (_frames.size == 1) {
            reset(currentImage.width, currentImage.height)
            return
        }
        _frames.removeAt(frameIndex)
        frameIndex = (frameIndex + _frames.size - 1) % _frames.size
        currentImage = _frames[frameIndex]
    }

    override val id: String
        get() = "Image album"
}
