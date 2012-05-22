/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.simbrain.network.networks.WinnerTakeAll;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;

/**
 * <b>WTAPropertiesDialog</b> is a dialog box for setting the properties of a
 * winner take all network.
 */
public class WTAPropertiesDialog extends StandardDialog {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Winner value field. */
    private JTextField winnerValue = new JTextField();

    /** Loser value field. */
    private JTextField loserValue = new JTextField();

    /** Checkbox for using random method. */
    private JCheckBox useRandomBox = new JCheckBox();

    /** Probability of using random field. */
    private JTextField randomProb = new JTextField();

    /** The model subnetwork. */
    private WinnerTakeAll wta;

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction;

    /**
     * Default constructor.
     *
     * @param wta WinnerTakeAll network being modified.
     */
    public WTAPropertiesDialog(final WinnerTakeAll wta) {
        this.wta = wta;
        setTitle("Set WTA Properties");
        fillFieldValues();
        this.setLocation(500, 0); // Sets location of network dialog
        helpAction = new ShowHelpAction("Pages/Network/network/winnerTakeAll.html");
        helpButton.setAction(helpAction);

        this.addButton(helpButton);
        mainPanel.addItem("Winner Value", winnerValue);
        mainPanel.addItem("Loser Value", loserValue);
        mainPanel.addItem("Set winner randomly (with some probability)", useRandomBox);
        mainPanel.addItem("Probability of choosing a random winner", randomProb);
        setContentPane(mainPanel);

        // Enable / disable random prob box based on state of use random
        // checkbox
        randomProb.setEnabled(useRandomBox.isSelected());
        useRandomBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                randomProb.setEnabled(useRandomBox.isSelected());
            }
        });
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        wta.setWinValue(Double.parseDouble(winnerValue.getText()));
        wta.setLoseValue(Double.parseDouble(loserValue.getText()));
        wta.setUseRandom(useRandomBox.isSelected());
        wta.setRandomProb(Double.parseDouble(randomProb.getText()));
        super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        loserValue.setText("" + wta.getLoseValue());
        winnerValue.setText("" + wta.getWinValue());
        useRandomBox.setSelected(wta.isUseRandom());
        randomProb.setText("" + wta.getRandomProb());
    }

}
