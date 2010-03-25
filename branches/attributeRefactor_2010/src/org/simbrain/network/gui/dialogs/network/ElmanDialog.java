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

import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.networks.Elman;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>ElmanDialog</b> is a dialog box for creating Elman networks.
 */
public class ElmanDialog extends StandardDialog {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Number of input units. */
    private JTextField numberOfInputUnits = new JTextField();

    /** Number of hidden units. */
    private JTextField numberOfHiddenUnits = new JTextField();

    /**
     * This method is the default constructor.
     * @param np Network panel
     */
    public ElmanDialog(final NetworkPanel np) {
        init();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        //Initialize Dialog
        setTitle("New Elman Network");

        fillFieldValues();

        numberOfHiddenUnits.setColumns(3);

        //Set up grapics panel
        mainPanel.addItem("Number of Input / Output Units", numberOfInputUnits);
        mainPanel.addItem("Number of Hidden Units", numberOfHiddenUnits);

        setContentPane(mainPanel);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        Elman el = new Elman();
        numberOfHiddenUnits.setText(Integer.toString(el.getNHidden()));
        numberOfInputUnits.setText(Integer.toString(el.getNInputs()));
    }

//    /**
//     * @return Number of inputs.
//     */
//    public int getNumInputs() {
//        return Integer.parseInt(numberOfInputUnits.getText());
//    }
//
//    /**
//     * @return Number hidden.
//     */
//    public int getNumHidden() {
//        return Integer.parseInt(numberOfHiddenUnits.getText());
//    }
}
