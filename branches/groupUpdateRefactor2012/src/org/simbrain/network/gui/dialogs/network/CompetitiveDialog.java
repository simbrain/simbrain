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
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.groups.Competitive;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.layout.AbstractLayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.GridLayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.LayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.LineLayoutPanel;
import org.simbrain.network.layouts.Layout;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>CompetitiveDialog</b> is used as an assistant to create competitive networks.
 *
 */
public class CompetitiveDialog extends StandardDialog implements ActionListener {
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

    /** Number of neurons field. */
    private JTextField tfNumNeurons = new JTextField();

    /** Epsilon field. */
    private JTextField tfEpsilon = new JTextField();

    /** Winner value field. */
    private JTextField tfWinnerValue = new JTextField();

    /** Loser value field. */
    private JTextField tfLoserValue = new JTextField();

    /** Leaky learning check box. */
    private JCheckBox cbUseLeakyLearning = new JCheckBox();

    /** Normalize inputs check box. */
    private JCheckBox cbNormalizeInputs = new JCheckBox();

    /** Leaky epsilon. */
    private JTextField tfLeakyEpsilon = new JTextField();

    /** Network Panel. */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     *
     * @param networkPanel Network panel
     */
    public CompetitiveDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        layoutPanel = new LayoutPanel(this, new AbstractLayoutPanel[]{new LineLayoutPanel(), new GridLayoutPanel()});
        init();
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        Layout layout = layoutPanel.getNeuronLayout();
        layout.setInitialLocation(networkPanel.getLastClickedPosition());
        Competitive competitive = new Competitive(networkPanel.getRootNetwork(), Integer.parseInt(tfNumNeurons.getText()), layout);
        competitive.setEpsilon(Double.parseDouble(tfEpsilon.getText()));
        competitive.setWinValue(Double.parseDouble(tfWinnerValue.getText()));
        competitive.setLoseValue(Double.parseDouble(tfLoserValue.getText()));
        competitive.setLeakyEpsilon(Double.parseDouble(tfLeakyEpsilon.getText()));
        competitive.setUseLeakyLearning(cbUseLeakyLearning.isSelected());
        competitive.setNormalizeInputs(cbNormalizeInputs.isSelected());
        networkPanel.getRootNetwork().addGroup(competitive);
        networkPanel.repaint();
        super.closeDialogOk();
    }

    /**
     * Initializes all components used in dialog.
     */
    private void init() {
        // Initializes dialog
        setTitle("New Competitive Network");

        cbUseLeakyLearning.addActionListener(this);
        cbUseLeakyLearning.setActionCommand("useLeakyLearning");

        fillFieldValues();
        checkLeakyEpsilon();

        tfNumNeurons.setColumns(5);

        // Set up logic panel
        logicPanel.addItem("Number of Neurons", tfNumNeurons);
        logicPanel.addItem("Winner Value", tfWinnerValue);
        logicPanel.addItem("Loser Value", tfLoserValue);
        logicPanel.addItem("Epsilon", tfEpsilon);
        logicPanel.addItem("Use leaky learning", cbUseLeakyLearning);
        logicPanel.addItem("Leaky Epsilon", tfLeakyEpsilon);
        logicPanel.addItem("Normalize inputs", cbNormalizeInputs);

        // Set up tab panels
        tabLogic.add(logicPanel);
        tabLayout.add(layoutPanel);
        tabbedPane.addTab("Logic", tabLogic);
        tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(tabbedPane);
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

    /**
     * Populate fields with current data.
     */
    private void fillFieldValues() {
        // REDO
        //Competitive ct = new Competitive();
        tfEpsilon.setText(Double.toString(.1));
        tfLoserValue.setText(Double.toString(0));
        tfNumNeurons.setText(Integer.toString(5));
        tfWinnerValue.setText(Double.toString(1));
        tfLeakyEpsilon.setText(Double.toString(.1/4));
        cbUseLeakyLearning.setSelected(false);
        cbNormalizeInputs.setSelected(true);
    }

}
