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

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.LogisticGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.RandomGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.SinusoidalGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.generator_panels.StochasticGeneratorPanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.*;
import org.simbrain.network.neuron_update_rules.*;
import org.simbrain.network.neuron_update_rules.activity_generators.LogisticRule;
import org.simbrain.network.neuron_update_rules.activity_generators.RandomNeuronRule;
import org.simbrain.network.neuron_update_rules.activity_generators.SinusoidalRule;
import org.simbrain.network.neuron_update_rules.activity_generators.StochasticRule;
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.ParameterGetter;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.EditablePanel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main container for selecting and editing neuron / activity generator types.
 * Holds the combo box for selecting an update rule, and a panel (which extends
 * abstractneuronrulepanel) for editing that rules properties.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class UpdateRulePanel extends JPanel implements EditablePanel {

    /**
     * The neurons being modified.
     */
    private final List<Neuron> neuronList;

    /**
     * Null string.
     */
    public static final String NULL_STRING = "...";

    /**
     * The default display state of the neuron panel. Currently, True, that is,
     * by default, the neuron panel corresponding to the rule in the combo box
     * is visible.
     */
    private static final boolean DEFAULT_NP_DISPLAY_STATE = true;

    /**
     * Neuron type combo box.
     */
    private final JComboBox<String> cbNeuronType;

    /**
     * Neuron update rule panel.
     */
    private AnnotatedPropertyEditor neuronRulePanel;

    /**
     * For showing/hiding the neuron update rule panel.
     */
    public final DropDownTriangle neuronRulePanelTriangle;

    /**
     * A reference to the parent window containing this panel for the purpose of
     * adjusting to different sized neuron update rule dialogs.
     */
    private final Window parent;

    /**
     * A reference to the original panel, so that we can easily know if we are
     * writing to already existing neuron update rules or replacing them with
     * new rules.
     */
    private AnnotatedPropertyEditor startingPanel;

    /**
     * Rule map that is either set to the RULE_MAP or GENERATOR_MAP depending on
     * what type of objects are contained in the main rule list.
     */
    private LinkedHashMap<String, AnnotatedPropertyEditor> ruleMap;

    /**
     * Associations between names of rules and panels for editing them.
     */
    private static final LinkedHashMap<String, AnnotatedPropertyEditor> RULE_MAP = new LinkedHashMap<String, AnnotatedPropertyEditor>();

    // Populate the Rule Map. Note! Place items in alphabetical order so they
    // appear that way in the GUI combo box.
    static {
//        RULE_MAP.put(new AdExIFRule().getName(), new AdExIFRulePanel());
//        RULE_MAP.put(new BinaryRule().getName(), new BinaryRulePanel());
//        RULE_MAP.put(new DecayRule().getName(), new DecayRulePanel());
//        RULE_MAP.put(new FitzhughNagumo().getName(), new FitzhughNagumoRulePanel());
        //        RULE_MAP.put(new HodgkinHuxleyRule().getName(),
        //                new HodgkinHuxleyRulePanel());
//        RULE_MAP.put(new IACRule().getName(), new IACRulePanel());
//        RULE_MAP.put(new IntegrateAndFireRule().getName(), new IntegrateAndFireRulePanel());
//        RULE_MAP.put(new IzhikevichRule().getName(), new IzhikevichRulePanel());
//        RULE_MAP.put(new KuramotoRule().getName(), new KuramotoRulePanel());
        RULE_MAP.put(new LinearRule().getName(), new AnnotatedPropertyEditor(new LinearRule()));
//        RULE_MAP.put(new MorrisLecarRule().getName(), new MorrisLecarRulePanel());
//        RULE_MAP.put(new NakaRushtonRule().getName(), new NakaRushtonRulePanel());
//        RULE_MAP.put(new ProductRule().getName(), new ProductRulePanel());
//        RULE_MAP.put(new ContinuousSigmoidalRule().getName(), new ContinuousSigmoidalRulePanel());
//        RULE_MAP.put(new SigmoidalRule().getName(), new DiscreteSigmoidalRulePanel());
//        RULE_MAP.put(new SpikingThresholdRule().getName(), new SpikingThresholdRulePanel());
//        RULE_MAP.put(new ThreeValueRule().getName(), new ThreeValueRulePanel());
    }

    /**
     * Associations between names of activity generators and panels for editing
     * them.
     */
    public static final LinkedHashMap<String, AnnotatedPropertyEditor> GENERATOR_MAP = new LinkedHashMap<String, AnnotatedPropertyEditor>();

    // Populate the Activity Generator Map. Note! Place items in alphabetical
    // order so they appear that way in the GUI combo box.
    static {
//        GENERATOR_MAP.put(new LogisticRule().getName(), new LogisticGeneratorPanel());
//        GENERATOR_MAP.put(new RandomNeuronRule().getName(), new RandomGeneratorPanel());
//        GENERATOR_MAP.put(new SinusoidalRule().getName(), new SinusoidalGeneratorPanel());
//        GENERATOR_MAP.put(new StochasticRule().getName(), new StochasticGeneratorPanel());
    }

    /**
     * Construct the panel with default starting visibility.
     *
     * @param neuronList the list of neurons being edited
     * @param parent     the parent window referenced for resizing purposes
     */
    public UpdateRulePanel(List<Neuron> neuronList, Window parent) {
        this(neuronList, parent, DEFAULT_NP_DISPLAY_STATE);
    }

    /**
     * Create the panel with specified starting visibility.
     *
     * @param neuronList    the list of neurons being edited
     * @param parent        the parent window referenced for resizing purposes
     * @param startingState the starting state of whether or not details of the
     *                      rule are initially visible
     */
    public UpdateRulePanel(List<Neuron> neuronList, Window parent, boolean startingState) {
        this.neuronList = neuronList;
        this.parent = parent;
        if (neuronList.get(0).getUpdateRule() instanceof ActivityGenerator) {
            ruleMap = GENERATOR_MAP;
            cbNeuronType = new JComboBox<String>(GENERATOR_MAP.keySet().toArray(new String[GENERATOR_MAP.size()]));
        } else {
            ruleMap = RULE_MAP;
            cbNeuronType = new JComboBox<String>(RULE_MAP.keySet().toArray(new String[RULE_MAP.size()]));
        }

        neuronRulePanelTriangle = new DropDownTriangle(UpDirection.LEFT, startingState, "Settings", "Settings", parent);

        //TODO: Do something like in SynapseRulePanel
//        checkNeuronConsistency();
//        startingPanel = neuronRulePanel;
        initializeLayout();
        addListeners();
        setRandomizerPanelParent();
    }

//    /**
//     * Initialize the main neuron panel based on whether all the neurons are the
//     * same type or not.
//     */
//    private void checkNeuronConsistency() {
//
//        // TODO: Better handling of mixed case with activity generators. Warn
//        // against it
//        // or if allowing it, change the shape of the neuron to match.
//
//        ParameterGetter<Neuron, Class<?>> typeGetter = (n) -> ((Neuron) n).getUpdateRule().getClass();
//
//        if (!NetworkUtils.isConsistent(neuronList, typeGetter)) {
//            cbNeuronType.addItem(SimbrainConstants.NULL_STRING);
//            cbNeuronType.setSelectedIndex(cbNeuronType.getItemCount() - 1);
////            neuronRulePanel = new EmptyRulePanel();
//        } else {
//            String neuronName = neuronList.get(0).getUpdateRule().getName();
//            neuronRulePanel = ruleMap.get(neuronName);
////            neuronRulePanel.setReplacingUpdateRules(false);
////            neuronRulePanel.fillFieldValues(Neuron.getRuleList(neuronList));
//            cbNeuronType.setSelectedItem(neuronName);
//        }
//    }

    /**
     * Lays out the components of the panel.
     */
    private void initializeLayout() {

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        JPanel tPanel = new JPanel();
        tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.X_AXIS));
        tPanel.add(cbNeuronType);
        int horzStrut = 30;

        // Create a minimum spacing
        tPanel.add(Box.createHorizontalStrut(horzStrut));

        // Give all extra space to the space between the components
        tPanel.add(Box.createHorizontalGlue());

        tPanel.add(neuronRulePanelTriangle);
        tPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tPanel.setBorder(padding);
        this.add(tPanel);

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        neuronRulePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        neuronRulePanel.setBorder(padding);
        neuronRulePanel.setVisible(neuronRulePanelTriangle.isDown());
        this.add(neuronRulePanel);

        TitledBorder tb = BorderFactory.createTitledBorder("Update Rule");
        this.setBorder(tb);

    }

    /**
     * Adds the listeners to this dialog.
     */
    private void addListeners() {

        neuronRulePanelTriangle.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                neuronRulePanel.setVisible(neuronRulePanelTriangle.isDown());
                repaint();
                parent.pack();
                parent.setLocationRelativeTo(null);
            }

        });

        // Change neuron type
        cbNeuronType.addActionListener(e -> {

            neuronRulePanel = ruleMap.get(cbNeuronType.getSelectedItem());
            setRandomizerPanelParent();

            // Is the current panel different from the starting panel?
            boolean replaceUpdateRules = neuronRulePanel != startingPanel;

            List<EditableObject> ruleList = neuronList.stream().map(Neuron::getUpdateRule).collect(Collectors.toList());


            // If so we have to fill the new panel with default values
            if (replaceUpdateRules) {
                neuronRulePanel.fillDefaultValues();
            } else {
                // If not we can fill the new panel with values from the
                // neurons being edited.
                neuronRulePanel.fillFieldValues(ruleList);
            }

            // Tell the panel whether it will have to replace neuron
            // update rules or edit them upon commit.
//            neuronRulePanel.setReplacingUpdateRules(replaceUpdateRules);

            repaintPanel();
            repaint();
            parent.pack();
            parent.setLocationRelativeTo(null);
        });

    }

    /**
     * Set window on randomizer panels.
     */
    private void setRandomizerPanelParent() {
//        if (neuronRulePanel.getPrototypeRule() instanceof NoisyUpdateRule) {
//            if (neuronRulePanel.getNoisePanel() != null) {
//                neuronRulePanel.getNoisePanel().setParent(parent);
//            }
//        }
    }

    @Override
    public boolean commitChanges() {
        // If the panel remains open after changes have been committed,
        // i.e. for apply button during editing, then set the original panel
        // to the selected one since for all intents and purposes it is now.
        // Prevents ClassCastExceptions for switching between panels.
//        startingPanel = neuronRulePanel;
//        neuronRulePanel.commitChanges(neuronList);
        return true; // TODO:Finish implementation of CommittablePanel interface
    }

    /**
     * Called to repaint the panel based on changes in the to the selected
     * neuron type.
     */
    public void repaintPanel() {
        removeAll();
        initializeLayout();
        repaint();
    }

    /**
     * Directly access the neuron rule panel to utilize its methods without
     * using this class as an intermediary. An example of this can be seen in
     * AddNeuronsDialog
     *
     * @return the currently displayed neuron update rule panel
     * @see AddNeuronsDialog
     */
    public AbstractNeuronRulePanel getNeuronRulePanel() {
        //TODO: Return annotated property editor and updated things that use this. They are broken for now.
        return null;
    }

    /**
     * @return the neuron update settings options combo box
     */
    public JComboBox<String> getCbNeuronType() {
        return cbNeuronType;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void fillFieldValues() {
    }

}
