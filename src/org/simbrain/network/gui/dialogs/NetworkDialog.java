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
package org.simbrain.network.gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.QuickConnectPreferencesPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.randomizer.gui.RandomizerPanel;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * <b>NetworkDialog</b> is a dialog box for setting the properties of the
 * Network GUI. If the user presses ok, values become default values. Restore
 * defaults restores to original values. When canceling out the values prior to
 * making any changes are restored.
 */
public class NetworkDialog extends StandardDialog {

    /** Network panel. */
    protected NetworkPanel networkPanel;

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main properties panel. */
    protected NetworkPropertiesPanel networkPropertiesPanel;

    /** Connection preferences panel. */
    private QuickConnectPreferencesPanel quickConnectPanel;

    /** Random panel. */
    private NetworkRandomizerPanel randomPanel;

    /**
     * This method is the default constructor.
     *
     * @param np reference to <code>NetworkPanel</code>.
     */
    public NetworkDialog(final NetworkPanel np) {
        networkPanel = np;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {

        // Initialize Dialog
        setTitle("Network Dialog");

        // Main properties tab
        networkPropertiesPanel = new NetworkPropertiesPanel(networkPanel);
        tabbedPane.addTab("Main", networkPropertiesPanel);

        // Quick-connect properties
        quickConnectPanel = new QuickConnectPreferencesPanel(networkPanel, this);
        tabbedPane.addTab("Connections", quickConnectPanel);

        // Randomizer properties
        randomPanel = new NetworkRandomizerPanel();
        // randomPanel.fillFieldValues(networkPanel.getNetwork()
        // .getWeightRandomizer());
        tabbedPane.addTab("Randomizer", randomPanel);

        // Set main panel
        setContentPane(tabbedPane);

        // Add help button
        JButton helpButton = new JButton("Help");
        ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Network/network_prefs.html");
        helpButton.setAction(helpAction);
        this.addButton(helpButton);
    }

    /**
     * Commits changes not handled in action performed.
     */
    private void commitChanges() {
        networkPropertiesPanel.commitChanges();
        quickConnectPanel.commitChanges();
        randomPanel.commitChanges();
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Panel for selecting which network randomizer to edit.
     */
    private class NetworkRandomizerPanel extends JPanel {

        /** Selects which randomizer to edit. */
        private JComboBox comboBox;

        /** Panel which holds the randomizer panel. */
        private JPanel randomizerHolder = new JPanel();

        /** The activation randomizer panel. */
        private RandomizerPanel activationRandomizer;

        /** The weight randomizer panel. */
        private RandomizerPanel weightRandomizer;

        /**
         * Construct the panel.
         */
        public NetworkRandomizerPanel() {
            Box  mainPanel = Box.createVerticalBox();
            mainPanel.add(Box.createVerticalStrut(10));
            comboBox = new JComboBox(new String[] { "Activation Randomizer",
                    "Weight Randomizer" });
            comboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    initPanel();
                }
            });

            activationRandomizer = new RandomizerPanel();
            activationRandomizer.fillFieldValues(networkPanel.getNetwork()
                    .getActivationRandomizer());
            weightRandomizer = new RandomizerPanel();
            weightRandomizer.fillFieldValues(networkPanel.getNetwork()
                    .getWeightRandomizer());
            initPanel();

            mainPanel.add(comboBox);
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(new JSeparator(JSeparator.HORIZONTAL));
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(randomizerHolder);

            add(mainPanel);

        }

        /**
         * Re-initialize the panel every time the combo box is changed.
         */
        private void initPanel() {
            randomizerHolder.removeAll();
            if (comboBox.getSelectedIndex() == 0) {
                randomizerHolder.add(activationRandomizer);
            } else {
                randomizerHolder.add(weightRandomizer);
            }
            repaint();
            pack();
        }

        /**
         * Apply changes to randomizer when parent dialogs closed.
         */
        public void commitChanges() {
            activationRandomizer.commitRandom(networkPanel.getNetwork()
                    .getActivationRandomizer());
            weightRandomizer.commitRandom(networkPanel.getNetwork()
                    .getWeightRandomizer());
        }

    }
}
