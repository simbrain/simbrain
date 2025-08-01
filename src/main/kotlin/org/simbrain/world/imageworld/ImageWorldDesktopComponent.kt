package org.simbrain.world.imageworld

import org.simbrain.util.*
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.gui.CouplingMenu
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import org.simbrain.world.imageworld.ImageWorldPreferences.imageDirectory
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import org.simbrain.world.imageworld.gui.ImagePipelineCollectionGui
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ImageWorldDesktopComponent(frame: GenericFrame, component: ImageWorldComponent) :
    DesktopComponent<ImageWorldComponent>(frame, component) {

    private val imageToolbar = JToolBar()

    /**
     * Custom file chooser for selecting image files.
     */
    private var fileChooser = SFileChooser(imageDirectory, "")

    /**
     * Main model object.
     */
    private val imageWorld: ImageWorld = component.world

    /**
     * If true, allow painting
     */
    var paintMode: Boolean = true

    @Transient
    private val clipboard = ImageClipboard(imageWorld)

    /**
     * Current pen color when drawing on the current image.
     */
    private var penColor: Color = Color.white

    /**
     * Current pen size for drawing points. Values greater than 1 will draw wider points.
     */
    private var penSize: Int = 1
    
    /**
     * Whether to use anti-aliasing (smoothing) when drawing.
     */
    private var useSmoothing: Boolean = false
    
    /**
     * The quality level of smoothing (LOW, MEDIUM, HIGH)
     */
    private enum class SmoothingQuality {
        LOW, MEDIUM, HIGH;

        override fun toString(): String {
            return name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
    
    /**
     * The available brush shapes
     */
    private enum class BrushShape {
        CIRCLE, SQUARE, SOFT;

        override fun toString(): String {
            return name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
    
    private var smoothingQuality: SmoothingQuality = SmoothingQuality.HIGH
    private var brushShape: BrushShape = BrushShape.CIRCLE
    
    /**
     * Store the last mouse position for drawing continuous lines
     */
    private var lastX: Int = -1
    private var lastY: Int = -1

    private val frameLabel = JLabel()

    private val deleteImageAction = createAction(
        "Delete image",
        description = "Delete current image",
        iconPath =  "menu_icons/minus.png"
    ) {
        imageWorld.imageAlbum.deleteCurrentImage()
    }

    private val previousImageAction = createAction(
        "Previous image",
        description = "Move the to previous image in the image album ('A')",
        iconPath =  "menu_icons/TangoIcons-GoPrevious.png",
        keyboardShortcut = KeyCombination('A')
    ) {
        imageWorld.previousFrame()
    }
    private val nextImageAction = createAction(
        "Next image",
        description = "Move the to next image in the image album ('D')",
        iconPath =  "menu_icons/TangoIcons-GoNext.png",
        keyboardShortcut = KeyCombination('D')
    ) {
        imageWorld.nextFrame()
    }
    private val takeSnapshotAction = createAction(
        "Take snapshot",
        description = "Add the current image to the photo album ('S')",
        iconPath =  "menu_icons/camera.png",
        keyboardShortcut = KeyCombination('S')
    ) {
        imageWorld.imageAlbum.takeSnapshot()
    }

    /**
     * Central panel to render the image.
     */
    private inner class ImagePanel : JPanel() {
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

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            imageWorld.imagePipelineCollection.currentPipeline.applyPipeline()
            val processedImage = imageWorld.imagePipelineCollection.currentPipeline.processedImage

            g.drawImage(processedImage, 0, 0, width, height, this)
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
            val drawColor = if (evt.isShiftDown) penColor.invert() else penColor
            
            // Create a temporary Graphics2D to draw on the image
            val g2d = image.createGraphics()
            
            // Apply smoothing if enabled
            if (useSmoothing) {
                applySmoothing(g2d)
            }
            
            // For single pixel mode, just set the RGB value directly
            if (penSize == 1) {
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
                    
                    for (i in 0..distance step max(1, penSize / 4)) {
                        val t = if (distance == 0) 0.0 else i.toDouble() / distance
                        val ix = (lastX + dx * t).toInt()
                        val iy = (lastY + dy * t).toInt()
                        
                        // Check the intermediary point is within bounds
                        if (ix >= 0 && ix < image.width && iy >= 0 && iy < image.height) {
                            drawBrushAt(g2d, ix, iy, penSize, image)
                        }
                    }
                } else {
                    // Just draw at the current point
                    drawBrushAt(g2d, x, y, penSize, image)
                }
            }
            
            // Store current position as last for next draw
            lastX = x
            lastY = y
            
            g2d.dispose()
            imageWorld.imageAlbum.fireImageUpdate()
        }
        
        /**
         * Apply smoothing settings to the graphics context based on quality level
         */
        private fun applySmoothing(g2d: Graphics2D) {
            // Apply different rendering hints based on quality setting
            when (smoothingQuality) {
                SmoothingQuality.LOW -> {
                    g2d.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                    )
                }
                SmoothingQuality.MEDIUM -> {
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
                SmoothingQuality.HIGH -> {
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
                    if (penSize > 3) {
                        val center = point(if (lastX != -1) lastX else 0, if (lastY != -1) lastY else 0)
                        val radius = penSize * 0.8f
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
            
            when (brushShape) {
                BrushShape.CIRCLE -> {
                    g2d.fillOval(penX, penY, actualWidth, actualHeight)
                }
                BrushShape.SQUARE -> {
                    g2d.fillRect(penX, penY, actualWidth, actualHeight)
                }
                BrushShape.SOFT -> {
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
    }

    private fun setupMenuBar(frame: GenericFrame) {
        val menuBar = JMenuBar()
        val fileMenu = JMenu("File  ")
        menuBar.add(fileMenu)

        // Add load images menu item if it's an image album world
        val loadImages = JMenuItem("Load Images...")
        loadImages.addActionListener { loadImages() }
        fileMenu.add(loadImages)

        fileMenu.add(saveImageAction)

        val saveAllImages = JMenuItem(saveImageAllAction)
        fileMenu.add(saveAllImages)

        fileMenu.addSeparator()
        fileMenu.add(copyAction)
        fileMenu.add(pasteAction)

        fileMenu.addSeparator()
        fileMenu.add(actionManager.createImportAction(this))
        fileMenu.add(actionManager.createExportAction(this))
        fileMenu.addSeparator()
        fileMenu.add(actionManager.createRenameAction(this))
        fileMenu.addSeparator()
        fileMenu.add(actionManager.createCloseAction(this))

        // Edit Menu
        val editMenu = JMenu("Edit")

        val resetCanvasAction = org.simbrain.util.createAction(
            "Reset canvas...",
            description = "Remove all images and replace with an empty canvas",
            iconPath =  "menu_icons/Reset.png"
        ) {
            val wInp = JTextField(5)
            val hInp = JTextField(5)
            wInp.text = imageWorld.currentImage.width.toString()
            hInp.text = imageWorld.currentImage.width.toString()
            val myPanel = JPanel()
            myPanel.add(JLabel("Width:"))
            myPanel.add(wInp)
            myPanel.add(Box.createHorizontalStrut(15)) // a spacer
            myPanel.add(JLabel("Height:"))
            myPanel.add(hInp)
            val result = JOptionPane.showConfirmDialog(
                null,
                myPanel,
                "Create new canvas, enter dimensions.",
                JOptionPane.OK_CANCEL_OPTION
            )
            if (result == JOptionPane.OK_OPTION) {
                imageWorld.resetImageAlbum(wInp.text.toInt(), hInp.text.toInt())
            }
        }
        menuBar.add(editMenu)
        fun createEditMenu() {
            editMenu.removeAll()
            editMenu.add(resetCanvasAction)
            editMenu.addSeparator()
            imageWorld.imagePipelineCollection.currentPipeline?.let { pipeline ->
                editMenu.add(CouplingMenu(workspaceComponent, pipeline))
            }
        }
        swingInvokeLater {
            createEditMenu()
            onCouplingAttributesChanged { createEditMenu() }
        }

        // Help Menu
        val helpMenu = JMenu("Help")
        val helpItem = JMenuItem("World Help")
        menuBar.add(helpMenu)
        val helpAction = ShowHelpAction("https://docs.simbrain.net/docs/worlds/imageworld.html")
        helpItem.action = helpAction
        helpMenu.add(helpItem)

        frame.jMenuBar = menuBar
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
        imageWorld.imagePipelineCollection.currentPipeline?.let { pipeline ->
            val pipelineMenu = CouplingMenu(workspaceComponent, pipeline)
            contextMenu.add(pipelineMenu)
        }
        contextMenu.show(this, evt.x, evt.y)
    }

    /**
     * Set up toolbars depending on what type of world is being displayed
     */
    private fun setupToolbars() {

        imageToolbar.add(frameLabel)
        imageToolbar.add(deleteImageAction)
        imageToolbar.add(previousImageAction)
        imageToolbar.add(nextImageAction)
        imageToolbar.add(takeSnapshotAction)
        imageToolbar.addSeparator()


        val fillCanvasAction = org.simbrain.util.createAction(
            "Fill",
            description = "Fill canvas using current color",
            iconPath =  "menu_icons/fill.png"
        ) {
            val confirm = showWarningConfirmDialog("Are you sure you want to fill the canvas?")
            if (confirm == JOptionPane.YES_OPTION) {
                imageWorld.imageAlbum.currentImage.fill(penColor)
                imageWorld.imageAlbum.fireImageUpdate()
            }
        }
        val clearCanvasAction = org.simbrain.util.createAction(
            "Clear",
            description = "Clear canvas (with black pixels)",
            iconPath =  "menu_icons/Eraser.png"
        ) {
            val confirm = showWarningConfirmDialog("Are you sure you want to clear the canvas?")
            if (confirm == JOptionPane.YES_OPTION) {
                imageWorld.imageAlbum.currentImage.fill(Color.black)
                imageWorld.imageAlbum.fireImageUpdate()
            }
        }

        imageToolbar.add(fillCanvasAction)
        imageToolbar.add(clearCanvasAction)
        imageToolbar.addSeparator()

        // Color options
        val colorList = arrayOf(
            Color.white,
            Color.black,
            Color.red,
            Color.blue,
            Color.green,
            Color.yellow,
            Color.cyan,
            Color.magenta
        )
        val colorNames =
            arrayOf<String?>("White", "Black", "Red", "Blue", "Green", "Yellow", "Cyan", "Magenta", "Custom")
        val cbColorChoice: JComboBox<*> = JComboBox<Any?>(colorNames)

        cbColorChoice.addActionListener { e: ActionEvent ->
            val len = cbColorChoice.itemCount
            if ((e.source as JComboBox<*>).selectedIndex == len - 1) {
                println("Custom...")
            } else {
                this.penColor = colorList[cbColorChoice.selectedIndex]
            }
        }
        imageToolbar.add(cbColorChoice)

        // Pen size slider for more precise control
        val penSizeLabel = JLabel("${penSize}px")
        val penSizeSlider = JSlider(JSlider.HORIZONTAL, 1, 30, penSize)
        penSizeSlider.preferredSize = Dimension(80, penSizeSlider.preferredSize.height)
        penSizeSlider.addChangeListener { 
            penSize = penSizeSlider.value
            penSizeSlider.toolTipText = "Pen size: ${penSize}px"
            penSizeLabel.text = "${penSize}px"
        }
        imageToolbar.add(penSizeLabel)
        imageToolbar.add(penSizeSlider)

        // Smoothing checkbox and quality selection
        val checkBoxSmoothing = JCheckBox("Smoothing")
        checkBoxSmoothing.isSelected = useSmoothing
        checkBoxSmoothing.addItemListener {
            useSmoothing = checkBoxSmoothing.isSelected
        }
        
        // Smoothing quality selection
        val cbSmoothingQuality = JComboBox(SmoothingQuality.values())
        cbSmoothingQuality.selectedItem = smoothingQuality
        cbSmoothingQuality.isEnabled = useSmoothing
        cbSmoothingQuality.toolTipText = "Select smoothing quality level"
        cbSmoothingQuality.addActionListener {
            smoothingQuality = cbSmoothingQuality.selectedItem as SmoothingQuality
        }
        
        // Brush shape selection
        val cbBrushShape = JComboBox(BrushShape.values())
        cbBrushShape.selectedItem = brushShape
        cbBrushShape.toolTipText = "Select brush shape"
        cbBrushShape.addActionListener {
            brushShape = cbBrushShape.selectedItem as BrushShape
        }
        val brushShapePanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
        brushShapePanel.add(JLabel("Brush:"))
        brushShapePanel.add(cbBrushShape)
        imageToolbar.add(brushShapePanel)

        //smoothingPanel.add(cbSmoothingQuality)
        imageToolbar.add(checkBoxSmoothing)

    }

    /**
     * Copy image from current system clipboard.
     */
    var copyAction: Action = object : AbstractAction("Copy") {
        override fun actionPerformed(e: ActionEvent) {
            clipboard.copyImage()
        }
    }

    /**
     * Paste image from current system clipboard.
     */
    var pasteAction: Action = object : AbstractAction("Paste") {
        override fun actionPerformed(e: ActionEvent) {
            clipboard.pasteImage()
        }
    }

    val saveImageAction = createAction(
        "Save Current Image...",
        description = "Save the current image to a file",
        iconPath =  "menu_icons/Save.png"
    ) {
        saveImage()
    }

    val saveImageAllAction = createAction("Save All Images...") {
        saveAllImages()
    }

    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The ImageWorldComponent to interact with.
     */
    init {
        setupMenuBar(frame)
        layout = BorderLayout()

        // Main image
        add(ImagePanel(), BorderLayout.CENTER)
        imageWorld.imageAlbum.events.imageUpdate.on(swingDispatcher) {
            updateToolbar()
            repaint()
        }
        imageWorld.imagePipelineCollection.events.pipelineChanged.on(swingDispatcher) { _: ImageProcessingPipeline, _: ImageProcessingPipeline -> this.repaint() }
        imageWorld.imagePipelineCollection.events.pipelineSelectionChanged.on(swingDispatcher) { _: ImageProcessingPipeline -> this.repaint() }

        // Toolbars
        val transformationGui = ImagePipelineCollectionGui(this, imageWorld.imagePipelineCollection)
        add(transformationGui.toolbar, BorderLayout.NORTH)
        add(imageToolbar, BorderLayout.SOUTH)
        setupToolbars()
        updateToolbar()

        // TODO: Below breaks the file chooser
        //fileChooser.setUseImagePreview(true);
        // String[] exts = ImageIO.getReaderFileSuffixes();
        // String[] descriptions = ImageIO.getReaderFormatNames();
        // for (int i = 0; i < exts.length; ++i) {
        //    fileChooser.addExtension(descriptions[i], "." + exts[i]);
        // }
    }

    /**
     * Save the current image.
     */
    private fun saveImage() {
        fileChooser.setDescription("Save current image")
        fileChooser.setUseImagePreview(true)
        fileChooser.showSaveDialog("${workspaceComponent.name}.png")?.let { file ->
            imageWorld.imageAlbum.writeCurrentImageToFile(file)
        }
    }

    private fun saveAllImages() {
        JOptionPane.showInputDialog("Enter a prefix for the image files")?.let { fileNamePrefix ->
            fileChooser.setDescription("Save images")
            showDirectorySelectionDialog()?.let { File(it) }?.let { dir ->
                imageWorld.imageAlbum.writeAllImagesToFile(dir, fileNamePrefix)
            }

        }

    }


    /**
     * Load a set of images to be used as the "Album" in an image album.
     */
    private fun loadImages() {
        fileChooser.setDescription("Select images to load")
        val files = fileChooser.showMultiOpenDialogNative()
        if (files != null) {
            // Load the images

            imageWorld.loadImages(files.filterNotNull().toTypedArray())

            // Update status of buttons
            updateToolbar()

            // Save preferences
            imageDirectory = fileChooser.currentLocation!!
        }
    }

    fun updateToolbar() {
        // Disable next / previous buttons when there is less than two images
        if (imageWorld.numImages < 2) {
            nextImageAction.isEnabled = false
            previousImageAction.isEnabled = false
        } else {
            nextImageAction.isEnabled = true
            previousImageAction.isEnabled = true
        }
        val index = imageWorld.imageAlbum.frameIndex
        val numFrames = imageWorld.imageAlbum.numFrames
        val humanReadableFrameIndex = min((index + 1).toDouble(), numFrames.toDouble()).toInt()
        frameLabel.text = "$humanReadableFrameIndex/$numFrames"
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(800, 600)
    }
}
