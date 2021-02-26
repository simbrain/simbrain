package org.simbrain.network.gui.nodes

import org.piccolo2d.nodes.PPath
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.smile.SmileSVM
import java.awt.Color
import javax.swing.JDialog
import javax.swing.JPopupMenu

class SmileSVMNode(network : NetworkPanel, val svn : SmileSVM) : ScreenElement(network) {

    init {
        svn.events.onDeleted { removeFromParent() };
    }

    private val square = createRectangle(0.0,0.0,100.0,100.0).let {
        addChild(it)
        setBounds(it.bounds)
        it.paint = Color.blue
    }

    override fun getModel(): SmileSVM {
        return svn
    }

    override fun isSelectable() : Boolean = true

    override fun isDraggable(): Boolean = true

}