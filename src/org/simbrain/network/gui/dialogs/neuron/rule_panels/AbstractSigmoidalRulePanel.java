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
package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronNoiseGenPanel;
import org.simbrain.network.neuron_update_rules.AbstractSigmoidalRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.math.SquashingFunction;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * A rule panel containing all the variables and methods which would be shared
 * between rules panels for discrete and continuous time sigmoidal rule panels.
 * 
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public abstract class AbstractSigmoidalRulePanel extends
    AbstractNeuronRulePanel {

    /** Implementation combo box. */
    protected JComboBox<SquashingFunction> cbImplementation =
        new JComboBox<SquashingFunction>(new SquashingFunction[] {
            SquashingFunction.ARCTAN, SquashingFunction.LOGISTIC,
            SquashingFunction.TANH, });

    /** Bias field. */
    protected JTextField tfBias = new JTextField();

    /** Slope field. */
    protected JTextField tfSlope = new JTextField();

    /** Tabbed pane. */
    protected JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    protected LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    protected NeuronNoiseGenPanel randTab = new NeuronNoiseGenPanel();

    /** Add noise combo box. */
    protected TristateDropDown isAddNoise = new TristateDropDown();

    /**
     * The initially selected squashing function (or NULL_STRING), used for
     * determining how to fill field values based on the selected
     * implementation.
     */
    protected SquashingFunction initialSfunction;

    /**
     * @return the combo box responsible for setting the specific squashing
     * function
     */
    public JComboBox<SquashingFunction> getCbImplementation() {
        return cbImplementation;
    }

}
