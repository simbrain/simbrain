/**
 * Menu bar for the odor world desktop component, laid out like the network's File | Edit | Insert | View | Help.
 * Every item uses the shared action instances in [org.simbrain.world.odorworld.gui.OdorWorldActions], so enabled
 * state and shortcuts match the toolbar and context menus. Insert holds everything that adds objects to the world.
 */
package org.simbrain.world.odorworld

import org.simbrain.util.onMenuSelected
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.gui.SimbrainDesktop
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JMenuBar

fun OdorWorldDesktopComponent.createMenuBar() = JMenuBar().apply {
    add(createFileMenu())
    add(worldPanel.editMenu)
    add(worldPanel.insertMenu)
    add(worldPanel.viewMenu)
    add(JMenu("Help").apply {
        add(ShowHelpAction("https://docs.simbrain.net/docs/worlds/odorworld.html"))
    })
}

private fun OdorWorldDesktopComponent.createFileMenu() = JMenu("File").apply {
    val actionManager = SimbrainDesktop.actionManager
    add(actionManager.createImportAction<OdorWorldComponent>(this@createFileMenu))
    add(actionManager.createExportAction<OdorWorldComponent>(this@createFileMenu))
    add(actionManager.createRenameAction<OdorWorldComponent>(this@createFileMenu))
    addSeparator()
    add(worldPanel.odorWorldActions.loadTileMapAction)
    addSeparator()
    add(worldPanel.odorWorldActions.showWorldPropertiesAction)
    addSeparator()
    add(actionManager.createCloseAction<OdorWorldComponent>(this@createFileMenu))
}

val OdorWorldPanel.editMenu
    get() = JMenu("Edit").apply {
        with(odorWorldActions) {
            add(selectAllAction)
            add(deleteSelectedAction)
            add(editEntityAction)
            addSeparator()
            add(addTileAction)
            add(fillLayerAction)
            add(createChooseLayerMenu())
            add(editLayersAction)
            add(clearTileMapAction)
            addSeparator()
            add(clearAllTrailsAction)
        }
    }

val OdorWorldPanel.insertMenu
    get() = JMenu("Insert").apply {
        with(odorWorldActions) {
            add(addAgentAction)
            add(addEntityAction)
        }
    }

val OdorWorldPanel.viewMenu
    get() = JMenu("View").apply {
        with(odorWorldActions) {
            add(zoomInAction)
            add(zoomOutAction)
            add(resetZoomAction)
            add(zoomToFitAction)
            addSeparator()
            val showTrails = JCheckBoxMenuItem(showAllTrailsAction)
            add(showTrails)
            addSeparator()
            val showToolbar = JCheckBoxMenuItem(showToolbarAction)
            add(showToolbar)
            onMenuSelected {
                showTrails.isSelected = anyTrailShown
                showToolbar.isSelected = mainToolBar.isVisible
            }
        }
    }
