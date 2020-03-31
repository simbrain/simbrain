package org.simbrain.world.odorworld

import org.simbrain.util.jTabbedPane
import org.simbrain.util.piccolo.TileSet
import javax.swing.JLabel

fun Collection<TileSet>.tilePicker() = jTabbedPane {
    forEach {  tileSet ->
        tab(tileSet.name) {
            add(JLabel(tileSet.name))
        }
    }
}

