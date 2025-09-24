package org.simbrain.network.gui.dialogs

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.addSubnetworkAction
import org.simbrain.network.layouts.Layout
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.widgets.ShowHelpAction
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTabbedPane

/**
 * Dialog for creating discrete Hopfield networks with undo/redo support.
 */
class HopfieldCreationDialog(private val networkPanel: NetworkPanel) : StandardDialog() {

    private val tabbedPane = JTabbedPane()
    private val tabLogic = JPanel()
    private val tabLayout = JPanel()
    private val hc = Hopfield.HopfieldCreator()
    private val layoutEditor = Layout.LayoutEditor()

    private val hopPropertiesPanel: AnnotatedPropertyEditor<Hopfield.HopfieldCreator>
    private val layoutPanel: AnnotatedPropertyEditor<Layout.LayoutEditor>

    init {
        title = "New Hopfield Network"

        // Logic Panel
        hopPropertiesPanel = AnnotatedPropertyEditor(hc)
        tabLogic.layout = FlowLayout()
        tabLogic.add(hopPropertiesPanel)

        // Layout panel
        layoutPanel = AnnotatedPropertyEditor(layoutEditor)
        tabLayout.add(layoutPanel)

        // Set it all up
        tabbedPane.addTab("Logic", tabLogic)
        tabbedPane.addTab("Layout", layoutPanel)
        contentPane = tabbedPane

        // Help action
        val helpAction = ShowHelpAction("https://docs.simbrain.net/docs/network/subnetworks/hopfield.html")
        addButton(JButton(helpAction))
    }

    override fun closeDialogOk() {
        hopPropertiesPanel.commitChanges()
        val hopfield = hc.create()
        layoutPanel.commitChanges()
        
        // Handle layout based on neuron count
        if (hopfield.neuronGroup.size == 2) {
            val neuron1 = hopfield.neuronGroup.getNeuron(0)
            val neuron2 = hopfield.neuronGroup.getNeuron(1)
            neuron2.setLocation(neuron1.x + 100, neuron1.y)
        } else {
            hopfield.neuronGroup.layout = layoutEditor.layout
            hopfield.neuronGroup.applyLayout()
        }
        
        // Add network with undo/redo support
        addSubnetworkAction(networkPanel) {
            hopfield
        }
        
        networkPanel.repaint()
        super.closeDialogOk()
    }
}