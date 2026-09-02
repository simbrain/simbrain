package org.simbrain.world.odorworld

import org.simbrain.util.SFileChooser
import org.simbrain.util.createAction
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.gui.OdorWorldActions
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

/**
 * **OdorWorldFrameMenu**.
 */
class OdorWorldFrameMenu(private val parent: OdorWorldDesktopComponent, private val world: OdorWorld) : JMenuBar() {
    private val odorWorldActions: OdorWorldActions
    
    private val fileMenu = JMenu("File  ")

    private val editMenu = JMenu("Edit  ")

    private val copyItem = JMenuItem("Copy")

    private val cutItem = JMenuItem("Cut")

    private val pasteItem = JMenuItem("Paste")

    private val helpMenu = JMenu("Help")

    private val helpItem = JMenuItem("World help")

    init {
        odorWorldActions = parent.worldPanel.odorWorldActions
    }

    fun setUpMenus() {
        setUpFileMenu()
        setUpEditMenu()

        // Help Menu
        add(helpMenu)
        val helpAction = ShowHelpAction("https://docs.simbrain.net/docs/worlds/odorworld.html")
        helpItem.setAction(helpAction)
        helpMenu.add(helpItem)
    }

    fun setUpFileMenu() {
        add(fileMenu)
        fileMenu.add(SimbrainDesktop.actionManager.createImportAction<OdorWorldComponent>(parent))
        fileMenu.add(SimbrainDesktop.actionManager.createExportAction<OdorWorldComponent>(parent))
        fileMenu.addSeparator()

        fileMenu.add(object : AbstractAction("Load tile map...") {
            override fun actionPerformed(e: ActionEvent?) {
                val chooser = SFileChooser(OdorWorldPreferences.tileMapDirectory, "Load TMX Tilemap", null, true)
                chooser.addExtension("tmx")
                val theFile = chooser.showOpenDialog()
                if (theFile != null) {
                    world.tileMap = loadTileMap(theFile)
                    OdorWorldPreferences.tileMapDirectory = chooser.currentLocation!!
                }
            }
        })

        fileMenu.add(odorWorldActions.clearTileMapAction())

        fileMenu.addSeparator()
        fileMenu.add(odorWorldActions.showWorldPropertiesAction())
        fileMenu.addSeparator()
        fileMenu.add(SimbrainDesktop.actionManager.createRenameAction<OdorWorldComponent>(parent))
        fileMenu.addSeparator()
        fileMenu.add(SimbrainDesktop.actionManager.createCloseAction<OdorWorldComponent>(parent))
    }

    fun setUpEditMenu() {
        add(editMenu)

        // editMenu.add(cutItem);
        // editMenu.add(copyItem);
        // editMenu.add(pasteItem);
        // editMenu.addSeparator();

        // TODO: Factor the code for placing new entities out of network, to utils, and reuse here.
        val addEntity = JMenuItem(parent.worldPanel.createAction("Add entity") { world.addEntity() })
        editMenu.add(addEntity)
        val addAgent = JMenuItem(parent.worldPanel.createAction("Add agent") { world.addAgent() })
        editMenu.add(addAgent)
        editMenu.addSeparator()
        editMenu.add(odorWorldActions.deleteSelectedAction())
        editMenu.addSeparator()
        editMenu.add(odorWorldActions.editLayersAction)
        editMenu.addSeparator()

        editMenu.add(odorWorldActions.toggleAllTrails)
        editMenu.add(odorWorldActions.clearAllTrails)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}