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

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.layout.AbstractLayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.GridLayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.HexagonalGridLayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.LayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.LineLayoutPanel;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.networks.WinnerTakeAll;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>WTADialog</b> is a dialog box for setting the properties of the  Network GUI.
 */
public class WTADialog extends StandardDialog {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Logic tab panel. */
    private JPanel tabLogic = new JPanel();

    /** Layout tab panel. */
    private JPanel tabLayout = new JPanel();

    /** Logic panel. */
    private LabelledItemPanel logicPanel = new LabelledItemPanel();

    /** Layout panel. */
    private LayoutPanel layoutPanel;

    /** Number of units field. */
    private JTextField numberOfUnits = new JTextField();

    /** Winner value field. */
    private JTextField winnerValue = new JTextField();

    /** Loser value field. */
    private JTextField loserValue = new JTextField();

    /** Network panel. */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     * @param np Network panel
     */
    public WTADialog(final NetworkPanel np) {
        networkPanel = np;
        layoutPanel = new LayoutPanel(this, new AbstractLayoutPanel[]{new LineLayoutPanel(),
                           new HexagonalGridLayoutPanel(), new GridLayoutPanel()});
        init();
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      Layout layout = layoutPanel.getNeuronLayout();
      layout.setInitialLocation(networkPanel.getLastClickedPosition());
      WinnerTakeAll wta = new WinnerTakeAll(networkPanel.getRootNetwork(), Integer.parseInt(numberOfUnits.getText()), layout);
      wta.setWinValue(Double.parseDouble(winnerValue.getText()));
      wta.setLoseValue(Double.parseDouble(loserValue.getText()));
      networkPanel.getRootNetwork().addNetwork(wta);
      networkPanel.repaint();
      super.closeDialogOk();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        //Initialize Dialog
        setTitle("New WTA Network");

        fillFieldValues();

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
     * Populate fields with current data.
     */
    private void fillFieldValues() {
        WinnerTakeAll wta = new WinnerTakeAll();
        loserValue.setText(Double.toString(wta.getLoseValue()));
        numberOfUnits.setText(Integer.toString(wta.getNumUnits()));
        winnerValue.setText(Double.toString(wta.getWinValue()));
    }
}
