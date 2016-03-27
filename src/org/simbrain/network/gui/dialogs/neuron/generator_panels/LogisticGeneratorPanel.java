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
package org.simbrain.network.gui.dialogs.neuron.generator_panels;

import javax.swing.JTextField;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.activity_generators.LogisticRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>LogisticNeuronPanel</b> TODO: Work into new Input Generator Framework,
 * currently no implementation.
 */
public class LogisticGeneratorPanel extends AbstractNeuronRulePanel {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** A reference to the neuron rule being edited. */
    private LogisticRule prototypRule  = new LogisticRule();

    /**
     * Creates an instance of this panel.
     */
    public LogisticGeneratorPanel() {
        super();
        JTextField tfGrowthRate = createTextField(
                (r) -> ((LogisticRule) r).getGrowthRate(),
                (r, val) -> ((LogisticRule) r).setGrowthRate((double) val));
        mainPanel.addItem("Growth Rate", tfGrowthRate);
        add(mainPanel);
        this.addBottomText(
                "<html>Note 1: This is not a sigmoidal logistic function. <p>"
                        + "For that, create a neuron and set its update rule to sigmoidal.<p> "
                        + " Note 2: for chaos, try growth rates between 3.6 and 4</html>");
    }

    @Override
    public final NeuronUpdateRule getPrototypeRule() {
        return prototypRule.deepCopy();
    }
}
