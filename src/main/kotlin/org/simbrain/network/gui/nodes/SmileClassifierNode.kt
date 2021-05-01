package org.simbrain.network.gui.nodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.smile.SmileClassifier
import javax.swing.JDialog

class SmileClassifierNode(val np : NetworkPanel, val classifier : SmileClassifier) : NeuronArrayNode(np,
    classifier) {

    override fun getPropertyDialog(): JDialog? {
        return null
    }

}