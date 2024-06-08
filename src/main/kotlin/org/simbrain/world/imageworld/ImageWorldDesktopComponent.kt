package org.simbrain.world.imageworld

import org.simbrain.util.*
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.gui.CouplingMenu
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import org.simbrain.world.imageworld.ImageWorldPreferences.imageDirectory
import org.simbrain.world.imageworld.gui.FilterCollectionGui
import java.awt.*
import java.awt.event.*
import java.io.File
import java.util.*
import javax.swing.*
import kotlin.math.min

class ImageWorldDesktopComponent(frame: GenericFrame, component: ImageWorldComponent) :
    DesktopComponent<ImageWorldComponent>(frame, component) {

    private val toolbars = JPanel(FlowLayout(FlowLayout.LEFT))
    private val sourceToolbar = JToolBar()
    private val imageAlbumToolbar = JToolBar()
    private val sensorToolbar = JToolBar()

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

    private val frameLabel = JLabel()

    private val deleteImageAction = createAction(
        "Delete image",
        description = "Delete current image",
        iconPath =  "menu_icons/RedX.png"
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
                    drawPixel(evt)
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
            imageWorld.filterCollection.currentFilter.updateFilter()
            g.drawImage(
                imageWorld.filterCollection.currentFilter.filteredImage,
                0, 0, width, height, this
            )
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
            if (x < 0 || x >= image.width || y < 0 || y >= image.height) {
                return
            }
            if (evt.isShiftDown) {
                image.setRGB(x, y, penColor.invert().rgb)
            } else {
                image.setRGB(x, y, penColor.rgb)
            }
            imageWorld.imageAlbum.fireImageUpdate()
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
            editMenu.add(CouplingMenu(workspaceComponent,  imageWorld.filterCollection.currentFilter))
        }
        swingInvokeLater {
            createEditMenu()
            onCouplingAttributesChanged { createEditMenu() }
        }

        // Help Menu
        val helpMenu = JMenu("Help")
        val helpItem = JMenuItem("World Help")
        menuBar.add(helpMenu)
        val helpAction = ShowHelpAction("Pages/Worlds/ImageWorld/ImageWorld.html")
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
        val filterMenu = CouplingMenu(
            workspaceComponent, imageWorld.filterCollection.currentFilter
        )
        contextMenu.add(filterMenu)
        contextMenu.show(this, evt.x, evt.y)
    }

    /**
     * Set up toolbars depending on what type of world is being displayed
     */
    private fun setupToolbars() {

        imageAlbumToolbar.add(frameLabel)
        imageAlbumToolbar.add(deleteImageAction)
        imageAlbumToolbar.add(previousImageAction)
        imageAlbumToolbar.add(nextImageAction)
        imageAlbumToolbar.add(takeSnapshotAction)

        //        // Add Color Picker
        //        JButton setColorButton = new JButton();
        //        setColorButton.setIcon(ResourceManager.getSmallIcon("menu_icons/PaintView.png"));
        //        setColorButton.setToolTipText("Pen Color");
        //        setColorButton.addActionListener(e -> {
        //            Color newColor = JColorChooser.showDialog(this, "Choose Color", penColor);
        //            if (newColor != null) {
        //                this.penColor = newColor;
        //            }
        //        });
        //        sourceToolbar.add(setColorButton);
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

        // Check box handling
        val checkBoxDrawMode = JCheckBox("Draw")
        checkBoxDrawMode.isSelected = paintMode
        cbColorChoice.isEnabled = checkBoxDrawMode.isSelected
        checkBoxDrawMode.addItemListener { e: ItemEvent? ->
            paintMode = checkBoxDrawMode.isSelected
            cbColorChoice.setEnabled(checkBoxDrawMode.isSelected)
        }

        cbColorChoice.addActionListener { e: ActionEvent ->
            val len = cbColorChoice.itemCount
            if ((e.source as JComboBox<*>).selectedIndex == len - 1) {
                println("Custom...")
            } else {
                this.penColor = colorList[cbColorChoice.selectedIndex]
            }
        }

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

        sourceToolbar.add(checkBoxDrawMode)
        sourceToolbar.add(cbColorChoice)
        sourceToolbar.add(fillCanvasAction)
        sourceToolbar.add(clearCanvasAction)
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
        imageWorld.filterCollection.events.filterChanged.on(swingDispatcher) { _, _ -> this.repaint() }

        // Toolbars
        add(toolbars, BorderLayout.NORTH)
        toolbars.add(sourceToolbar)
        toolbars.add(sensorToolbar)
        val filterGui = FilterCollectionGui(this, imageWorld.filterCollection)
        toolbars.add(filterGui.toolBar)

        setupToolbars()

        add(imageAlbumToolbar, BorderLayout.SOUTH)
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

            imageWorld.loadImages(files)

            // Update status of buttons
            updateToolbar()

            // Save preferences
            imageDirectory = fileChooser.currentLocation
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
        return Dimension(650, 500)
    }
}
