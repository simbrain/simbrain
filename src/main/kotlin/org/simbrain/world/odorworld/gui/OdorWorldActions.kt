package org.simbrain.world.odorworld.gui

import org.simbrain.util.*
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.dialogs.EntityDialog
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.showTilePicker
import java.awt.event.KeyEvent

class OdorWorldActions(val odorWorldPanel: OdorWorldPanel) {

    fun addAgentAction() = odorWorldPanel.createAction(
        name = "Add agent",
        iconPath = "odorworld/rotating/mouse/Mouse_225.gif",
        keyboardShortcut = CmdOrCtrl + 'P'
    ) {
        world.addAgent()
    }

    fun addEntityAction() = odorWorldPanel.createAction(
        name = "Add entity",
        iconPath = "odorworld/static/Swiss.gif",
        keyboardShortcut = 'P'
    ) {
        world.addEntity()
    }

    fun deleteSelectedAction() = odorWorldPanel.createAction(
        name = "Delete selected entities",
        iconPath = "menu_icons/Eraser.png",
        keyboardShortcuts = listOf(KeyCombination(KeyEvent.VK_DELETE), KeyCombination(KeyEvent.VK_BACK_SPACE))
    ) {
        odorWorldPanel.deleteSelectedEntities()
    }

    fun showWorldPrefsAction() = odorWorldPanel.createAction(
        name = "Preferences...",
        iconPath = "menu_icons/Prefs.png",
        keyboardShortcut = CmdOrCtrl + ','
    ) {
        world.createEditorDialog().apply { title = "World Preferences" }.display()
    }

    val showPropertyDialogAction = odorWorldPanel.createAction(
        name = "Edit..",
        iconPath = "menu_icons/Properties.png",
        keyboardShortcut = CmdOrCtrl + 'E'
    ) {
        odorWorldPanel.selectedEntities.firstOrNull()?.let {
            EntityDialog(it.entity).apply { title = "Edit ${it.entity.name}"  }.display()
        }
    }

    // TODO: Add images and to toolbar
    val addTileAction = odorWorldPanel.createAction("Add Tile") {
        world.addTile()
    }

    val fillLayerAction = odorWorldPanel.createAction("Fill Layer...") {
        showTilePicker(world.tileMap.tileSets) {
            world.tileMap.fill(it)
        }
    }

    val clearAllTrails = odorWorldPanel.createAction("Clear all trails") {
        world.entityList.map {
            it.clearTrail()
        }
    }

    val turnOnTrails = odorWorldPanel.createAction("Turn on trails") {
        world.entityList.map {
            it.isShowTrail = true
        }
    }

    val turnOffTrails = odorWorldPanel.createAction("Turn off trails") {
        world.entityList.map {
            it.isShowTrail = false
        }
    }

    @JvmOverloads
    fun toggleTrailAction(entity: OdorWorldEntity) = odorWorldPanel.createAction("Toggle show trails") {
        entity.isShowTrail = !entity.isShowTrail
    }

    fun resetZoomAction() = odorWorldPanel.createAction(
        "Reset Zoom",
        iconPath = "menu_icons/ZoomFitPage.png",
        keyboardShortcut = CmdOrCtrl + KeyEvent.VK_0
    ) {
        scalingFactor = 1.0
    }

    fun zoomInAction() = odorWorldPanel.createAction(
        "Zoom In",
        iconPath = "menu_icons/ZoomIn.png",
        keyboardShortcuts = listOf(CmdOrCtrl + KeyEvent.VK_ADD, CmdOrCtrl + KeyEvent.VK_EQUALS)
    ) {
        scalingFactor *= 1.1
    }

    fun zoomOutAction() = odorWorldPanel.createAction(
        "Zoom Out",
        iconPath = "menu_icons/ZoomOut.png",
        keyboardShortcuts = listOf(CmdOrCtrl + KeyEvent.VK_SUBTRACT, CmdOrCtrl + KeyEvent.VK_MINUS)
    ) {
        scalingFactor /= 1.1
    }

}