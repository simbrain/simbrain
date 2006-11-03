package org.simbrain.network.dialog.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.simbrain.network.actions.ShowHelpAction;
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

    /** AlphaDecayRate value field. */
    private JTextField tfAlphaDecayRate = new JTextField();

    /** NeighborhoodDecayAmount value field. */
    private JTextField tfNeigborhoodDecayAmount = new JTextField();

    /** Current Learning Rate. */
    private JLabel lLearningRate = new JLabel();

    /** Current Neighborhood Size. */
    private JLabel lNeighborhoodSize = new JLabel();

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction = new ShowHelpAction();

    /** Information on current SOM */

    /** The model subnetwork. */
    private SOM som;

    /**
     * Default constructor.
     *
     * @param som network being modified.
     */
    public SOMPropertiesDialog(final SOM som) {
        this.som = som;
        setTitle("Set SOM Properties");

        fillFieldValues();
        helpAction.setTheURL("Network/network/som.html");
        helpButton.setAction(helpAction);

        this.addButton(helpButton);
        mainPanel.addItem("Initial Learning Rate", tfAlpha);
        mainPanel.addItem("Initial Neighborhood Size", tfInitNeighborhoodSize);
        mainPanel.addItem("Learning Decay Rate", tfAlphaDecayRate);
        mainPanel.addItem("Neighborhood Decay Amount", tfNeigborhoodDecayAmount);
        mainPanel.addItem("Learning Rate", lLearningRate);
        mainPanel.addItem("Neighborhood Size", lNeighborhoodSize);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      som.setInitAlpha(Double.parseDouble(tfAlpha.getText()));
      som.setInitNeighborhoodSize(Double.parseDouble(tfInitNeighborhoodSize.getText()));
      som.setAlphaDecayRate(Double.parseDouble(tfAlphaDecayRate.getText()));
      som.setNeighborhoodDecayAmount(Integer.parseInt(tfNeigborhoodDecayAmount.getText()));
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfAlpha.setText(Double.toString(som.getInitAlpha()));
        tfInitNeighborhoodSize.setText(Double.toString(som.getInitNeighborhoodSize()));
        tfAlphaDecayRate.setText(Double.toString(som.getAlphaDecayRate()));
        tfNeigborhoodDecayAmount.setText(Integer.toString(som.getNeighborhoodDecayAmount()));
        lLearningRate.setText(Double.toString(som.getAlpha()));
        lNeighborhoodSize.setText(Double.toString(som.getNeighborhoodSize()));
    }

    public void actionPerformed(final ActionEvent e) {
    }

}
