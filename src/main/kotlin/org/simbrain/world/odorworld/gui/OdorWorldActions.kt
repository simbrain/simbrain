package org.simbrain.world.odorworld.gui

import org.simbrain.util.createAction
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.showTilePicker
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

}