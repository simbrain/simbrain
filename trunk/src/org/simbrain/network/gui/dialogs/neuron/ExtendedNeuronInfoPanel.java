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
package org.simbrain.network.gui.dialogs.neuron;

import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ClippableUpdateRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * 
 * A panel containing more detailed generic information about neurons. Generally
 * speaking, this panel is not meant to exist in a dialog by itself, it is a set
 * of commonly used (hence generic) neuron value fields which is shared by
 * multiple complete dialogs.
 * 
 * Values included are: Activation ceiling and floor, label, priority and
 * increment.
 * 
 * @author ztosi
 * 
 */
@SuppressWarnings("serial")
public class ExtendedNeuronInfoPanel extends JPanel implements EditablePanel {

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Upper bound field. */
    private final JTextField tfCeiling = new JTextField();

    /** Lower bound field. */
    private final JTextField tfFloor = new JTextField();

    /**
     * A drop down box to display whether clipping is used, unused or both among
     * the selected neurons.
     */
    private final TristateDropDown clipping = new TristateDropDown();

    {
        clipping.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setBoundsEnabled(clipping.getSelectedIndex() == TristateDropDown
                        .getTRUE());
            }

        });
    }

    /**
     * Label for upper bound text field. Is a class variable so that its
     * visibility can be set alongside the visibility of the text field.
     */
    private final JLabel upperBound = new JLabel("Upper bound: ");

    /**
     * Label for lower bound text field. Is a class variable so that its
     * visibility can be set alongside the visibility of the text field.
     */
    private final JLabel lowerBound = new JLabel("Lower bound: ");

    /**
     * Label for clipping field.
     */
    private final JLabel clipL = new JLabel("Clipping: ");

    /** Increment field. */
    private final JTextField tfIncrement = new JTextField();

    /** Priority Field. */
    private final JTextField tfPriority = new JTextField();

    /** Are upper and lower bounds visible? */
    private boolean boundsVisible;

    /** Are upper and lower bounds enabled? */
    private boolean boundsEnabled;

    private boolean clippingVisible;

    /** Bounds panel. */
    private final JPanel boundsPanel = new JPanel();

    private final JPanel clippingPanel = new JPanel();

    /**
     * Whether or not the neuron is clamped (i.e. will not update/change its
     * activation once set).
     */
    private final TristateDropDown clamped = new TristateDropDown();

    /** Parent reference so pack can be called. */
    private final Window parent;

    /** The neurons being modified. */
    private List<Neuron> neuronList;

    /**
     * Construct the panel representing the provided neurons.
     * 
     * @param neuronList
     *            list of neurons to represent.
     * @param parent
     *            parent window so pack can be called
     */
    public ExtendedNeuronInfoPanel(final List<Neuron> neuronList,
            final Window parent) {
        this.neuronList = neuronList;
        this.parent = parent;
        fillFieldValues();
        initializeLayout();
    }

    /**
     * Lays out the panel
     */
    private void initializeLayout() {

        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);

        GridLayout gl = new GridLayout(0, 2);
        gl.setVgap(5);

        JPanel clampP = new JPanel(gl);
        clampP.add(new JLabel("Clamped: "));
        clampP.add(clamped);
        clampP.setAlignmentX(CENTER_ALIGNMENT);
        this.add(clampP);

        this.add(Box.createVerticalStrut(5));

        boundsPanel.setLayout(new BoxLayout(boundsPanel, BoxLayout.Y_AXIS));
        clippingPanel.setLayout(gl);
        clippingPanel.add(clipL);
        clippingPanel.add(clipping);
        clippingPanel.setAlignmentX(CENTER_ALIGNMENT);
        boundsPanel.add(clippingPanel);
        boundsPanel.add(Box.createVerticalStrut(5));
        JPanel sbp2 = new JPanel(gl);
        sbp2.add(upperBound);
        sbp2.add(tfCeiling);
        sbp2.add(lowerBound);
        sbp2.add(tfFloor);
        sbp2.setAlignmentX(CENTER_ALIGNMENT);
        boundsPanel.add(sbp2);
        boundsPanel.add(Box.createVerticalStrut(5));
        boundsPanel.setAlignmentX(CENTER_ALIGNMENT);
        this.add(boundsPanel);

        JPanel subP = new JPanel(gl);
        subP.add(new JLabel("Increment: "));
        subP.add(tfIncrement);
        subP.add(new JLabel("Priority:"));
        subP.add(tfPriority);
        subP.setAlignmentX(CENTER_ALIGNMENT);
        this.add(subP);

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    }

    /**
     * Fills the values of the text fields based on the corresponding values of
     * the neurons to be edited. Called before {@link #initializeLayout()}.
     */
    public void fillFieldValues() {

        Neuron neuronRef = neuronList.get(0);
        List<NeuronUpdateRule> ruleList = Neuron.getRuleList(neuronList);

        boolean skipClipCheck = false;
        try {
            double upBound = ((BoundedUpdateRule) neuronRef.getUpdateRule())
                    .getUpperBound();
            double lowBound = ((BoundedUpdateRule) neuronRef.getUpdateRule())
                    .getLowerBound();
            boolean upDiscrepancy = false;
            boolean lowDiscrepancy = false;
            for (NeuronUpdateRule nur : ruleList) {
                upDiscrepancy = upBound != ((BoundedUpdateRule) nur)
                        .getUpperBound();
                if (upDiscrepancy) {
                    break;
                }
            }
            if (upDiscrepancy) {
                tfCeiling.setText(SimbrainConstants.NULL_STRING);
            } else {
                tfCeiling.setText(Double.toString(upBound));
            }

            for (NeuronUpdateRule nur : ruleList) {
                lowDiscrepancy = lowBound != ((BoundedUpdateRule) nur)
                        .getLowerBound();
                if (lowDiscrepancy) {
                    break;
                }
            }
            if (lowDiscrepancy) {
                tfCeiling.setText(SimbrainConstants.NULL_STRING);
            } else {
                tfFloor.setText(Double.toString(lowBound));
            }
            setBoundsVisible(true);
            setBoundsEnabled(true);
        } catch (ClassCastException cce) {
            setBoundsVisible(false);
            setBoundsEnabled(false);
            setClippingVisible(false);
            skipClipCheck = true;
        }

        if (!skipClipCheck) {
            try {
                boolean clipped = ((ClippableUpdateRule) neuronRef
                        .getUpdateRule()).isClipped();
                boolean discrepancy = false;
                for (NeuronUpdateRule nur : ruleList) {
                    discrepancy = clipped != ((ClippableUpdateRule) nur)
                            .isClipped();
                    if (discrepancy) {
                        break;
                    }
                }
                if (discrepancy) {
                    clipping.setSelectedIndex(TristateDropDown.getNULL());
                    setBoundsEnabled(false);
                } else {
                    clipping.setSelected(clipped);
                    setBoundsEnabled(clipped);
                }
                setClippingVisible(true);
                setBoundsEnabled(clipped);
            } catch (ClassCastException cce) {
                setClippingVisible(false);
            }
        }

        // Handle Increment
        if (!NetworkUtils.isConsistent(Neuron.getRuleList(neuronList),
                NeuronUpdateRule.class, "getIncrement")) {
            tfIncrement.setText(NULL_STRING);
        } else {
            tfIncrement.setText(Double.toString(neuronRef.getUpdateRule()
                    .getIncrement()));
        }

        // Handle Priority
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
                "getUpdatePriority")) {
            tfPriority.setText(NULL_STRING);
        } else {
            tfPriority.setText(Integer.toString(neuronRef.getUpdatePriority()));
        }
        // Handle Clamped
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "isClamped")) {
            clamped.setNull();
        } else {
            clamped.setSelected(neuronList.get(0).isClamped());
        }
    }

    /**
     * @param rule
     *            the neuron update rule from which default values will be used
     *            to fill field data.
     */
    public void fillDefaultValues(NeuronUpdateRule rule) {
        boolean bounded = rule instanceof BoundedUpdateRule;
        boolean clip = false;
        setBoundsVisible(bounded);
        if (bounded) {
            tfCeiling.setText(Double.toString(((BoundedUpdateRule) rule)
                    .getUpperBound()));
            tfFloor.setText(Double.toString(((BoundedUpdateRule) rule)
                    .getLowerBound()));
            clip = rule instanceof ClippableUpdateRule;
            clipping.setSelected(clip);
        }
        setClippingVisible(clip);
        setBoundsEnabled(bounded);
        tfIncrement.setText(Double.toString(rule.getIncrement()));
        tfPriority.setText(Integer.toString(0));
    }

    /**
     * {@inheritDoc} <b>Specifically:</b> Uses the values from text fields to
     * alter corresponding values in the neuron(s) being edited. Called
     * externally to apply changes.
     */
    @Override
    public boolean commitChanges() {
        int numNeurons = neuronList.size();
        boolean success = true;
        if (boundsVisible) {
            // Clipping?
            if (!clipping.isNull() && clippingVisible) {
                boolean clip = clipping.getSelectedIndex() == TristateDropDown
                        .getTRUE();
                for (int i = 0; i < numNeurons; i++) {
                    ((ClippableUpdateRule) neuronList.get(i).getUpdateRule())
                            .setClipped(clip);
                }
            }
            if (boundsVisible && boundsEnabled) {
                // Upper Bound
                double ceiling = Utils.doubleParsable(tfCeiling);
                if (!Double.isNaN(ceiling)) {
                    for (int i = 0; i < numNeurons; i++) {
                        ((BoundedUpdateRule) neuronList.get(i).getUpdateRule())
                                .setUpperBound(ceiling);
                    }
                } else {
                    // Only successful if the field can't be parsed because
                    // it is a NULL_STRING standing in for multiple values
                    success &= tfCeiling.getText().matches(NULL_STRING);
                }
                // Lower Bound
                double floor = Utils.doubleParsable(tfFloor);
                if (!Double.isNaN(floor)) {
                    for (int i = 0; i < numNeurons; i++) {
                        ((BoundedUpdateRule) neuronList.get(i).getUpdateRule())
                                .setLowerBound(floor);
                    }
                } else {
                    // Only successful if the field can't be parsed because
                    // it is a NULL_STRING standing in for multiple values
                    success &= tfFloor.getText().matches(NULL_STRING);
                }
            }

        }

        // Increment
        double increment = Utils.doubleParsable(tfIncrement);
        if (!Double.isNaN(increment)) {
            for (int i = 0; i < numNeurons; i++) {
                neuronList.get(i).getUpdateRule().setIncrement(increment);
            }
        } else {
            // Only successful if the field can't be parsed because
            // it is a NULL_STRING standing in for multiple values
            success &= tfIncrement.getText().matches(NULL_STRING);
        }

        // Priority
        double priority = Utils.doubleParsable(tfPriority);
        if (!Double.isNaN(priority)) {
            int p = (int) priority; // Cast to integer (there is no NaN value
            // for integers to use as a flag).
            for (int i = 0; i < numNeurons; i++) {
                neuronList.get(i).setUpdatePriority(p);
            }
        } else {
            // Only successful if the field can't be parsed because
            // it is a NULL_STRING standing in for multiple values
            success &= tfPriority.getText().matches(NULL_STRING);
        }

        // Clamped
        if (!clamped.isNull()) {
            boolean clamp = clamped.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                neuronList.get(i).setClamped(clamp);
            }
        }

        return success;
    }

    /**
     * @return the TristateDropDown menu controlling whether or not the neurons'
     *         activation(s) are clamped
     */
    public TristateDropDown getClamped() {
        return clamped;
    }

    /**
     * @param enabled
     *            are upper and lower bounds fields enabled?
     */
    public void setBoundsEnabled(boolean enabled) {
        boundsEnabled = enabled;
        int t = TristateDropDown.getTRUE();
        int f = TristateDropDown.getFALSE();
        clipping.setSelectedIndex(isBoundsEnabled() ? t : f);
        tfCeiling.setEnabled(enabled);
        tfFloor.setEnabled(enabled);
        repaint();
    }

    /**
     * @param visible
     *            are upper and lower bound fields visible?
     */
    public void setBoundsVisible(boolean visible) {
        boundsVisible = visible;
        boundsPanel.setVisible(visible);
        repaint();
        parent.pack();
    }

    /**
     * Properly repaints the panel when clipping and its label are made visible
     * or invisible.
     * 
     * @param visible
     */
    public void setClippingVisible(boolean visible) {
        clippingVisible = visible;
        clippingPanel.setVisible(visible);
        repaint();
        parent.pack();
    }

    /**
     * Are the upper and lower bound fields visible.
     * 
     * @return whether or not boundaries are visible. Only occurs for
     *         BoundedUpdateRules
     * 
     * @see org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule.java
     */
    public boolean isBoundsVisible() {
        return boundsVisible;
    }

    /**
     * Are the upper /lower bound fields enabled.
     * 
     * @return whether or not boundary fields are enabled given the current
     *         neuron update rule
     */
    public boolean isBoundsEnabled() {
        return boundsEnabled;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}
