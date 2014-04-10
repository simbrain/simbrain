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
package org.simbrain.network.gui.trainer.subnetworkTrainingPanels;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.randomizer.RandomizerPanel;

/**
 * Panel displaying controls specific to reservoir computing.
 */
public class ReservoirUtilsPanel extends JPanel {

    // TODO: change to more general ReservoirNetwork, when LSMs are added
    /** The esn being set. */
    private EchoStateNetwork esn;

    /** The noise generator. */
    private Randomizer randomSource;

    /** A panel for setting random values. */
    private RandomizerPanel randomPanel = new RandomizerPanel();

    /** Enables noise. */
    private JButton enableNoise = new JButton();

    /** Flag for noise enabled/disabled */
    private boolean noiseEnabled;

    /** Commits changes made in the random panel. */
    private JButton noiseButton = new JButton("Apply");

    /** Constraints on the layout made accessible to all methods. */
    private GridBagConstraints gbc = new GridBagConstraints();

    /** The parent frame. */
    private GenericFrame frame;

    /**
     * The constructor, requiring both a reference to an esn and the parent
     * trainer panel.
     *
     * @param esn the esn where state noise is being injected.
     */
    public ReservoirUtilsPanel(EchoStateNetwork esn) {
        this.esn = esn;
        this.setLayout(new GridBagLayout());
        // this.setBorder(title);
        randomSource = esn.getNoiseGenerator();

        noiseEnabled = false;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        paintNoiseDisabled();
        addActionListeners();
        resetConstraints();
    }

    private void addActionListeners() {
        enableNoise.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                if (noiseEnabled) {
                    noiseEnabled = false;
                    paintNoiseDisabled();
                    frame.pack();
                } else {
                    noiseEnabled = true;
                    paintNoiseEnabled();
                    frame.pack();
                }

                esn.setNoise(noiseEnabled);

            }

        });

        noiseButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                randomPanel.commitRandom(randomSource);
            }

        });

    }

    /**
     * Paints the panel for the noise enabled state.
     */
    private void paintNoiseEnabled() {
        clearAll();
        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        this.add(new JLabel("Noise"), gbc);

        gbc.gridy = 1;
        gbc.gridx = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        enableNoise.setText("On");
        enableNoise.setForeground(Color.green);
        this.add(enableNoise, gbc);

        gbc.gridy = 1;
        gbc.gridx = 3;
        gbc.gridwidth = 2;
        gbc.gridheight = 6;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        randomPanel.fillFieldValues(randomSource);
        this.add(randomPanel, gbc);
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.gridy = 20;
        gbc.gridx = 4;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        this.add(noiseButton, gbc);
        this.repaint();
    }

    /**
     * Paints the panel for the noise disabled state.
     */
    private void paintNoiseDisabled() {
        clearAll();
        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        this.add(new JLabel("Noise"), gbc);

        enableNoise.setText("Off");
        enableNoise.setForeground(Color.red);
        gbc.gridy = 1;
        gbc.gridx = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        this.add(enableNoise, gbc);
        this.repaint();
    }

    /**
     * Clears everything from the panel.
     */
    private void clearAll() {
        this.removeAll();
        resetConstraints();
    }

    /**
     * Resets the gridbagconstraints on this panel to default, except that the
     * anchor is set to NORTHWEST.
     */
    private void resetConstraints() {
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
    }

    /**
     * @param frame the parentFrame to set
     */
    public void setFrame(GenericFrame frame) {
        this.frame = frame;
    }

}
