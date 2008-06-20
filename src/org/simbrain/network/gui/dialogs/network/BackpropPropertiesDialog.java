package org.simbrain.network.gui.dialogs.network;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JButton;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.network.gui.actions.ShowHelpAction;
import org.simbrain.network.networks.Backprop;

/**
 * <b>BackpropPropertiesDialog</b> is a dialog box for setting the properties of a backprop network.
 *
 */
public class BackpropPropertiesDialog extends StandardDialog {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Epsilon value field. */
    private JTextField tfEta = new JTextField();

    /** Bias epsilon value field. */
    private JTextField tfBiasEta = new JTextField();

    /** Winner value field. */
    private JTextField tfMu = new JTextField();
    
    private JCheckBox cbTrain = new JCheckBox();
    
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
        mainPanel.addItem("Bias Learning Rate", tfBiasEta);        
        mainPanel.addItem("Train", cbTrain);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      backprop.setEta(Double.parseDouble(tfEta.getText()));
      backprop.setMu(Double.parseDouble(tfMu.getText()));
      backprop.setBiasEta(Double.parseDouble(tfBiasEta.getText()));      
      backprop.setTrain(cbTrain.isSelected());
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfEta = new JTextField("" + backprop.getEta());
        tfMu = new JTextField("" + backprop.getMu());
        tfBiasEta = new JTextField("" + backprop.getBiasEta());        
        cbTrain = new JCheckBox("", backprop.getTrain());
    }
}
