package org.simbrain.network.gui.dialogs

import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.LMSNetworkNode
import org.simbrain.network.subnetworks.LMSNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.Trainable2
import org.simbrain.util.StandardDialog
import org.simbrain.util.createDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator


/**
 * Generic training dialog for supervised learning.
 */
fun Trainable2.getTrainingDialog(): StandardDialog {
    return StandardDialog().apply {

        title = "Train Network"
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

fun NetworkPanel.showSRNCreationDialog(): StandardDialog {

    val creator = SRNNetwork.SRNCreator(
        network.idManager.getProposedId(SRNNetwork::class.java),
        network.placementManager.lastClickedLocation
    )
    return creator.createDialog {
        network.addNetworkModelAsync(creator.create(network))
    }

}

// fun main() {
//     val networkComponent = NetworkComponent("")
//     val np = NetworkPanel(networkComponent)
//     val result = with(networkComponent.network) {
//         val lmsNet = LMSNetwork(this, 5, 5)
//         addNetworkModelAsync(lmsNet)
//         lmsNet
//     }
//     LMSNetworkNode(np,result ).propertyDialog.run { makeVisible() }
// }


fun main() {
    val networkComponent = NetworkComponent("")
    val np = NetworkPanel(networkComponent)
    val result = with(networkComponent.network) {
        val lmsNet = LMSNetwork(this, 5, 5)
        addNetworkModelAsync(lmsNet)
        lmsNet
    }
    LMSNetworkNode(np,result ).propertyDialog.run { makeVisible() }
}


/**
 * Creation dialog for [LMSNetwork]
 */
fun NetworkPanel.showLMSCreationDialog(): StandardDialog {

    val creator = LMSNetwork.LMSCreator(
        network.idManager.getProposedId(LMSNetwork::class.java),
        network.placementManager.lastClickedLocation
    )
    return creator.createDialog {
        network.addNetworkModelAsync(creator.create(network))
    }

}