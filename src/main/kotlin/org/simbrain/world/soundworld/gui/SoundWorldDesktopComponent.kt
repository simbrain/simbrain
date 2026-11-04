/**
 * Desktop frame for the sound world, attaching its menu bar to [SoundWorldPanel].
 */
package org.simbrain.world.soundworld.gui

import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.soundworld.SoundWorld
import org.simbrain.world.soundworld.SoundWorldComponent
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

class SoundWorldDesktopComponent(frame: GenericFrame, component: SoundWorldComponent) :
    DesktopComponent<SoundWorldComponent>(frame, component) {
    /**
     * Menu Bar.
     */
    private val menuBar = JMenuBar()

    /**
     * File menu for saving and opening world files.
     */
    private val file = JMenu("File")

    /**
     * Opens the help dialog for SoundWorld.
     */
    private val help = JMenu("Help")

    /**
     * The pane representing the sound world.
     */
    private val panel: SoundWorldPanel = SoundWorldPanel(component.soundWorld)

    /**
     * The sound world.
     */
    private val world: SoundWorld = component.soundWorld

    /**
     * Creates a new frame of type SoundWorld.
     */
    init {
        this.preferredSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        addMenuBar()
        add(panel)
        frame.pack()

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
     * Adds menu bar to the top of SoundWorldComponent.
     */
    private fun addMenuBar() {

        // File Menu
        menuBar.add(file)
        file.add(SimbrainDesktop.actionManager.createImportAction(this))
        file.add(SimbrainDesktop.actionManager.createExportAction(this))
        file.addSeparator()
        file.add(SimbrainDesktop.actionManager.createRenameAction(this))
        file.addSeparator()
        file.add(SimbrainDesktop.actionManager.createCloseAction(this))

        // Help Menu
        menuBar.add(help)
        help.add(JMenuItem(ShowHelpAction("https://docs.simbrain.net/docs/worlds/soundworld.html")))

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