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

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.groups.Group;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.GroupPropertiesPanel;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.widgets.CommittablePanel;

/**
 * <b>WTAPropertiesDialog</b> is a dialog box for setting the properties of a
 * winner take all network.
 */
public class WTAPropertiesPanel extends JPanel implements
    GroupPropertiesPanel, CommittablePanel {

    /** Default number of neurons. */
    private static final int DEFAULT_NUM_NEURONS = 5;

    /** Parent Network Panel. */
    private NetworkPanel networkPanel;

    /** Number of neurons field. */
    private JTextField tfNumNeurons = new JTextField();

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

    /** If true this is a creation panel.  Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Default constructor.
     *
     * @param np parent network panel
     */
    public WTAPropertiesPanel(final NetworkPanel np) {
        this.networkPanel = np;
        isCreationPanel = true;
        mainPanel.addItem("Number of neurons", tfNumNeurons);
        initPanel();
    }

    /**
     * Default constructor.
     *
     * @param np parent network panel
     * @param wta WinnerTakeAll network being modified.
     */
    public WTAPropertiesPanel(final NetworkPanel np, final WinnerTakeAll wta) {
        this.wta = wta;
        isCreationPanel = false;
        initPanel();
    }


    /**
     * Initialize the panel.
     */
    private void initPanel() {

        fillFieldValues();

        mainPanel.addItem("Winner Value", winnerValue);
        mainPanel.addItem("Loser Value", loserValue);
        mainPanel.addItem("Set winner randomly (with some probability)",
                useRandomBox);
        mainPanel
        .addItem("Probability of choosing a random winner", randomProb);

        // Enable / disable random prob box based on state of use random
        // checkbox
        randomProb.setEnabled(useRandomBox.isSelected());
        useRandomBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                randomProb.setEnabled(useRandomBox.isSelected());
            }
        });
        add(mainPanel);
    }

    @Override
    public boolean commitChanges() {
        try {
            if (isCreationPanel) {
                wta = new WinnerTakeAll(networkPanel.getNetwork(),
                        Integer.parseInt(tfNumNeurons.getText()));
            }
            wta.setWinValue(Double.parseDouble(winnerValue.getText()));
            wta.setLoseValue(Double.parseDouble(loserValue.getText()));
            wta.setUseRandom(useRandomBox.isSelected());
            wta.setRandomProb(Double.parseDouble(randomProb.getText()));
        } catch (NumberFormatException nfe) {
            return false; // Failure
        }
        return true; // Success
    }

    @Override
    public void fillFieldValues() {
        // For creation panels use an "empty" competitive network to harvest
        // default values
        if (isCreationPanel) {
            wta = new WinnerTakeAll(null, 1);
            tfNumNeurons.setText("" + DEFAULT_NUM_NEURONS);
        }
        loserValue.setText("" + wta.getLoseValue());
        winnerValue.setText("" + wta.getWinValue());
        useRandomBox.setSelected(wta.isUseRandom());
        randomProb.setText("" + wta.getRandomProb());
    }

    @Override
    public String getHelpPath() {
        return "Pages/Network/network/winnerTakeAll.html";
    }

    @Override
    public Group getGroup() {
        return wta;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getPanel() {
        return this;
    }

}
