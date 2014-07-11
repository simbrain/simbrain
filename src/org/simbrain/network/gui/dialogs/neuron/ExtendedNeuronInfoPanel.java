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
 * A panel containing more detailed generic information about neurons. Generally
 * speaking, this panel is not meant to exist in a dialog by itself, it is a set
 * of commonly used (hence generic) neuron value fields which is shared by
 * multiple complete dialogs.
 *
 * Values included are: Activation, upper / lower bounds, label, priority and
 * increment.
 *
 * @author ztosi
 *
 */
@SuppressWarnings("serial")
public class ExtendedNeuronInfoPanel extends JPanel implements EditablePanel {

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

    /** Is clipping visible in this panel. */
    private boolean clippingVisible;

    /** Bounds panel. */
    private final JPanel boundsPanel = new JPanel();

    /** Clipping panel. */
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
     * Checks that the upper bounds of all the neurons this panel is editing are
     * the same. If they are not, sets the text in {@link #tfCeiling} to "...".
     * If not all the neurons share the bounded interface, throws a
     * ClassCastException.
     *
     * @param ruleList
     * @throws ClassCastException
     */
    private void upBoundConsistencyCheckAndAssign(
            List<NeuronUpdateRule> ruleList) throws ClassCastException {
        Neuron neuronRef = neuronList.get(0);
        double upBound = ((BoundedUpdateRule) neuronRef.getUpdateRule())
                .getUpperBound();
        boolean upDiscrepancy = false;
        for (NeuronUpdateRule nur : ruleList) {
            // Does the upper bound of the first neuron equal the upper bound
            // of this one? upDiscrepancy is true if it does not.
            upDiscrepancy = upBound != ((BoundedUpdateRule) nur)
                    .getUpperBound();
            if (upDiscrepancy) { // There is at least one discrepancy
                                 // so we can't assign tfCeiling a value
                break;
            }
        }
        if (upDiscrepancy) { // Discrepancy... assign null string
            tfCeiling.setText(SimbrainConstants.NULL_STRING);
        } else { // No discrepancies... assign value
            tfCeiling.setText(Double.toString(upBound));
        }
    }

    /**
     * Checks that the lower bounds of all the neurons this panel is editing are
     * the same. If they are not, sets the text in {@link #tfFloor} to "...". If
     * not all the neurons share the bounded interface, throws a
     * ClassCastException.
     *
     * @param ruleList
     * @throws ClassCastException
     */
    private void lowBoundConsistencyCheckAndAssign(
            List<NeuronUpdateRule> ruleList) throws ClassCastException {
        Neuron neuronRef = neuronList.get(0);
        double lowBound = ((BoundedUpdateRule) neuronRef.getUpdateRule())
                .getLowerBound();
        boolean lowDiscrepancy = false;
        for (NeuronUpdateRule nur : ruleList) {
            // Does the lower bound of the first neuron equal the lower bound
            // of this one? lowDiscrepancy is true if it does not.
            lowDiscrepancy = lowBound != ((BoundedUpdateRule) nur)
                    .getLowerBound();
            if (lowDiscrepancy) { // There is at least one discrepancy
                                  // so we can't assign tfFloor a value
                break;
            }
        }
        if (lowDiscrepancy) { // Discrepancy... assign null string
            tfFloor.setText(SimbrainConstants.NULL_STRING);
        } else { // No discrepancies... assign value
            tfFloor.setText(Double.toString(lowBound));
        }
    }

    /**
     * Checks that the clipping states of all the neurons this panel is editing
     * are the same. If they are not, sets the the values of the
     * {@link #clipping} TristateDropDown to a null value. If not all the
     * neurons share the clippable interface, throws a ClassCastException.
     *
     * @param ruleList
     * @throws ClassCastException
     */
    private void clippingConsistencyCheckAndAssign(
            List<NeuronUpdateRule> ruleList) throws ClassCastException {
        Neuron neuronRef = neuronList.get(0);
        boolean clipped = ((ClippableUpdateRule) neuronRef.getUpdateRule())
                .isClipped();
        boolean discrepancy = false;
        for (NeuronUpdateRule nur : ruleList) {
            // Is the first neuron clipped, but not this one? If so
            // there is a discrepancy and below code assigns true
            // to discrepancy.
            discrepancy = clipped != ((ClippableUpdateRule) nur).isClipped();
            if (discrepancy) { // There is at least one discrepancy
                               // so we can't assign clipping a binary value
                break;
            }
        }
        if (discrepancy) { // Discrepancy detected, set clipping to null state
            clipping.setSelectedIndex(TristateDropDown.getNULL());
            setBoundsEnabled(false);
        } else { // No discrepancies... assign neuronRef's clipping value to
                 // clipping
            clipping.setSelected(clipped);
            setBoundsEnabled(clipped);
        }
        setClippingVisible(true);
        setBoundsEnabled(clipped);
    }

    @Override
    public void fillFieldValues() {
        Neuron neuronRef = neuronList.get(0);
        List<NeuronUpdateRule> ruleList = Neuron.getRuleList(neuronList);
        boolean skipClipCheck = false;
        try {
            // Check for consistency of bounded interface and consistency
            // of bounded values provided the former is consistent
            upBoundConsistencyCheckAndAssign(ruleList);
            lowBoundConsistencyCheckAndAssign(ruleList);
            setBoundsVisible(true);
            setBoundsEnabled(true);
            // Catch class cast exceptions thrown if not all the neurons (or
            // none)
            // implement the bounded interface... in which case tfCeiling and
            // tfFloor are not applicable
        } catch (ClassCastException cce) {
            // Disable fields related to bounded & clipped rules.
            setBoundsVisible(false);
            setBoundsEnabled(false);
            setClippingVisible(false);
            // If not all neurons are bounded, then by necesity not all are
            // clipped, thus checks related to clipping can be skipped.
            skipClipCheck = true;
        }
        if (!skipClipCheck) {
            try {
                clippingConsistencyCheckAndAssign(ruleList);
            } catch (ClassCastException cce) {
                // Not all neurons shared the clippable interface
                setClippingVisible(false);
            }
        }

        // Handle Increment
        if (!NetworkUtils.isConsistent(Neuron.getRuleList(neuronList),
                NeuronUpdateRule.class, "getIncrement")) {
            tfIncrement.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfIncrement.setText(Double.toString(neuronRef.getUpdateRule()
                    .getIncrement()));
        }

        // Handle Priority
        if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
                "getUpdatePriority")) {
            tfPriority.setText(SimbrainConstants.NULL_STRING);
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
     * Initialize the panel with default field values.
     *
     * @param rule
     *            rule to use for setting the default values.
     */
    private void initializeDefaultValues(NeuronUpdateRule rule) {
        tfCeiling.setText(Double.toString(((BoundedUpdateRule) rule)
                .getUpperBound()));
        tfFloor.setText(Double.toString(((BoundedUpdateRule) rule)
                .getLowerBound()));
        tfIncrement.setText(Double.toString(rule.getIncrement()));
        tfPriority.setText(Integer.toString(0));
    }

    /**
     * Update field visibility based on whether rule is bounded and/or clipped.
     *
     * @param rule
     *            the current rule
     */
    public void updateFieldVisibility(NeuronUpdateRule rule) {
        boolean bounded = rule instanceof BoundedUpdateRule;
        boolean clip = false;
        setBoundsVisible(bounded);
        if (bounded) {
            clip = rule instanceof ClippableUpdateRule;
            clipping.setSelected(clip);
        }
        setClippingVisible(clip);
        setBoundsEnabled(bounded);
    }

    /**
     * <b>Specifically:</b> Uses the values from text fields to alter
     * corresponding values in the neuron(s) being edited. Called externally to
     * apply changes.
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
                    success &= tfCeiling.getText().matches(
                            SimbrainConstants.NULL_STRING);
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
                    success &= tfFloor.getText().matches(
                            SimbrainConstants.NULL_STRING);
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
            success &= tfIncrement.getText().matches(
                    SimbrainConstants.NULL_STRING);
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
            success &= tfPriority.getText().matches(
                    SimbrainConstants.NULL_STRING);
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
        clipping.setSelectedIndex(boundsEnabled ? t : f);
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
     *            should the clipping stuff be visible or not
     */
    public void setClippingVisible(boolean visible) {
        clippingVisible = visible;
        clippingPanel.setVisible(visible);
        repaint();
        parent.pack();
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}
