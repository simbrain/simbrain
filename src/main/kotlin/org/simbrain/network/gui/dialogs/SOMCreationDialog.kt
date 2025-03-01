package org.simbrain.network.gui.dialogs

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.addSubnetworkAction
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.widgets.ShowHelpAction
import javax.swing.JButton

/**
 * Dialog for creating SOM networks with undo/redo support.
 */
class SOMCreationDialog(private val networkPanel: NetworkPanel) : StandardDialog() {

    private val somPanel: AnnotatedPropertyEditor<SOMNetwork.SOMCreator>
    private val sc = SOMNetwork.SOMCreator()

    init {
        title = "New SOM Network"
        somPanel = AnnotatedPropertyEditor(sc)
        contentPane = somPanel

        val helpAction = ShowHelpAction("https://docs.simbrain.net/docs/network/subnetworks/selfOrganizingMap.html")
        addButton(JButton(helpAction))
    }

    override fun closeDialogOk() {
        somPanel.commitChanges()
        addSubnetworkAction(networkPanel) { sc.create() }
        
        super.closeDialogOk()
    }
}