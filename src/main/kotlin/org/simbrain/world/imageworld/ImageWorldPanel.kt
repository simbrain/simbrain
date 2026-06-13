package org.simbrain.world.imageworld

import kotlinx.coroutines.CoroutineScope
import org.simbrain.util.*
import org.simbrain.workspace.gui.CouplingMenu
import org.simbrain.world.imageworld.ImageWorldPreferences.imageDirectory
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.Action
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ImageWorldPanel(val imageWorldComponent: ImageWorldComponent) : JPanel(), CoroutineScope {

    val imageWorld = imageWorldComponent.world

    override val coroutineContext get() = imageWorld.coroutineContext

    /**
     * Store the last mouse position for drawing continuous lines
     */
    private var lastX: Int = -1
    private var lastY: Int = -1

    /**
     * If true, allow painting
     */
    var paintMode: Boolean = true

    @Transient
    private val clipboard = ImageClipboard(imageWorld)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        imageWorld.imagePipelineCollection.currentPipeline.applyPipeline()
        val processedImage = imageWorld.imagePipelineCollection.currentPipeline.processedImage

        g.drawImage(processedImage, 0, 0, width, height, this)
    }

    val deleteImageAction = createAction(
        "Delete image",
        description = "Delete current image",
        coroutineScope = imageWorld,
        iconPath =  "menu_icons/minus.png"
    ) {
        imageWorld.imageAlbum.deleteCurrentImage()
    }

    val previousImageAction = createAction(
        "Previous image",
        description = "Move the to previous image in the image album ('A')",
        iconPath =  "menu_icons/TangoIcons-GoPrevious.png",
        coroutineScope = imageWorld,
        keyboardShortcut = KeyCombination('A')
    ) {
        imageWorld.previousFrame()
    }
    val nextImageAction = createAction(
        "Next image",
        description = "Move the to next image in the image album ('D')",
        iconPath =  "menu_icons/TangoIcons-GoNext.png",
        keyboardShortcut = KeyCombination('D')
    ) {
        imageWorld.nextFrame()
    }
    val takeSnapshotAction = createAction(
        "Take snapshot",
        description = "Add the current image to the photo album ('S')",
        iconPath =  "menu_icons/camera.png",
        keyboardShortcut = KeyCombination('S')
    ) {
        imageWorld.imageAlbum.takeSnapshot()
    }

    /**
     * Copy image from current system clipboard.
     */
    var copyAction = createAction("Copy") {
        clipboard.copyImage()
    }

    /**
     * Paste image from current system clipboard.
     */
    var pasteAction: Action = createAction("Paste") {
        clipboard.pasteImage()
    }

    val saveImageAction = createAction(
        "Save Current Image...",
        description = "Save the current image to a file",
        iconPath =  "menu_icons/Save.png"
    ) {
        saveImage()
    }

    val saveImageAllAction = createAction("Save all images...") {
        saveAllImages()
    }

    /**
     * Draw a pixel at the current point in the image panel.
     */
    private fun drawPixel(evt: MouseEvent) {
        if (!paintMode || evt.isControlDown || (evt.button == MouseEvent.BUTTON3)) {
            return
        }
        val image = imageWorld.imageAlbum.currentImage
        val ratioX = 1.0 * width / image.width
        val ratioY = 1.0 * height / image.height
        val x = (evt.x / ratioX).toInt()
        val y = (evt.y / ratioY).toInt()

        // Basic boundary check for the center point
        if (x < 0 || x >= image.width || y < 0 || y >= image.height) {
            return
        }

        // Get the color to draw (inverted if shift is down)
        val drawColor = if (evt.isShiftDown) imageWorld.penColor.invert() else imageWorld.penColor

        // Create a temporary Graphics2D to draw on the image
        val g2d = image.createGraphics()

        // Apply smoothing if enabled
        if (imageWorld.useSmoothing) {
            applySmoothing(g2d)
        }

        // For single pixel mode, just set the RGB value directly
        if (imageWorld.penSize == 1) {
            if (lastX >= 0 && lastY >= 0 && (lastX != x || lastY != y)) {
                // Draw a line from last position to current for continuous effect
                g2d.color = drawColor
                g2d.drawLine(lastX, lastY, x, y)
            } else {
                image.setRGB(x, y, drawColor.rgb)
            }
        } else {
            // For multi-pixel mode, draw a circle at the current position
            g2d.color = drawColor

            // If we have a last position and it's different from current, draw connecting line
            if (lastX >= 0 && lastY >= 0 && (abs(lastX - x) > 2 || abs(lastY - y) > 2)) {
                // Draw a line of circles between the points for a smoother stroke
                val dx = x - lastX
                val dy = y - lastY
                val distance = max(abs(dx), abs(dy))

                for (i in 0..distance step max(1, imageWorld.penSize / 4)) {
                    val t = if (distance == 0) 0.0 else i.toDouble() / distance
                    val ix = (lastX + dx * t).toInt()
                    val iy = (lastY + dy * t).toInt()

                    // Check the intermediary point is within bounds
                    if (ix >= 0 && ix < image.width && iy >= 0 && iy < image.height) {
                        drawBrushAt(g2d, ix, iy, imageWorld.penSize, image)
                    }
                }
            } else {
                // Just draw at the current point
                drawBrushAt(g2d, x, y, imageWorld.penSize, image)
            }
        }

        // Store current position as last for next draw
        lastX = x
        lastY = y

        g2d.dispose()
        imageWorld.imageAlbum.events.imageUpdate.fireAsync()
    }

    /**
     * Apply smoothing settings to the graphics context based on quality level
     */
    private fun applySmoothing(g2d: Graphics2D) {
        // Apply different rendering hints based on quality setting
        when (imageWorld.smoothingQuality) {
            ImageWorld.SmoothingQuality.LOW -> {
                g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                )
            }
            ImageWorld.SmoothingQuality.MEDIUM -> {
                g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                )
                g2d.setRenderingHint(
                    RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE
                )
                g2d.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
                )
            }
            ImageWorld.SmoothingQuality.HIGH -> {
                g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                )
                g2d.setRenderingHint(
                    RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE
                )
                g2d.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
                )
                g2d.setRenderingHint(
                    RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
                )
                g2d.setRenderingHint(
                    RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY
                )
                g2d.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
                )
                g2d.setRenderingHint(
                    RenderingHints.KEY_DITHERING,
                    RenderingHints.VALUE_DITHER_ENABLE
                )

                // For high quality, use alpha blending for smoother edges
                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f)

                // Create a radial gradient for a feathered brush effect if we're drawing with larger brushes
                if (imageWorld.penSize > 3) {
                    val center = point(if (lastX != -1) lastX else 0, if (lastY != -1) lastY else 0)
                    val radius = imageWorld.penSize * 0.8f
                    val penColor = imageWorld.penColor
                    val gradient = RadialGradientPaint(
                        center,
                        radius,
                        floatArrayOf(0.0f, 0.7f, 1.0f),
                        arrayOf(penColor,
                            Color(penColor.red, penColor.green, penColor.blue, 180),
                            Color(penColor.red, penColor.green, penColor.blue, 0))
                    )
                    g2d.paint = gradient
                }
            }
        }
    }

    /**
     * Draw at a specific point according to current brush settings
     */
    private fun drawBrushAt(g2d: Graphics2D, x: Int, y: Int, penSize: Int, image: java.awt.image.BufferedImage) {
        val penX = max(0, x - penSize / 2)
        val penY = max(0, y - penSize / 2)
        val actualWidth = min(penSize, image.width - penX)
        val actualHeight = min(penSize, image.height - penY)

        when (imageWorld.brushShape) {
            ImageWorld.BrushShape.CIRCLE -> {
                g2d.fillOval(penX, penY, actualWidth, actualHeight)
            }
            ImageWorld.BrushShape.SQUARE -> {
                g2d.fillRect(penX, penY, actualWidth, actualHeight)
            }
            ImageWorld.BrushShape.SOFT -> {
                // Draw with decreasing opacity for a soft brush effect
                val center = Point(x, y)
                val radius = penSize * 0.9f
                val gradient = RadialGradientPaint(
                    center,
                    radius,
                    floatArrayOf(0.0f, 0.5f, 0.9f),
                    arrayOf(g2d.color,
                        Color(g2d.color.red, g2d.color.green, g2d.color.blue, 128),
                        Color(g2d.color.red, g2d.color.green, g2d.color.blue, 0))
                )

                // Store the old paint and composite
                val oldPaint = g2d.paint
                val oldComposite = g2d.composite

                // Set new settings for soft brush
                g2d.paint = gradient
                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f)

                // Draw with some extra size for soft edges
                val softSize = (penSize * 1.2).toInt()
                val softX = max(0, x - softSize / 2)
                val softY = max(0, y - softSize / 2)
                g2d.fillOval(
                    softX,
                    softY,
                    min(softSize, image.width - softX),
                    min(softSize, image.height - softY)
                )

                // Restore original settings
                g2d.paint = oldPaint
                g2d.composite = oldComposite
            }
        }
    }

    /**
     * Create and display the context menu.
     */
    private fun showContextMenu(evt: MouseEvent) {
        val contextMenu = JPopupMenu()
        contextMenu.add(copyAction)
        contextMenu.add(pasteAction)
        contextMenu.addSeparator()
        contextMenu.add(saveImageAction)
        contextMenu.add(saveImageAllAction)
        contextMenu.addSeparator()
        imageWorld.imagePipelineCollection.currentPipeline.let { pipeline ->
            val pipelineMenu = CouplingMenu(imageWorldComponent, pipeline)
            contextMenu.add(pipelineMenu)
        }
        contextMenu.show(this, evt.x, evt.y)
    }

    /**
     * Save the current image.
     */
    private fun saveImage() {
        val fileChooser = SFileChooser(imageDirectory, "")
        fileChooser.setDescription("Save current image")
        fileChooser.setUseImagePreview(true)
        fileChooser.showSaveDialog("${imageWorldComponent.name}.png")?.let { file ->
            imageWorld.imageAlbum.writeCurrentImageToFile(file)
        }
    }

    private fun saveAllImages() {
        val fileChooser = SFileChooser(imageDirectory, "")
        JOptionPane.showInputDialog("Enter a prefix for the image files")?.let { fileNamePrefix ->
            fileChooser.setDescription("Save images")
            showDirectorySelectionDialog()?.let { File(it) }?.let { dir ->
                imageWorld.imageAlbum.writeAllImagesToFile(dir, fileNamePrefix)
            }

        }

    }

    init {
        // Ability to paint pixels black and white
        val mouseAdapter: MouseAdapter = object : MouseAdapter() {
            override fun mouseDragged(evt: MouseEvent) {
                drawPixel(evt)
            }

            override fun mousePressed(evt: MouseEvent) {
                // Reset last position when starting a new stroke
                lastX = -1
                lastY = -1
                drawPixel(evt)
            }

            override fun mouseReleased(evt: MouseEvent) {
                // Reset last position when ending a stroke
                lastX = -1
                lastY = -1
            }

            override fun mouseClicked(evt: MouseEvent) {
                super.mouseClicked(evt)
                if (evt.isControlDown || (evt.button == MouseEvent.BUTTON3)) {
                    showContextMenu(evt)
                }
            }
        }
        addMouseListener(mouseAdapter)
        addMouseMotionListener(mouseAdapter)
    }
}