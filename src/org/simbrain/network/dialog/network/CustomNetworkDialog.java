/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComboBox;


/**
 * <b>CustomNetworkDialog</b> is a dialog box for creating custom networks.
 */
public class CustomNetworkDialog extends StandardDialog implements ActionListener {
    private Box mainPanel = Box.createVerticalBox();
    private LabelledItemPanel topPanel = new LabelledItemPanel();
    private AbstractNetworkPanel networkPanel = new LayeredNetworkPanel();
    private String[] tempList = { "Layered", "Ring" };
    private JComboBox cbNetworkType = new JComboBox(tempList);

    /**
     * This method is the default constructor.
     */
    public CustomNetworkDialog() {
        init();
    }

    /**
     * Initialises the components on the panel.
     */
    private void init() {
        setTitle("Custom Nework Dialog");
        this.setLocation(500, 0); //Sets location of network dialog		

        networkPanel.fillFieldValues();

        cbNetworkType.addActionListener(this);
        topPanel.addItem("Network type", cbNetworkType);
        mainPanel.add(topPanel);
        mainPanel.add(networkPanel);
        setContentPane(mainPanel);
    }

    /**
     * Respond to neuron type changes
     */
    public void actionPerformed(ActionEvent e) {
        if (cbNetworkType.getSelectedItem().equals("Layered")) {
            mainPanel.remove(networkPanel);
            networkPanel = new LayeredNetworkPanel();
            networkPanel.fillFieldValues();
            mainPanel.add(networkPanel);
        } else if (cbNetworkType.getSelectedItem().equals("Ring")) {
            mainPanel.remove(networkPanel);
            networkPanel = new RingNetworkPanel();
            networkPanel.fillFieldValues();
            mainPanel.add(networkPanel);
        } //Something different for mixed panel... 

        pack();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commmitChanges() {
        networkPanel.commitChanges();
    }
}
