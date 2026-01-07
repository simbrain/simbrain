package org.simbrain.world.imageworld

import kotlinx.coroutines.CoroutineScope
import org.simbrain.util.SFileChooser
import org.simbrain.util.createAction
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.swingDispatcher
import org.simbrain.util.swingInvokeLater
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.gui.CouplingMenu
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import org.simbrain.world.imageworld.ImageWorldPreferences.imageDirectory
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import org.simbrain.world.imageworld.gui.ImagePipelineCollectionGui
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import kotlin.math.min

class ImageWorldDesktopComponent(frame: GenericFrame, component: ImageWorldComponent) :
    DesktopComponent<ImageWorldComponent>(frame, component), CoroutineScope by component.world {

    private val imageToolbar = JToolBar()

    private val imageWorld: ImageWorld = component.world

    private val imageWorldPanel = ImageWorldPanel(component)

    private val frameLabel = JLabel()

    private val outputSizeLabel = JLabel()

    private fun setupMenuBar(frame: GenericFrame) {
        val menuBar = JMenuBar()
        val fileMenu = JMenu("File  ")
        menuBar.add(fileMenu)

        // Add load images menu item if it's an image album world
        val loadImages = JMenuItem(createAction("Load images...") { loadImages() })
        fileMenu.add(loadImages)

        fileMenu.add(imageWorldPanel.saveImageAction)

        val saveAllImages = JMenuItem(imageWorldPanel.saveImageAllAction)
        fileMenu.add(saveAllImages)

        fileMenu.addSeparator()
        fileMenu.add(imageWorldPanel.copyAction)
        fileMenu.add(imageWorldPanel.pasteAction)

        fileMenu.addSeparator()
        fileMenu.add(actionManager.createImportAction(this))
        fileMenu.add(actionManager.createExportAction(this))
        fileMenu.addSeparator()
        fileMenu.add(actionManager.createRenameAction(this))
        fileMenu.addSeparator()
        fileMenu.add(actionManager.createCloseAction(this))

        // Edit Menu
        val editMenu = JMenu("Edit")

        val resetCanvasAction = createAction(
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
     * Set up toolbars depending on what type of world is being displayed.
     * Bottom toolbar contains navigation/read operations only.
     * Editing tools are in the top toolbar (see ImagePipelineCollectionGui).
     */
    private fun setupToolbars() {
        imageToolbar.add(frameLabel)
        imageToolbar.add(imageWorldPanel.deleteImageAction)
        imageToolbar.add(imageWorldPanel.previousImageAction)
        imageToolbar.add(imageWorldPanel.nextImageAction)
        imageToolbar.add(imageWorldPanel.takeSnapshotAction)
        imageToolbar.addSeparator()
        imageToolbar.add(outputSizeLabel)
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
        add(ImageWorldPanel(component), BorderLayout.CENTER)
        imageWorld.imageAlbum.events.imageUpdate.on(swingDispatcher) {
            updateToolbar()
            repaint()
        }
        imageWorld.imagePipelineCollection.events.pipelineChanged.on(swingDispatcher) { _: ImageProcessingPipeline, _: ImageProcessingPipeline ->
            updateToolbar()
            repaint()
        }
        imageWorld.imagePipelineCollection.events.pipelineSelectionChanged.on(swingDispatcher) { _: ImageProcessingPipeline ->
            updateToolbar()
            repaint()
        }

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
     * Load a set of images to be used as the "Album" in an image album.
     */
    private suspend fun loadImages() {
        val fileChooser = SFileChooser(imageDirectory, "")
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
            imageWorldPanel.nextImageAction.isEnabled = false
            imageWorldPanel.previousImageAction.isEnabled = false
        } else {
            imageWorldPanel.nextImageAction.isEnabled = true
            imageWorldPanel.previousImageAction.isEnabled = true
        }
        val index = imageWorld.imageAlbum.frameIndex
        val numFrames = imageWorld.imageAlbum.numFrames
        val humanReadableFrameIndex = min((index + 1).toDouble(), numFrames.toDouble()).toInt()
        frameLabel.text = "$humanReadableFrameIndex/$numFrames"

        // Update canvas size label (shows pipeline output size)
        val processedImage = imageWorld.imagePipelineCollection.currentPipeline.processedImage
        outputSizeLabel.text = "${processedImage.width}×${processedImage.height}"
        outputSizeLabel.toolTipText = "Output size: ${processedImage.width} × ${processedImage.height} pixels"
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(800, 600)
    }
}
