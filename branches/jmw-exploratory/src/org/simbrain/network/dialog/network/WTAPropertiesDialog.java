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
package org.simbrain.network.dialog.network;

import javax.swing.JTextField;
import javax.swing.JButton;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.networks.WinnerTakeAll;
import org.simbrain.network.actions.ShowHelpAction;

/**
 * <b>WTAPropertiesDialog</b> is a dialog box for setting the properties of a winner take all network.
 */
public class WTAPropertiesDialog extends StandardDialog {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Winner value field. */
    private JTextField winnerValue = new JTextField();

    /** Loser value field. */
    private JTextField loserValue = new JTextField();

    /** The model subnetwork. */
    private WinnerTakeAll wta;

    /** Help Button. */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. */
    private ShowHelpAction helpAction = new ShowHelpAction();

    /**
     * Default constructor.
     *
     * @param wta WinnerTakeAll network being modified.
     */
    public WTAPropertiesDialog(final WinnerTakeAll wta) {
        this.wta = wta;
        setTitle("Set WTA Properties");
        fillFieldValues();
        this.setLocation(500, 0); //Sets location of network dialog
        helpAction.setTheURL("Network/network/winnerTakeAll.html");
        helpButton.setAction(helpAction);

        this.addButton(helpButton);
        mainPanel.addItem("Winner Value", winnerValue);
        mainPanel.addItem("Loser Value", loserValue);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      wta.setWinValue(Double.parseDouble(winnerValue.getText()));
      wta.setLoseValue(Double.parseDouble(loserValue.getText()));
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        loserValue = new JTextField("" + wta.getLoseValue());
        winnerValue = new JTextField("" + wta.getWinValue());
    }

}
