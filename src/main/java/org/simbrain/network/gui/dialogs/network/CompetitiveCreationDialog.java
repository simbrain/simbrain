package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.CompetitiveNetwork;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;

/**
 * <b>CompetitiveDialog</b> is used as an assistant to create Competitive
 * networks.
 */
public class CompetitiveCreationDialog extends StandardDialog {

    /**
     * Competitive properties panel.
     */
    private final AnnotatedPropertyEditor competitivePanel;

    /**
     * Creator object
     */
    private final CompetitiveNetwork.CompetitiveCreator
            cc = new  CompetitiveNetwork.CompetitiveCreator();

    private final NetworkPanel networkPanel;

    public CompetitiveCreationDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        setTitle("New Competitive Network");
        competitivePanel = new AnnotatedPropertyEditor(cc);
        setContentPane(competitivePanel);

        Action helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/network/neurongroups/competitive.html");
        addButton(new JButton(helpAction));

    }

    @Override
    protected void closeDialogOk() {
        competitivePanel.commitChanges();
        CompetitiveNetwork cn = cc.create();
        networkPanel.getNetwork().addNetworkModel(cn);
        super.closeDialogOk();
    }

}