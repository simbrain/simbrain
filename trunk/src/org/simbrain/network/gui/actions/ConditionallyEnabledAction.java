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
package org.simbrain.network.gui.actions;

import javax.swing.AbstractAction;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.NetworkSelectionEvent;
import org.simbrain.network.gui.NetworkSelectionListener;

/**
 * Subclass this class if an action should be enabled or not depending on
 * whether neurons, synapses or both are selected in the network panel.
 *
 * @author Jeff Yoshimi
 */
public abstract class ConditionallyEnabledAction extends AbstractAction {

    /** Network panel. */
    protected final NetworkPanel networkPanel;

    /**
     * Possible conditions under which to enable the action:
     * <ul>
     * <li>NEURONS: if at least one neuron is selected</li>
     * <li>SYNAPSES: if at least one synapse is selected</li>
     * <li>ALLITEMS: if at least one synapse or neuron is selected</li>
     * <li>SOURCE_NEURONS: if at least one neuron is designated as source neuron
     * </li>
     * <li>SOURCE_AND_TARGETS: if at least one synapse is designated as source
     * and one is selected (i.e. there is at least one source and one target)</li>
     * </ul>
     *
     */
    public static enum EnablingCondition {
        NEURONS, SYNAPSES, ALLITEMS, SOURCE_NEURONS, SOURCE_AND_TARGETS
    };

    /** Under what condition should this action be enabled. */
    private final EnablingCondition enableCondition;

    /**
     * Construct the update action.
     *
     * @param networkPanel parent network panel, must not be null
     * @param title the name for this action, passed up to superclass
     * @param updateType in what conditions to enable the action
     */
    public ConditionallyEnabledAction(final NetworkPanel networkPanel,
            final String title, final EnablingCondition updateType) {
        super(title);
        this.enableCondition = updateType;

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        updateAction();
        networkPanel.addSelectionListener(new NetworkSelectionListener() {
            public void selectionChanged(final NetworkSelectionEvent event) {
                updateAction();
            }
        });
    }

    /**
     * Enable or disable action depending on relevant conditions.
     */
    private void updateAction() {
        boolean enabled = false;
        if (enableCondition == EnablingCondition.ALLITEMS) {
            if (networkPanel.getSelectedModelElements().size() > 0) {
                enabled = true;
            }
        } else if (enableCondition == EnablingCondition.NEURONS) {
            if (networkPanel.getSelectedModelNeurons().size() > 0) {
                enabled = true;
            }
        } else if (enableCondition == EnablingCondition.SYNAPSES) {
            if (networkPanel.getSelectedModelSynapses().size() > 0) {
                enabled = true;
            }
        } else if (enableCondition == EnablingCondition.SOURCE_NEURONS) {
            if (networkPanel.getSourceModelNeurons().size() > 0) {
                enabled = true;
            }
        } else if (enableCondition == EnablingCondition.SOURCE_AND_TARGETS) {
            if ((networkPanel.getSourceModelNeurons().size() > 0)
                    && (networkPanel.getSelectedModelNeurons().size() > 0)) {
                enabled = true;
            }
        }
        setEnabled(enabled);
    }

}