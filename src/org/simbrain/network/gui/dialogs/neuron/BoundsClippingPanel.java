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
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ClippableUpdateRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.YesNoNull;

/**
 * Manage the clipping, upper bound, and lower bound fields, and their logic. If
 * clipping is on, then bounds are not used, so disable those fields.
 */
@SuppressWarnings("serial")
public class BoundsClippingPanel extends JPanel implements EditablePanel {

    /** The neurons being modified. */
    private List<Neuron> neuronList;

    /** Upper bound field. */
    private final JTextField tfCeiling = new JTextField();

    /** Lower bound field. */
    private final JTextField tfFloor = new JTextField();

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
     * True if this neuron update rule implements BoundedUpdateRule interface.
     */
    private boolean boundsVisible;

    /** True if the bounds text fields are enabled and clipping turned on. */
    private boolean boundsEnabled;

    /** Is clipping visible in this panel. */
    private boolean clippingVisible;

    /** Bounds panel. */
    private final JPanel boundsPanel = new JPanel();

    /** Clipping panel. */
    private final JPanel clippingPanel = new JPanel();

    /**
     * A drop down box to display whether clipping is used, unused or both among
     * the selected neurons.
     */
    private final YesNoNull clippingDropDown = new YesNoNull();
    
    /** Parent reference so pack can be called. */
    private final Window parent;

    /**
     * Construct the clipping / bounds panel.
     *
     * @param neuronList the neuron list being edited
     * @param parent the parent window so it can be packed when needed
     */
    public BoundsClippingPanel(final List<Neuron> neuronList,
            final Window parent) {
        this.neuronList = neuronList;
        this.parent = parent;

        // Clipping dropdown listener
        clippingDropDown.addActionListener(e -> setBoundsEnabled(
                clippingDropDown.getSelectedIndex() == YesNoNull.getTRUE()));
 
        // Layout panel
        boundsPanel.setLayout(new BoxLayout(boundsPanel, BoxLayout.Y_AXIS));
        GridLayout gl = new GridLayout(0, 2);
        gl.setVgap(2);
        clippingPanel.setLayout(gl);
        clippingPanel.add(new JLabel("Clipping: "));
        clippingPanel.add(clippingDropDown);
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

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(clippingPanel);
        this.add(boundsPanel);
    }

    @Override
    public boolean commitChanges() {
        boolean success = true;

        if (boundsVisible) {
            // Clipping?
            if (!clippingDropDown.isNull() && clippingVisible) {
                neuronList.stream()
                        .forEach(n -> ((ClippableUpdateRule) n.getUpdateRule())
                                .setClipped(clippingDropDown.isSelected()));
            }
            if (boundsEnabled) {
                // Upper Bound
                double ceiling = Utils.doubleParsable(tfCeiling);
                if (!Double.isNaN(ceiling)) {
                    neuronList.stream().forEach(
                            n -> ((BoundedUpdateRule) n.getUpdateRule())
                                    .setUpperBound(ceiling));
                } else {
                    // Only successful if the field can't be parsed because
                    // it is a NULL_STRING standing in for multiple values
                    success &= tfCeiling.getText()
                            .matches(SimbrainConstants.NULL_STRING);
                }
                // Lower Bound
                double floor = Utils.doubleParsable(tfFloor);
                if (!Double.isNaN(floor)) {
                    neuronList.stream().forEach(
                            n -> ((BoundedUpdateRule) n.getUpdateRule())
                                    .setLowerBound(floor));
                } else {
                    // Only successful if the field can't be parsed because
                    // it is a NULL_STRING standing in for multiple values
                    success &= tfFloor.getText()
                            .matches(SimbrainConstants.NULL_STRING);
                }
            }
        }
        return success;
    }

    @Override
    public void fillFieldValues() {
        List<NeuronUpdateRule> ruleList = Neuron.getRuleList(neuronList);
        boolean skipClipCheck = false;

        // Handle upper and lower bound fields
        try {
            upBoundConsistencyCheckAndAssign(ruleList);
            lowBoundConsistencyCheckAndAssign(ruleList);
            setBoundsPanelVisible(true);
            setBoundsEnabled(true);
        } catch (ClassCastException cce) {
            // If at least one neuron is not a bound or clipped rule,
            // the bound and clipping fields are not handled.
            // Disable fields related to bounded & clipped rules.
            setBoundsPanelVisible(false);
            setBoundsEnabled(false);
            setClippingPanelVisible(false);
            // If not all neurons are bounded, then by necessity not all are
            // clipped, thus checks related to clipping can be skipped.
            skipClipCheck = true;
        }

        // Handle clipping drop-down
        if (!skipClipCheck) {
            try {
                clippingConsistencyCheckAndAssign(ruleList);
            } catch (ClassCastException cce) {
                // Not all neurons shared the clippable interface
                setClippingPanelVisible(false);
            }
        }

    }

