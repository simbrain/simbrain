package org.simbrain.network.gui.dialogs.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.simbrain.network.subnetworks.Competitive;
import org.simbrain.network.gui.actions.ShowHelpAction;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>CompetitivePropertiesDialog</b> is a dialog box for setting the properties of a competitive network.
 *
 */
public class CompetitivePropertiesDialog extends StandardDialog implements ActionListener {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Epsilon value field. */
    private JTextField tfEpsilon = new JTextField();

    /** Winner value field. */
    private JTextField tfWinnerValue = new JTextField();

    /** Loser value field. */
    private JTextField tfLoserValue = new JTextField();

    /** Leaky epsilon value. */
    private JTextField tfLeakyEpsilon = new JTextField();

    /** Leaky learning check box. */
    private JCheckBox cbUseLeakyLearning = new JCheckBox();

    /** Normalize inputs check box. */
    private JCheckBox cbNormalizeInputs = new JCheckBox();

    /** The model subnetwork. */
    private Competitive competitive;

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction = new ShowHelpAction();

    /**
     * Default constructor.
     *
     * @param competitive Competitive network being modified.
     */
    public CompetitivePropertiesDialog(final Competitive competitive) {
        this.competitive = competitive;
        setTitle("Set Competitive Properties");

        cbUseLeakyLearning.addActionListener(this);
        cbUseLeakyLearning.setActionCommand("useLeakyLearning");

        fillFieldValues();
        checkLeakyEpsilon();
        helpAction.setTheURL("Network/network/competitivenetwork.html");
        helpButton.setAction(helpAction);

        this.addButton(helpButton);
        mainPanel.addItem("Epsilon", tfEpsilon);
        mainPanel.addItem("Winner Value", tfWinnerValue);
        mainPanel.addItem("Loser Value", tfLoserValue);
        mainPanel.addItem("Use Leaky Learning", cbUseLeakyLearning);
        mainPanel.addItem("Leaky Epsilon", tfLeakyEpsilon);
        mainPanel.addItem("Normalize Inputs", cbNormalizeInputs);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      competitive.setEpsilon(Double.parseDouble(tfEpsilon.getText()));
      competitive.setWinValue(Double.parseDouble(tfWinnerValue.getText()));
      competitive.setLoseValue(Double.parseDouble(tfLoserValue.getText()));
      competitive.setLeakyEpsilon(Double.parseDouble(tfLeakyEpsilon.getText()));
      competitive.setUseLeakyLearning(cbUseLeakyLearning.isSelected());
      competitive.setNormalizeInputs(cbNormalizeInputs.isSelected());
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfEpsilon.setText(Double.toString(competitive.getEpsilon()));
        tfLoserValue.setText(Double.toString(competitive.getLoseValue()));
        tfWinnerValue.setText(Double.toString(competitive.getWinValue()));
        tfLeakyEpsilon.setText(Double.toString(competitive.getLeakyEpsilon()));
        cbUseLeakyLearning.setSelected(competitive.getUseLeakyLearning());
        cbNormalizeInputs.setSelected(competitive.getNormalizeInputs());
    }

    /**
     * @see java.awt.event.ActionListener
     */
    public void actionPerformed(final ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("useLeakyLearning")) {
            checkLeakyEpsilon();
        }

    }

    /**
     * Checks whether or not to enable leaky epsilon.
     */
    private void checkLeakyEpsilon() {
        if (cbUseLeakyLearning.isSelected()) {
            tfLeakyEpsilon.setEnabled(true);
        } else {
            tfLeakyEpsilon.setEnabled(false);
        }
    }
}
