package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.Subnetwork;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;

/**
 * Default subnetwork panel that displays basic info. Most subnetworks should
 * have custom panels so this should not typically be seen.
 *
 * @author Jeff Yoshimi
 */
public class SubnetworkPanel extends JPanel {


    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Subnetwork.
     */
    private final Subnetwork subnetwork;

    /**
     * Constructor for case where an existing subnetwork is being
     * edited.
     *
     * @param np           Parent network panel
     * @param sn           subnetwork being edited
     * @param parentDialog parent dialog containing this panel.
     */
    public SubnetworkPanel(final NetworkPanel np, final Subnetwork sn, final StandardDialog parentDialog) {
        this.networkPanel = np;
        this.subnetwork = sn;
        initPanel(parentDialog);
    }

    /**
     * Initialize the panel.
     *
     * @param parentDialog the parent window
     */
    private void initPanel(final StandardDialog parentDialog) {

        // Set title
        parentDialog.setTitle("Edit " + subnetwork.getClass().getSimpleName());

        // Set up help button
        Action helpAction;
        helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/network/subnetworks/l");
        parentDialog.addButton(new JButton(helpAction));
    }

}
