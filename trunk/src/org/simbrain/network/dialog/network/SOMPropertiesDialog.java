package org.simbrain.network.dialog.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.networks.SOM;

/**
 * <b>SOMPropertiesDialog</b> is a dialog box for setting the properties of a SOM.
 *
 */
public class SOMPropertiesDialog extends StandardDialog implements ActionListener {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** InitAlpha value field. */
    private JTextField tfAlpha = new JTextField();

    /** InitNeighborhoodSize value field. */
    private JTextField tfInitNeighborhoodSize = new JTextField();

    /** NumInputVectors value field. */
    private JTextField tfNumInputVectors = new JTextField();

    /** RecallMode check box. */
    private JCheckBox cbRecallMode = new JCheckBox();

    /** Information on current SOM */

    /** The model subnetwork. */
    private SOM SOM;

    /**
     * Default constructor.
     *
     * @param SOM network being modified.
     */
    public SOMPropertiesDialog(final SOM SOM) {
        this.SOM = SOM;
        setTitle("Set SOM Properties");

        fillFieldValues();

        mainPanel.addItem("Initial Learning Rate", tfAlpha);
        mainPanel.addItem("Initial Neighborhood Size", tfInitNeighborhoodSize);
        mainPanel.addItem("Total Input Vectors", tfNumInputVectors);
        mainPanel.addItem("Recall Mode", cbRecallMode);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      SOM.setInitAlpha(Double.parseDouble(tfAlpha.getText()));
      SOM.setInitNeighborhoodSize(Double.parseDouble(tfInitNeighborhoodSize.getText()));
      SOM.setNumInputVectors(Integer.parseInt(tfNumInputVectors.getText()));
      SOM.setRecallMode(cbRecallMode.isSelected());
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfAlpha.setText(Double.toString(SOM.getInitAlpha()));
        tfInitNeighborhoodSize.setText(Double.toString(SOM.getInitNeighborhoodSize()));
        tfNumInputVectors.setText(Integer.toString(SOM.getNumInputVectors()));
        cbRecallMode.setSelected(SOM.isRecallMode());
    }
    
    public void actionPerformed(final ActionEvent e) {
    }

}
