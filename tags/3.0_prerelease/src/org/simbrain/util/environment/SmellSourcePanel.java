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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA   02111-1307, USA.
 */
package org.simbrain.util.environment;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.environment.SmellSource.DecayFunction;

/**
 * <b>PanelStimulus</b> is a panel used to adjust the "smell signatures" (arrays
 * of doubles representing the effect an object has on the input nodes of the
 * network of non-creature entities in the world.
 */
public class SmellSourcePanel extends LabelledItemPanel implements
        ActionListener {

    /** Smell source. */
    private SmellSource smellSource;

    /** Value array. */
    private double[] valArray = null;

    /** Random number generator upper limit. */
    private double randomUpper;

    /** Random number generator lower limit. */
    private double randomLower;

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Values panel. */
    private LabelledItemPanel valuesPanel = new LabelledItemPanel();

    /** Dispersion panel. */
    private LabelledItemPanel dispersionPanel = new LabelledItemPanel();

    /** Stimulus values field. */
    private JTextField[] stimulusVals;

    /** Number of stimulus field. */
    private JTextField tfStimulusNum = new JTextField();

    /** Change number of stimulus button. */
    private JButton stimulusButton = new JButton("Change");

    /** Random number upper limit field. */
    private JTextField tfRandomUpper = new JTextField();

    /** Random number lower limit field. */
    private JTextField tfRandomLower = new JTextField();

    /** Upper label. */
    private JLabel upperLabel = new JLabel("Upper: ");

    /** Lower label. */
    private JLabel lowerLabel = new JLabel("Lower: ");

    /** Randomize button. */
    private JButton randomizeButton = new JButton("Randomize");

    /** Stimulus panel. */
    private JPanel addStimulusPanel = new JPanel();

    /** Grid bag layout constraints. */
    private GridBagConstraints con = new GridBagConstraints();

    /** Random sub panel upper. */
    private JPanel randomSubPanelUpper = new JPanel();

    /** Random sub panel lower. */
    private JPanel randomSubPanelLower = new JPanel();

    /** Random main panel. */
    private JPanel randomMainPanel = new JPanel();

    /** Stimulus panel. */
    private JPanel stimulusPanel = new JPanel();

    /** Stimulus scroller. */
    private JScrollPane stimScroller = new JScrollPane(stimulusPanel);

    /** Peak field. */
    private JTextField tfPeak = new JTextField();

    /** Decay function combo box. */
    private JComboBox cbDecayFunction = new JComboBox(
            SmellSource.DecayFunction.values());

    /** Dispersion field. */
    private JTextField tfDispersion = new JTextField();

    /** Maximum size. */
    private final int maxSize = 100;

    /** Noise level slider. */
    private JSlider jsNoiseLevel = new JSlider(0, maxSize, maxSize / 2);

    /** Add noise radio button. */
    private JRadioButton rbAddNoise = new JRadioButton();

    /**
     * Create and populate the stimulus panel.
     *
     * @param we reference to the world entity whose smell signature is being
     *            adjusted.
     */
    public SmellSourcePanel(final SmellSource source) {
        smellSource = source;

        final Dimension initDim = new Dimension(100, 125);

        // Handle stimulus scroller
        valArray = smellSource.getStimulusVector();
        stimulusVals = new JTextField[valArray.length];
        stimulusPanel.setLayout(new GridBagLayout());
        con.fill = GridBagConstraints.HORIZONTAL;
        stimScroller.setPreferredSize(initDim);

        final int initCol = 5;

        // Add Stimulus text field and button
        tfStimulusNum.setColumns(initCol);
        addStimulusPanel.add(tfStimulusNum);
        addStimulusPanel.add(stimulusButton);

        final int initTFCol = 3;

        // Add randomize stimulus text field and button
        tfRandomUpper.setColumns(initTFCol);
        tfRandomLower.setColumns(initTFCol);

        randomSubPanelUpper.setLayout(new FlowLayout());
        randomSubPanelUpper.add(lowerLabel);
        randomSubPanelUpper.add(tfRandomLower);
        randomSubPanelUpper.add(upperLabel);
        randomSubPanelUpper.add(tfRandomUpper);
        randomSubPanelLower.setLayout(new FlowLayout());
        randomSubPanelLower.add(randomizeButton);
        randomMainPanel.setLayout(new BorderLayout());
        randomMainPanel.add(randomSubPanelUpper, BorderLayout.NORTH);
        randomMainPanel.add(randomSubPanelLower, BorderLayout.SOUTH);

        final int majorTickSpacing = 25;

        // Turn on labels at major tick marks.
        jsNoiseLevel.setMajorTickSpacing(majorTickSpacing);
        jsNoiseLevel.setPaintTicks(true);
        jsNoiseLevel.setPaintLabels(true);

        rbAddNoise.addActionListener(this);
        stimulusButton.setActionCommand("addStimulus");
        stimulusButton.addActionListener(this);
        randomizeButton.setActionCommand("randomize");
        randomizeButton.addActionListener(this);

        fillFieldValues();

        this.add(tabbedPane);
        dispersionPanel.addItem("Decay function", cbDecayFunction);
        dispersionPanel.addItem("Dispersion", tfDispersion);
        tfPeak.setToolTipText("How far (in pixels) the smell disperses.");
        dispersionPanel.addItem("Peak distance", tfPeak);
        tfPeak.setToolTipText("Distance at which smell has peak value.");
        dispersionPanel.addItem("Add noise", rbAddNoise);
        dispersionPanel.addItem("Noise level", jsNoiseLevel);
        valuesPanel.addItem("Stimulus dimensions", addStimulusPanel);
        valuesPanel.addItem("Stimulus values", stimScroller);
        valuesPanel.addItem("Randomize stimulus", randomMainPanel);
        tabbedPane.addTab("Stimulus Values", valuesPanel);
        tabbedPane.addTab("Stimulus Dispersion", dispersionPanel);
    }

    /**
     * Populate fields with current data.
     */
    private void fillFieldValues() {
        cbDecayFunction.setSelectedItem(smellSource.getDecayFunction());
        tfDispersion.setText(Double.toString(smellSource.getDispersion()));
        tfPeak.setText(Double.toString(smellSource.getPeak()));

        updateStimulusPanel();

        rbAddNoise.setSelected(smellSource.isAddNoise());
        jsNoiseLevel.setValue((int) (smellSource.getNoiseLevel() * 100));
        if (smellSource.isAddNoise()) {
            jsNoiseLevel.setEnabled(true);
        } else {
            jsNoiseLevel.setEnabled(false);
        }

        // Sets initial upper and lower randomizer bounds to current rounded max
        // and min values in the stimulus vector
        randomUpper = Double.parseDouble(stimulusVals[0].getText());
        randomLower = Double.parseDouble(stimulusVals[0].getText());

        for (int i = 0; i < valArray.length; i++) {
            if ((Double.parseDouble(stimulusVals[i].getText())) > randomUpper) {
                randomUpper = Double.parseDouble(stimulusVals[i].getText());
            }

            if ((Double.parseDouble(stimulusVals[i].getText()) < randomLower)) {
                randomLower = Double.parseDouble(stimulusVals[i].getText());
            }
        }

        randomUpper = Math.rint(randomUpper);
        randomLower = Math.rint(randomLower);

        tfStimulusNum.setText(Integer.toString(valArray.length));
        tfRandomUpper.setText(Double.toString(randomUpper));
        tfRandomLower.setText(Double.toString(randomLower));
    }

    /**
     * Set values based on fields.
     */
    public void commitChanges() {

        for (int i = 0; i < valArray.length; i++) {
            valArray[i] = Double.parseDouble(stimulusVals[i].getText());
        }

        smellSource.setStimulusVector(valArray);
        smellSource.setDispersion(Double.parseDouble(tfDispersion.getText()));
        smellSource.setDecayFunction((DecayFunction) cbDecayFunction
                .getSelectedItem());
        smellSource.setPeak(Double.parseDouble(tfPeak.getText()));

        smellSource.setAddNoise(rbAddNoise.isSelected());

        if (rbAddNoise.isSelected()) {
            smellSource.setNoiseLevel((double) jsNoiseLevel.getValue() / 100);
        }
    }

    /**
     * Updates the stimulus panel.
     */
    private void updateStimulusPanel() {
        // Create stimulus panel
        for (int i = 0; i < valArray.length; i++) {
            stimulusVals[i] = new JTextField("" + valArray[i]);
            final int col = 7;
            stimulusVals[i].setColumns(col);

            int lbl = i + 1;
            JLabel tmp = new JLabel(lbl + ":");
            con.weightx = 0.3;
            con.gridx = 1;
            con.gridy = i + 1;
            stimulusPanel.add(tmp, con);
            con.weightx = 3;
            con.gridx = 2;
            con.gridy = i + 1;
            stimulusPanel.add(stimulusVals[i], con);
        }
    }

    /**
     * Populates stimulus panel with new data.
     */
    private void refreshStimulusPanel() {
        // removeStimulusPanel();
        stimulusPanel.removeAll();
        stimulusVals = new JTextField[valArray.length];

        updateStimulusPanel();

        stimulusPanel.updateUI();
        tfStimulusNum.setText(Integer.toString(valArray.length));
    }

    /**
     * Changes size of array.
     *
     * @param num New size of array
     */
    private void changeStimulusDimension(final int num) {
        double[] newStim = new double[num];

        for (int i = 0; i < num; i++) {
            if (i < valArray.length) {
                newStim[i] = valArray[i];
            } else {
                newStim[i] = 0;
            }
        }

        valArray = newStim;
    }

    /**
     * Randomizes numbers within text field array.
     */
    private void randomizeStimulus() {
        if (randomLower >= randomUpper) {
            JOptionPane.showMessageDialog(null,
                    "Upper and lower  values out of bounds.", "Warning",
                    JOptionPane.ERROR_MESSAGE);

            return;
        }

        stimulusPanel.removeAll();

        for (int i = 0; i < valArray.length; i++) {
            stimulusVals[i] = new JTextField(
                    ""
                            + (((randomUpper - randomLower) * Math.random()) + randomLower));
            int lbl = i + 1;
            JLabel tmp = new JLabel(lbl + ":");
            con.weightx = 0.3;
            con.gridx = 1;
            con.gridy = i + 1;
            stimulusPanel.add(tmp, con);
            con.weightx = 3;
            con.gridx = 2;
            con.gridy = i + 1;
            stimulusPanel.add(stimulusVals[i], con);
        }

        stimulusPanel.updateUI();
    }

    /**
     * Acton Listener.
     *
     * @param e the ActionEvent triggering this method
     */
    public void actionPerformed(final ActionEvent e) {
        String cmd = e.getActionCommand();

        if (rbAddNoise.isSelected()) {
            jsNoiseLevel.setEnabled(true);
        } else {
            jsNoiseLevel.setEnabled(false);
        }

        if (cmd.equals("addStimulus")) {
            changeStimulusDimension(Integer.parseInt(tfStimulusNum.getText()));
            refreshStimulusPanel();
        } else if (cmd.equals("randomize")) {
            randomUpper = Double.parseDouble(tfRandomUpper.getText());
            randomLower = Double.parseDouble(tfRandomLower.getText());
            randomizeStimulus();
        }
    }

    /**
     * @return the valuesPanel
     */
    public LabelledItemPanel getValuesPanel() {
        return valuesPanel;
    }

    /**
     * @return the dispersionPanel
     */
    public LabelledItemPanel getDispersionPanel() {
        return dispersionPanel;
    }

}
