package org.simbrain.world.odorworld.gui

import org.simbrain.util.createAction
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.showTilePicker

class OdorWorldActions(val odorWorldPanel: OdorWorldPanel) {

    val addTileAction = odorWorldPanel.createAction("Add Tile") {
        world.addTile()
    }

    val fillLayerAction = odorWorldPanel.createAction("Fill Layer...") {
        showTilePicker(world.tileMap.tileSets) {
            world.tileMap.fill(it)
        }
    }

}