    /**
     * Fill field values for upper bound field. Use "..." if the list is
     * inconsistent.
     *
     * @param ruleList the list of rules.
     * @throws ClassCastException
     */
    private void upBoundConsistencyCheckAndAssign(
            List<NeuronUpdateRule> ruleList) throws ClassCastException {
        Neuron neuronRef = neuronList.get(0);
        double upBound = ((BoundedUpdateRule) neuronRef.getUpdateRule())
                .getUpperBound();
        boolean upDiscrepancy = ruleList.stream().anyMatch(
                rule -> upBound != ((BoundedUpdateRule) rule).getUpperBound());
        if (upDiscrepancy) {
            tfCeiling.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfCeiling.setText(Double.toString(upBound));
        }
    }

    /**
     * Fill field values for lower bound field. Use "..." if the list is
     * inconsistent.
     *
     * @param ruleList list of rules
     * @throws ClassCastException
     */
    private void lowBoundConsistencyCheckAndAssign(
            List<NeuronUpdateRule> ruleList) throws ClassCastException {
        Neuron neuronRef = neuronList.get(0);
        double lowBound = ((BoundedUpdateRule) neuronRef.getUpdateRule())
                .getLowerBound();
        boolean lowDiscrepancy = ruleList.stream().anyMatch(
                rule -> lowBound != ((BoundedUpdateRule) rule).getLowerBound());
        if (lowDiscrepancy) {
            tfFloor.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfFloor.setText(Double.toString(lowBound));
        }
    }

    /**
     * Assign state to clipping drop down. Use "..." if the list is
     * inconsistent.
     *
     * @param ruleList list of rules
     * @throws ClassCastException
     */
    private void clippingConsistencyCheckAndAssign(
            List<NeuronUpdateRule> ruleList) throws ClassCastException {
        Neuron neuronRef = neuronList.get(0);
        boolean clipped = ((ClippableUpdateRule) neuronRef.getUpdateRule())
                .isClipped();
        boolean discrepancy = ruleList.stream().anyMatch(
                rule -> clipped != ((ClippableUpdateRule) rule).isClipped());
        if (discrepancy) {
            clippingDropDown.setNull();
            setBoundsEnabled(false);
        } else {
            clippingDropDown.setSelected(clipped);
            setBoundsEnabled(clipped);
        }
        setClippingPanelVisible(true);
        setBoundsEnabled(clipped);
    }

    /**
     * Update field visibility based on whether rule is bounded and/or clipped.
     * Called when the neuron update rule is changed.
     *
     * @param rule the current rule
     */
    public void updateFieldVisibility(NeuronUpdateRule rule) {
        boolean bounded = rule instanceof BoundedUpdateRule;
        boolean clip = false;
        setBoundsPanelVisible(bounded);
        if (bounded) {
            clip = rule instanceof ClippableUpdateRule;
            clippingDropDown.setSelected(clip);
        }
        setClippingPanelVisible(clip);
        setBoundsEnabled(bounded);
    }

    /**
     * If bounds are enabled then the text fields should be enabled, and
     * if not then text fields should be disabled.
     *
     * @param enabled are upper and lower bounds fields enabled?
     */
    private void setBoundsEnabled(boolean enabled) {
        boundsEnabled = enabled;
        tfCeiling.setEnabled(enabled);
        tfFloor.setEnabled(enabled);
        repaint();
    }

    /**
     * Toggle visibility of bounds sub-panel.
     *
     * @param visible true if upper and lower bound fields should be visible
     */
    private void setBoundsPanelVisible(boolean visible) {
        boundsVisible = visible;
        boundsPanel.setVisible(visible);
        repaint();
        parent.pack();
        parent.setLocationRelativeTo(null);
    }

    /**
     * Toggle visibility of clipping sub-panel.
     *
     * @param visible true if the clipping stuff should be visible
     */
    private void setClippingPanelVisible(boolean visible) {
        clippingVisible = visible;
        clippingPanel.setVisible(visible);
        repaint();
        parent.pack();
        parent.setLocationRelativeTo(null);
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}
