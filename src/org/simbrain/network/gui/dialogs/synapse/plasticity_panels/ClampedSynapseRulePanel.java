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
package org.simbrain.network.gui.dialogs.synapse.plasticity_panels;

import java.util.List;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.synapse_update_rules.ClampedSynapseRule;

/**
 * <b>ClampedSynapsePanel</b>.
 */
public class ClampedSynapseRulePanel extends AbstractSynapsePanel {

    /** Synapse reference. */
    private ClampedSynapseRule synapseRef;

    /**
     * This method is the default constructor.
     */
    public ClampedSynapseRulePanel() {
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues(List<SynapseUpdateRule> ruleList) {
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final List<Synapse> commitSynapses) {
        for (Synapse s : commitSynapses) {
            commitChanges(s);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final Synapse templateSynapse) {
        synapseRef = new ClampedSynapseRule();
        templateSynapse.setLearningRule(synapseRef);
    }

}
