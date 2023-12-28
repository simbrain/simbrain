package org.simbrain.world.odorworld.gui

import org.simbrain.util.CmdOrCtrl
import org.simbrain.util.createAction
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.showTilePicker
import java.awt.event.KeyEvent
import javax.swing.JOptionPane

class OdorWorldActions(val odorWorldPanel: OdorWorldPanel) {

    val showInfoAction = odorWorldPanel.createAction("Get info...") {
        // TODO: Provide more information. For now size was all I needed.
        JOptionPane.showMessageDialog(null, "World is ${world.width} by ${world.height} pixels",
            "Odor world info", JOptionPane.INFORMATION_MESSAGE)
    }

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