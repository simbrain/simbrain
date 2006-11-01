package org.simbrain.network.dialog.network;

import javax.swing.JTextField;
import javax.swing.JButton;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.networks.Backprop;
import org.simbrain.network.actions.ShowHelpAction;

/**
 * <b>BackpropPropertiesDialog</b> is a dialog box for setting the properties of a backprop network.
 *
 */
public class BackpropPropertiesDialog extends StandardDialog {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Epsilon value field. */
    private JTextField tfEta = new JTextField();

    /** Winner value field. */
    private JTextField tfMu = new JTextField();

    /** The model subnetwork. */
    private Backprop backprop;

     /** Help Button. */
     private JButton helpButton = new JButton("Help");

     /** Show Help Action. */
     private ShowHelpAction helpAction = new ShowHelpAction();

    /**
     * Default constructor.
     *
     * @param backprop Backprop network being modified.
     */
    public BackpropPropertiesDialog(final Backprop backprop) {
        this.backprop = backprop;
        setTitle("Set Backprop Properties");
        fillFieldValues();
        this.setLocation(500, 0); //Sets location of network dialog
        helpAction.setTheURL("Network/network/backpropnetwork.html");

        helpButton.setAction(helpAction);
        this.addButton(helpButton);
        mainPanel.addItem("Learning Rate", tfEta);
        mainPanel.addItem("Momentum", tfMu);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      backprop.setEta(Double.parseDouble(tfEta.getText()));
      backprop.setMu(Double.parseDouble(tfMu.getText()));
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfEta = new JTextField("" + backprop.getEta());
        tfMu = new JTextField("" + backprop.getMu());
    }
}
