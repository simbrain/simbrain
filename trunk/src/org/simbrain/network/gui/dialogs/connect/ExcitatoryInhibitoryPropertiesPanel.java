/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.connect;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.gui.dialogs.RandomPanel;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.util.RandomSource;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;

public class ExcitatoryInhibitoryPropertiesPanel extends JPanel {

    private final ConnectNeurons connection;

    /** Max ratio of excitatory/inhibitory connections. */
    private static final int RATIO_MAX = 100;

    /** Min ratio of excitatory/inhibitory connections. */
    private static final int RATIO_MIN = 0;

    /** Default starting ratio of excitatory/inhibitory. */
    private static final int RATIO_INIT = 50;

    /** A slider for setting the ratio of inhibitory to excitatory connections. */
    private JSlider ratioSlider = new JSlider(JSlider.HORIZONTAL, RATIO_MIN,
            RATIO_MAX, RATIO_INIT);

    /**
     * A text field for setting the ratio of excitatory to inhibitory
     * connections.
     */
    private JFormattedTextField tRatio = new JFormattedTextField(RATIO_INIT);

    /**
     * A button opening a menu to select the desired type of excitatory synapse.
     */
    private final JButton excitatorySynType = new JButton("ClampedSynapse");

    /**
     * A button opening a menu to select the desired type of inhibitory synapse.
     */
    private final JButton inhibitorySynType = new JButton("ClampedSynapse");

    /**
     * A random panel to set the range and distribution of excitatory strengths.
     */
    private RandomPanel exRandPanel = new RandomPanel(true);

    /**
     * A random panel to set the range and distribution of inhibitory strengths.
     */
    private RandomPanel inRandPanel = new RandomPanel(true);

    /** A button opening a menu to a random panel for excitatory connections. */
    private final JButton randExButton = new JButton();

    /** A button opening a menu to a random panel for inhibitory connections. */
    private final JButton randInButton = new JButton();

    /** A random source for inhibitory strengths. */
    private final RandomSource inhibRS = new RandomSource();

    /** A random source for excitatory strengths. */
    private final RandomSource exciteRS = new RandomSource();

    /**
     * A checkbox for selecting whether or not inhibitory strengths are to be
     * randomized.
     */
    private JCheckBox randInhib = new JCheckBox();

    /**
     * A checkbox for selecting whether or not excitatory strengths are to be
     * randomized.
     */
    private JCheckBox randExcite = new JCheckBox();

    public ExcitatoryInhibitoryPropertiesPanel(final ConnectNeurons connection) {
        this.connection = connection;
        fillFieldValues();
        initializeLayout();
    }

