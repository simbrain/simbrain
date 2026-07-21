package org.simbrain.world.textworld.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.CouplingMenu
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.textworld.*
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

/**
 * **ReaderComponentDesktopGui** is the gui view for the reader world.
 */
class TextWorldDesktopComponent(frame: GenericFrame, component: TextWorldComponent) :
    DesktopComponent<TextWorldComponent>(frame, component) {
    /**
     * Menu Bar.
     */
    private val menuBar = JMenuBar()

    /**
     * File menu for saving and opening world files.
     */
    private val file = JMenu("File")

    /**
     * Edit menu Item.
     */
    private val edit = JMenu("Edit")

    /**
     * Opens user preferences dialog.
     */
    private val preferences = JMenuItem("Preferences")

    /**
     * Opens the help dialog for TextWorld.
     */
    private val help = JMenu("Help")

    /**
     * Help menu item.
     */
    private val helpItem = JMenuItem("Reader Help")

    /**
     * The pane representing the text world.
     */
    val panel: TextWorldPanel

    /**
     * The text world.
     */
    private val world: TextWorld

    /**
     * Creates a new frame of type TextWorld.
     *
     * @param frame
     * @param component
     */
    init {
        world = component.world
        panel = TextWorldPanel(world)
        this.preferredSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        addMenuBar()
        add(panel)
        frame.pack()

        val updater = component.workspace.updater
        updater.events.runStarted.on(Dispatchers.Swing) { panel.setRunLock(world.lockWhileRunning) }
        updater.events.runFinished.on(Dispatchers.Swing) { panel.setRunLock(false) }
        panel.setRunLock(world.lockWhileRunning && updater.isRunning)

        // Force component to fill up parent panel
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                val component = e.component
                panel.preferredSize = Dimension(component.width, component.height)
                panel.revalidate()
            }
        })
        parentFrame.pack()
    }

    /**
     * Adds menu bar to the top of TextWorldComponent.
     */
    private fun addMenuBar() {

        // File Menu
        menuBar.add(file)
        file.add(SimbrainDesktop.actionManager.createImportAction(this))
        file.add(SimbrainDesktop.actionManager.createExportAction(this))
        file.addSeparator()
        file.add(loadTextAction)
        file.addSeparator()
        file.add(SimbrainDesktop.actionManager.createRenameAction(this))
        file.addSeparator()
        file.add(SimbrainDesktop.actionManager.createCloseAction(this))

        // Edit menu
        preferences.action = world.textWorldPrefs
        edit.add(createShowFindAndReplaceAction())
        edit.addSeparator()
        edit.add(
            SimbrainDesktop.actionManager.createCoupledDataWorldAction(
                name = "Record token embeddings",
                world.getProducer(TextWorld::currentVector),
                sourceName = "${world.id} Token Embeddings",
                world.tokenEmbedding.dimension
            )
        )
        edit.add(
            SimbrainDesktop.actionManager.createCoupledPlotMenu(
                world.getProducer(TextWorld::currentVector),
                "${world.id} Token Embeddings",
            )
        )
        edit.addSeparator()
        edit.add(CouplingMenu(workspaceComponent, world))
        edit.addSeparator()
        edit.add(preferences)
        menuBar.add(edit)

        // View Menu
        val viewMenu = JMenu("View")
        viewMenu.add(world.extractEmbeddingFromCurrentText)
        viewMenu.add(world.viewTokenEmbedding)
        menuBar.add(viewMenu)

        // Help Menu
        menuBar.add(help)
        val helpAction = ShowHelpAction("https://docs.simbrain.net/docs/worlds/textworld.html")
        helpItem.action = helpAction
        help.add(helpItem)

        // Add menu
        parentFrame.jMenuBar = menuBar
    }

    companion object {
        /**
         * Default height.
         */
        private const val DEFAULT_HEIGHT = 250

        /**
         * Default width.
         */
        private const val DEFAULT_WIDTH = 400
    }
}