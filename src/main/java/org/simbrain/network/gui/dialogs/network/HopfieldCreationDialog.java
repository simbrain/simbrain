package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import java.awt.*;

/**
 * <b>DiscreteHopfieldDialog</b> is a dialog box for creating discrete Hopfield
 * networks.
 */
public class HopfieldCreationDialog extends StandardDialog {

    /**
     * Tabbed pane.
     */
    private final JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Logic tab panel.
     */
    private final JPanel tabLogic = new JPanel();

    /**
     * Layout tab panel.
     */
    private final JPanel tabLayout = new JPanel();

    /**
     * Creator object
     */
    private final Hopfield.HopfieldCreator hc = new Hopfield.HopfieldCreator();

    /**
     * Logic panel.
     */
    private final AnnotatedPropertyEditor hopPropertiesPanel;

    /**
     * Layout to use in property editor.
     */
    private final Layout.LayoutEditor layoutEditor =  new Layout.LayoutEditor();

    /**
     * Layout panel.
     */
    private final AnnotatedPropertyEditor layoutPanel;

    /**
     * Network Panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     *
     * @param networkPanel Network panel
     */
    public HopfieldCreationDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;

        setTitle("New Hopfield Network");

        // Logic Panel
        hopPropertiesPanel = new AnnotatedPropertyEditor(hc);
        tabLogic.setLayout(new FlowLayout());
        tabLogic.add(hopPropertiesPanel);

        // Layout panel
        layoutPanel = new AnnotatedPropertyEditor(layoutEditor);
        tabLayout.add(layoutPanel);

        // Set it all up
        tabbedPane.addTab("Logic", tabLogic);
        tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(tabbedPane);

        // Help action
        Action helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/network/subnetworks/hopfield.html");
        addButton(new JButton(helpAction));

    }

    @Override
    protected void closeDialogOk() {
        hopPropertiesPanel.commitChanges();
        Hopfield hopfield = hc.create();
        layoutPanel.commitChanges();
        if (hopfield.neuronGroup.getSize() == 2) {
            var neuron1 = hopfield.neuronGroup.getNeuron(0);
            var neuron2 = hopfield.neuronGroup.getNeuron(1);
            neuron2.setLocation(neuron1.getX() + 100, neuron1.getY());
        } else {
            hopfield.getNeuronGroup().setLayout(layoutEditor.getLayout());
            hopfield.getNeuronGroup().applyLayout();
        }
        hopfield.reapplyOffsets();
        networkPanel.getNetwork().addNetworkModelAsync(hopfield);
        networkPanel.repaint();
        super.closeDialogOk();
    }
}
