package org.simbrain.network.dialog.network;

import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.networks.LMSNetwork;

/**
 * <b>LMSPropertiesDialog</b> is a dialog box for setting the properties of a LMSNetwork network.
 *
 */
public class LMSPropertiesDialog extends StandardDialog {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Learning rate value field. */
    private JTextField tfEta = new JTextField();

    /** The model subnetwork. */
    private LMSNetwork lms;

    /**
     * Default constructor.
     *
     * @param lms LMSNetwork network being modified.
     */
    public LMSPropertiesDialog(final LMSNetwork lms) {
        this.lms = lms;
        setTitle("Set LMS Network Properties");
        fillFieldValues();
        this.setLocation(500, 0); //Sets location of network dialog

        mainPanel.addItem("Learning Rate", tfEta);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      lms.setEta(Double.parseDouble(tfEta.getText()));
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfEta = new JTextField("" + lms.getEta());
    }
}
