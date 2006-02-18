package org.simbrain.network.dialog.network;

import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.networks.Competitive;

/**
 * <b>CompetitivePropertiesDialog</b> is a dialog box for setting the properties of a competitive network.
 *
 */
public class CompetitivePropertiesDialog extends StandardDialog {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Epsilon value field. */
    private JTextField epsilon = new JTextField();

    /** Winner value field. */
    private JTextField winnerValue = new JTextField();

    /** Loser value field. */
    private JTextField loserValue = new JTextField();

    /** The model subnetwork. */
    private Competitive competitive;

    /**
     * Default constructor.
     *
     * @param competitive Competitive network being modified.
     */
    public CompetitivePropertiesDialog(final Competitive competitive) {
        this.competitive = competitive;
        setTitle("Set Competitive Properties");
        fillFieldValues();
        this.setLocation(500, 0); //Sets location of network dialog

        mainPanel.addItem("Epsilon", epsilon);
        mainPanel.addItem("Winner Value", winnerValue);
        mainPanel.addItem("Loser Value", loserValue);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      competitive.setEpsilon(Double.parseDouble(epsilon.getText()));
      competitive.setWinValue(Double.parseDouble(winnerValue.getText()));
      competitive.setLoseValue(Double.parseDouble(loserValue.getText()));
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        epsilon = new JTextField("" + competitive.getEpsilon());
        loserValue = new JTextField("" + competitive.getLoseValue());
        winnerValue = new JTextField("" + competitive.getWinValue());
    }
}
