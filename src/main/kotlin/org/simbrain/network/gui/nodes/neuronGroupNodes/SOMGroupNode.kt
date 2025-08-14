package org.simbrain.network.gui.nodes.neuronGroupNodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createTooltipTextWithLocation
import org.simbrain.network.gui.nodes.NeuronGroupNode
import org.simbrain.network.neurongroups.SOMGroup
import org.simbrain.util.Utils
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JMenuItem

/**
 * PNode representation of Self-Organizing Map.
 *
 * @author jyoshimi
 */
class SOMGroupNode(networkPanel: NetworkPanel?, group: SOMGroup?) : NeuronGroupNode(networkPanel, group) {
    /**
     * Create a SOM Network PNode.
     *
     * @param networkPanel parent panel
     * @param group        the SOM network
     */
    init {
        // setStrokePaint(Color.green);
        setCustomMenuItems()
        interactionBox = SOMInteractionBox(networkPanel)
        // setOutlinePadding(15f);
        updateText()
    }

    /**
     * Custom interaction box for SOM group node.
     */
    private inner class SOMInteractionBox(net: NetworkPanel?) : NeuronGroupInteractionBox(net) {
        override val toolTipText: String
            get() = createTooltipTextWithLocation(model) {
                "Current learning rate: " + Utils.round(
                    (neuronGroup as SOMGroup).learningRate,
                    2
                ) + "\nCurrent neighborhood size: " + Utils.round(
                    (neuronGroup as SOMGroup).neighborhoodSize, 2
                )
            }
    }

    /**
     * Sets custom menu for SOM node.
     */
    protected fun setCustomMenuItems() {
        super.addCustomMenuItem(JMenuItem(object : AbstractAction("Reset SOM Network") {
            override fun actionPerformed(event: ActionEvent) {
                val group = (neuronGroup as SOMGroup)
                group.reset()
            }
        }))
        super.addCustomMenuItem(JMenuItem(object : AbstractAction("Recall SOM Memory") {
            override fun actionPerformed(event: ActionEvent) {
                val group = (neuronGroup as SOMGroup)
                group.recall()
            }
        }))
        super.addCustomMenuItem(JMenuItem(object : AbstractAction("Randomize SOM Weights") {
            override fun actionPerformed(event: ActionEvent) {
                val group = (neuronGroup as SOMGroup)
                group.randomizeIncomingWeights()
            }
        }))
    }
}
