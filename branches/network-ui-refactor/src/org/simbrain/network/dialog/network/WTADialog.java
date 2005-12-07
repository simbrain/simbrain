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

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>WTADialog</b> is a dialog box for setting the properties of the  Network GUI.
 */
public class WTADialog extends StandardDialog {
    private JTabbedPane tabbedPane = new JTabbedPane();
    private JPanel tabLogic = new JPanel();
    private JPanel tabLayout = new JPanel();
    private LabelledItemPanel logicPanel = new LabelledItemPanel();
    private LayoutPanel layoutPanel = new LayoutPanel();
    private JTextField numberOfUnits = new JTextField();
    private JTextField winnerValue = new JTextField();
    private JTextField loserValue = new JTextField();
    NetworkPanel thePanel;

    /**
     * This method is the default constructor.
     */
    public WTADialog(final NetworkPanel np) {
        thePanel = np;
        init();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        //Initialize Dialog
        setTitle("New WTA Network");
        fillFieldValues();
        this.setLocation(500, 0); //Sets location of network dialog
        layoutPanel.setCurrentLayout(LayoutPanel.LINE);

        //Set up logic panel
        logicPanel.addItem("Number of Units", numberOfUnits);
        logicPanel.addItem("Winner Value", winnerValue);
        logicPanel.addItem("Loser Value", loserValue);

        //Set up tab panels
        tabLogic.add(logicPanel);
        tabLayout.add(layoutPanel);
        tabbedPane.addTab("Logic", logicPanel);
        tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(tabbedPane);
    }

    /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
        numberOfUnits.setText("" + 10);
    }

    /**
     * Set projector values based on fields
     */
    public void getValues() {
    }

    public int getNumUnits() {
        return Integer.parseInt(numberOfUnits.getText());
    }

    public String getCurrentLayout() {
        return layoutPanel.getCurrentLayout();
    }
}