    private void initializeLayout() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 10, 0, 10);
        this.add(new JLabel("Full Inhibitory"), gbc);
        gbc.gridx = 2;
        this.add(new JLabel("Full Excitatory"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        this.add(ratioSlider, gbc);

        // tRatio.setMaximumSize(tRatioSize);
        // //the ratio text field gets its own panel to prevent distortion
        // JPanel tRatioPanel = new JPanel();
        // tRatioPanel.setLayout(new BorderLayout());
        // tRatioPanel.add(new JLabel("% Excitatory: "), BorderLayout.WEST);
        // tRatioPanel.add(tRatio, BorderLayout.CENTER);
        // JPanel tRatioContainer = new JPanel();
        // tRatioContainer.add(tRatioPanel);
        // this.add(tRatioContainer, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        this.add(new JLabel("% Excitatory: "), gbc);

        gbc.gridx = 1;
        Dimension tRatioSize = tRatio.getPreferredSize();
        tRatioSize.width = 40;
        tRatio.setPreferredSize(tRatioSize);
        this.add(tRatio, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        this.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 4;
        this.add(new JLabel("Excitatory Synapse Type: "), gbc);

        gbc.gridx = 1;
        this.add(excitatorySynType, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        this.add(new JLabel("Inhibitory Synapse Type: "), gbc);

        gbc.gridx = 1;
        this.add(inhibitorySynType, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        this.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 5, 0, 10);

        JPanel tempExRandPanel = new JPanel();
        FlowLayout RPFL = new FlowLayout(FlowLayout.LEFT);

        tempExRandPanel.setLayout(RPFL);
        tempExRandPanel.add(new JLabel("Randomize Excitatory Weights: "));
        tempExRandPanel.add(randExcite);
        tempExRandPanel.add(randExButton);

        JPanel tempInRandPanel = new JPanel();
        tempInRandPanel.setLayout(RPFL);
        tempInRandPanel.add(new JLabel("Randomize Inhibitory Weights: "));
        tempInRandPanel.add(randInhib);
        tempInRandPanel.add(randInButton);

        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;

        this.add(tempExRandPanel, gbc);

        gbc.gridy = 8;
        this.add(tempInRandPanel, gbc);

    }

    /**
     * Initializes the values of the GUI ratio slider.
     */
    private void initializeRatioSlider() {
        ratioSlider.setMajorTickSpacing(10);
        ratioSlider.setMinorTickSpacing(2);
        ratioSlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(new Integer(0), new JLabel("0/100"));
        labelTable.put(new Integer(25), new JLabel("25/75"));
        labelTable.put(new Integer(50), new JLabel("50/50"));
        labelTable.put(new Integer(75), new JLabel("75/25"));
        labelTable.put(new Integer(100), new JLabel("100/0"));
        ratioSlider.setLabelTable(labelTable);
        ratioSlider.setPaintLabels(true);
    }

    /**
     * Initializes the change listeners relating to the ratio text field and
     * ratio slider (reciprocal listeners).
     */
    private void initializeChangeListeners() {

        tRatio.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent arg0) {
                if (arg0.getSource() == tRatio) {
                    ratioSlider.setValue(((Number) tRatio.getValue())
                            .intValue());
                }
            }
        });

        ratioSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (!source.getValueIsAdjusting() && source == ratioSlider) {
                    tRatio.setValue(new Integer(ratioSlider.getValue()));
                }
            }

        });

    }

    /**
     * Initializes the action listeners relating to buttons and checkboxes.
     */
    private void initializeActionListeners() {

        excitatorySynType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ArrayList<Synapse> excitatoryList = new ArrayList<Synapse>();
                excitatoryList.add(connection.getBaseExcitatorySynapse());
                SynapseDialog dialog = new SynapseDialog(excitatoryList);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                Synapse excitatorySynapse = dialog.getSynapseList().get(0);
                connection.setBaseExcitatorySynapse(excitatorySynapse);
                excitatorySynType.setText(excitatorySynapse.getType());
            }

        });

        inhibitorySynType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ArrayList<Synapse> inhibitoryList = new ArrayList<Synapse>();
                inhibitoryList.add(connection.getBaseInhibitorySynapse());
                SynapseDialog dialog = new SynapseDialog(inhibitoryList);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                Synapse inhibitorySynapse = dialog.getSynapseList().get(0);
                connection.setBaseInhibitorySynapse(inhibitorySynapse);
                inhibitorySynType.setText(inhibitorySynapse.getType());
            }
        });

        randInhib.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getSource() == randInhib) {
                    if (randInhib.isSelected()) {
                        randInButton.setEnabled(true);
                        connection.setEnableInRand(true);
                    } else {
                        randInButton.setEnabled(false);
                        connection.setEnableInRand(false);
                    }
                }
            }
        });

        randExcite.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (arg0.getSource() == randExcite) {
                    if (randExcite.isSelected()) {
                        randExButton.setEnabled(true);
                        connection.setEnableExRand(true);
                    } else {
                        randExButton.setEnabled(false);
                        connection.setEnableExRand(false);
                    }
                }
            }
        });

        randExButton.addActionListener(new ActionListener() {

            @SuppressWarnings("serial")
            public void actionPerformed(ActionEvent arg0) {
                StandardDialog exRSD = new StandardDialog() {
                    @Override
                    protected void closeDialogOk() {
                        super.closeDialogOk();
                        exRandPanel.commitRandom(exciteRS);
                    }
                };
                exRSD.setContentPane(exRandPanel);
                exRSD.pack();
                exRSD.setLocationRelativeTo(null);
                exRSD.setVisible(true);
            }

        });

        randInButton.addActionListener(new ActionListener() {

            @SuppressWarnings("serial")
            public void actionPerformed(ActionEvent arg0) {
                StandardDialog inRSD = new StandardDialog() {
                    @Override
                    protected void closeDialogOk() {
                        super.closeDialogOk();
                        inRandPanel.commitRandom(inhibRS);
                    }
                };
                inRSD.setContentPane(inRandPanel);
                inRSD.pack();
                inRSD.setLocationRelativeTo(null);
                inRSD.setVisible(true);
            }

        });
    }

    public void commitChanges() {
        connection.setPercentExcitatory(((Number) tRatio.getValue())
                .doubleValue());
        connection.setEnableInRand(randInhib.isSelected());
        connection.setEnableExRand(randExcite.isSelected());
        if (randInhib.isSelected()) {
            connection.setInhibitoryRand(inhibRS);
        }
        if (randExcite.isSelected()) {
            connection.setExcitatoryRand(exciteRS);
        }
    }

    public void fillFieldValues() {
        Synapse e = Synapse.getTemplateSynapse(excitatorySynType.getText());
        connection.setBaseExcitatorySynapse(e);
        Synapse i = Synapse.getTemplateSynapse(inhibitorySynType.getText());
        connection.setBaseInhibitorySynapse(i);
        inRandPanel.fillDefaultValues();
        exRandPanel.fillDefaultValues();
        tRatio.setValue(((Number) (connection.getPercentExcitatory()))
                .intValue());
        initializeRatioSlider();
        initializeChangeListeners();
        initializeActionListeners();
        randExButton.setIcon(ResourceManager.getImageIcon("ExRand.png"));
        randInButton.setIcon(ResourceManager.getImageIcon("InRand.png"));
        randExButton.setEnabled(false);
        randInButton.setEnabled(false);

    }

    public JSlider getRatioSlider() {
        return ratioSlider;
    }

    public void setRatioSlider(JSlider ratioSlider) {
        this.ratioSlider = ratioSlider;
    }

    public JFormattedTextField gettRatio() {
        return tRatio;
    }

    public void settRatio(JFormattedTextField tRatio) {
        this.tRatio = tRatio;
    }

    public RandomPanel getExRandPanel() {
        return exRandPanel;
    }

    public void setExRandPanel(RandomPanel exRandPanel) {
        this.exRandPanel = exRandPanel;
    }

    public RandomPanel getInRandPanel() {
        return inRandPanel;
    }

    public void setInRandPanel(RandomPanel inRandPanel) {
        this.inRandPanel = inRandPanel;
    }

    public JCheckBox getRandInhib() {
        return randInhib;
    }

    public void setRandInhib(JCheckBox randInhib) {
        this.randInhib = randInhib;
    }

    public JCheckBox getRandExcite() {
        return randExcite;
    }

    public void setRandExcite(JCheckBox randExcite) {
        this.randExcite = randExcite;
    }

    public static int getRatioMax() {
        return RATIO_MAX;
    }

    public static int getRatioMin() {
        return RATIO_MIN;
    }

    public static int getRatioInit() {
        return RATIO_INIT;
    }

    public JButton getExcitatorySynType() {
        return excitatorySynType;
    }

    public JButton getInhibitorySynType() {
        return inhibitorySynType;
    }

    public JButton getRandExButton() {
        return randExButton;
    }

    public JButton getRandInButton() {
        return randInButton;
    }

    public RandomSource getInhibRS() {
        return inhibRS;
    }

    public RandomSource getExciteRS() {
        return exciteRS;
    }

}
