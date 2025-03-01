package org.simbrain.network.gui.dialogs

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.addSubnetworkAction
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.widgets.ShowHelpAction
import javax.swing.JButton

/**
 * Dialog for creating Competitive networks with undo/redo support.
 */
class CompetitiveCreationDialog(private val networkPanel: NetworkPanel) : StandardDialog() {

    private val competitivePanel: AnnotatedPropertyEditor<CompetitiveNetwork.CompetitiveCreator>
    private val cc = CompetitiveNetwork.CompetitiveCreator()

    init {
        title = "New Competitive Network"
        competitivePanel = AnnotatedPropertyEditor(cc)
        contentPane = competitivePanel

        val helpAction = ShowHelpAction("https://docs.simbrain.net/docs/network/neurongroups/competitive.html")
        addButton(JButton(helpAction))
    }

    override fun closeDialogOk() {
        competitivePanel.commitChanges()
        addSubnetworkAction(networkPanel) { cc.create() }
        super.closeDialogOk()
    }
}