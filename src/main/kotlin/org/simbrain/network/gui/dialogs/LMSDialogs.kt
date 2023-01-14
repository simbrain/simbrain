package org.simbrain.network.gui.dialogs

import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.LMSNetworkNode
import org.simbrain.network.subnetworks.LMSNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator

/**
 * Creation dialog for [LMSNetwork]
 */
fun NetworkPanel.showLMSCreationDialog(): StandardDialog {

    val creator = LMSNetwork.LMSCreator(
        network.idManager.getProposedId(LMSNetwork::class.java),
        network.placementManager.lastClickedLocation
    )
    return creator.createDialog {
        network.addNetworkModel(creator.create(network))
    }

}

fun LMSNetwork.getTrainingDialog(): StandardDialog {
    return StandardDialog().apply {

        title = "Train LMS Network"
        contentPane = JPanel()
        layout = MigLayout("gap 0px 0px, ins 0")

        val trainerProps = AnnotatedPropertyEditor(trainer)
        val trainerControls = TrainerControls(trainer)
        val inputs = MatrixEditor(trainingSet.inputs)
        val targets = MatrixEditor(trainingSet.targets)
        val addRemoveRows = AddRemoveRows(inputs.table, targets.table)

        trainer.events.beginTraining.on {
            trainerProps.commitChanges()
        }

        contentPane.add(trainerProps, "span, wrap")
        contentPane.add(JSeparator(), "span, growx, wrap")
        contentPane.add(trainerControls, "span, growx, wrap")
        contentPane.add(JSeparator(), "span, growx, wrap")
        contentPane.add(JLabel("Inputs"))
        contentPane.add(JLabel("Targets"), "wrap")
        contentPane.add(inputs)
        contentPane.add(targets, "wrap")
        contentPane.add(JLabel("Add / Remove rows:"), "split 2")
        contentPane.add(addRemoveRows)
    }
}

fun main() {
    val networkComponent = NetworkComponent("")
    val np = NetworkPanel(networkComponent)
    val result = with(networkComponent.network) {
        val lmsNet = LMSNetwork(this, 5, 5)
        addNetworkModel(lmsNet)
        lmsNet
    }
    LMSNetworkNode(np,result ).propertyDialog.run { makeVisible() }
}
