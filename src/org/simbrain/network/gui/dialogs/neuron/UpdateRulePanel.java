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
import org.simbrain.network.gui.dialogs.neuron.rulepanels.AdExIFPanel;
import org.simbrain.network.gui.dialogs.neuron.rulepanels.MorrisLecarPanel;
import org.simbrain.network.neuron_update_rules.*;
import org.simbrain.network.neuron_update_rules.activity_generators.LogisticRule;
import org.simbrain.network.neuron_update_rules.activity_generators.RandomNeuronRule;
import org.simbrain.network.neuron_update_rules.activity_generators.SinusoidalRule;
import org.simbrain.network.neuron_update_rules.activity_generators.StochasticRule;
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.randomizer.gui.RandomizerPanel2;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.EditablePanel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main container for selecting and editing neuron / activity generator types.
 * Holds the combo box for selecting an update rule, and a panel (which extends
 * abstractneuronrulepanel) for editing that rules properties.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 * @author Yulin Li
 */
@SuppressWarnings("serial")
public class UpdateRulePanel extends JPanel {

    /**
     * The neurons being modified.
     */
    private final List<Neuron> neuronList;

    /**
     * List of randomizers for noisy neurons.
     */
    private List<Randomizer> randomizerList = new ArrayList<>();

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
     * Noise panel if any, null otherwise.
     */
    private RandomizerPanel2 noisePanel;

    /**
     * For showing/hiding the neuron update rule panel.
     */
    private final DropDownTriangle neuronRulePanelTriangle;

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
    private static final LinkedHashMap<String, AnnotatedPropertyEditor> RULE_MAP = new LinkedHashMap<>();

    /**
     * Tabbed pane.
     */
    private JTabbedPane tabPane;

    /**
     * True if all nodes implement {@link NoisyUpdateRule}.
     */
    private boolean allNodesNoisy;

    /**
     * True if an inconsistent set of neurons is being edited.
     */
    private boolean inconsistentNeurons;

    // Populate the Rule Map. Note! Place items in alphabetical order so they
    // appear that way in the GUI combo box.
    static {
        RULE_MAP.put(new AdExIFRule().getName(), new AdExIFPanel(new AdExIFRule()));
        RULE_MAP.put(new BinaryRule().getName(), new AnnotatedPropertyEditor(new BinaryRule()));
        RULE_MAP.put(new DecayRule().getName(), new AnnotatedPropertyEditor(new DecayRule()));
        RULE_MAP.put(new FitzhughNagumo().getName(), new AnnotatedPropertyEditor(new FitzhughNagumo()));
        //        RULE_MAP.put(new HodgkinHuxleyRule().getName(),
        //                new AnnotatedPropertyEditor(new HodgkinHuxleyRule())));
        RULE_MAP.put(new IACRule().getName(), new AnnotatedPropertyEditor(new IACRule()));
        RULE_MAP.put(new IntegrateAndFireRule().getName(), new AnnotatedPropertyEditor(new IntegrateAndFireRule()));
        RULE_MAP.put(new IzhikevichRule().getName(), new AnnotatedPropertyEditor(new IzhikevichRule()));
//        RULE_MAP.put(new KuramotoRule().getName(), new AnnotatedPropertyEditor(new KuramotoRule()));
        RULE_MAP.put(new LinearRule().getName(), new AnnotatedPropertyEditor(new LinearRule()));
        RULE_MAP.put(new MorrisLecarRule().getName(), new MorrisLecarPanel(new MorrisLecarRule()));
        RULE_MAP.put(new NakaRushtonRule().getName(), new AnnotatedPropertyEditor(new NakaRushtonRule()));
        RULE_MAP.put(new ProductRule().getName(), new AnnotatedPropertyEditor(new ProductRule()));
        RULE_MAP.put(new ContinuousSigmoidalRule().getName(),
                new AnnotatedPropertyEditor(new ContinuousSigmoidalRule()));
        RULE_MAP.put(new SigmoidalRule().getName(), new AnnotatedPropertyEditor(new SigmoidalRule()));
        RULE_MAP.put(new SpikingThresholdRule().getName(), new AnnotatedPropertyEditor(new SpikingThresholdRule()));
        RULE_MAP.put(new ThreeValueRule().getName(), new AnnotatedPropertyEditor(new ThreeValueRule()));
    }

    /**
     * Associations between names of activity generators and panels for editing
     * them.
     */
    public static final LinkedHashMap<String, AnnotatedPropertyEditor> GENERATOR_MAP = new LinkedHashMap<>();

    // Populate the Activity Generator Map. Note! Place items in alphabetical
    // order so they appear that way in the GUI combo box.
    static {
        GENERATOR_MAP.put(new LogisticRule().getName(), new AnnotatedPropertyEditor(new LogisticRule()));
        GENERATOR_MAP.put(new RandomNeuronRule().getName(), new AnnotatedPropertyEditor(new RandomNeuronRule()));
        GENERATOR_MAP.put(new SinusoidalRule().getName(), new AnnotatedPropertyEditor(new SinusoidalRule()));
        GENERATOR_MAP.put(new StochasticRule().getName(), new AnnotatedPropertyEditor(new StochasticRule()));
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

        startingPanel = neuronRulePanel;

        allNodesNoisy = neuronList.stream().allMatch(n -> n.getUpdateRule() instanceof NoisyUpdateRule);

        initializePanel();
        initializeLayout();
        addListeners();
    }

    /**
     * Initialize the main neuron panel based on whether all the neurons are the
     * same type or not.
     */
    private void initializePanel() {

        // TODO: Better handling of mixed case with activity generators.

        // Check whether the set of synapses being edited are of the
        // same type or not
        Iterator<Neuron> neuronIter = neuronList.iterator();
        Neuron neuronRef = neuronIter.next();
        boolean discrepancy = false;
        while (neuronIter.hasNext()) {
            if (!neuronRef.getUpdateRule().getClass().equals(neuronIter.next().getUpdateRule().getClass())) {
                discrepancy = true;
                break;
            }
        }

        // Create different panel elements depending on whether we are dealing with a consistent set
        if (discrepancy) {
            // If dealing with mixed types just use empty panels
            cbNeuronType.addItem(SimbrainConstants.NULL_STRING);
            cbNeuronType.setSelectedIndex(cbNeuronType.getItemCount() - 1);
            neuronRulePanel = new AnnotatedPropertyEditor(Collections.emptyList());
            noisePanel = new RandomizerPanel2(Collections.emptyList(), parent);
        } else {
            // If they are the same type, use the appropriate editor panel.
            // Later if ok is pressed the values from that panel will be written
            // to the rules
            String neuronName = neuronRef.getUpdateRule().getName();
            if (neuronList.get(0).getUpdateRule() instanceof ActivityGenerator) {
                neuronRulePanel = GENERATOR_MAP.get(neuronName);
            } else {
                neuronRulePanel = RULE_MAP.get(neuronName);
            }

            List<EditableObject> ruleList = neuronList.stream().map(Neuron::getUpdateRule).collect(Collectors.toList());
            neuronRulePanel.fillFieldValues(ruleList);

            if(allNodesNoisy) {
                randomizerList= ruleList.stream().
                    map(r -> (NoisyUpdateRule) r).
                    map(NoisyUpdateRule::getNoiseGenerator).
                    collect(Collectors.toList());
                noisePanel = new RandomizerPanel2(randomizerList, parent);
            }

            cbNeuronType.setSelectedItem(neuronName);
        }

    }


    /**
     * Lays out the components of the panel.
     */
    private void initializeLayout() {

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        // Top Panel that contains the combo box, the triangle
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.add(cbNeuronType);
        int horzStrut = 30;
        topPanel.add(Box.createHorizontalStrut(horzStrut)); // Create a minimum spacing
        // Give all extra space to the space between the components
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(neuronRulePanelTriangle);
        topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.setBorder(padding);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(topPanel);

        // Create the tabbed pane
        tabPane = new JTabbedPane();
        this.add(tabPane);
        tabPane.setVisible(neuronRulePanelTriangle.isDown()); // Set initial visibility

        // Main tab with neuron rule panel
        tabPane.addTab("Main",neuronRulePanel);
        neuronRulePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        neuronRulePanel.setBorder(padding);

        // Noise tab with randomizer panel
        tabPane.addTab("Noise", noisePanel);


        //Format the whole panel
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
                tabPane.setVisible(neuronRulePanelTriangle.isDown());
                repaint();
                parent.pack();
                parent.setLocationRelativeTo(null);
            }

        });

        // Change neuron type
        cbNeuronType.addActionListener(e -> {

            neuronRulePanel = ruleMap.get(cbNeuronType.getSelectedItem());

            // Is the current panel different from the starting panel?
            // If so we have to fill the new panel with default values
            boolean replaceUpdateRules = neuronRulePanel != startingPanel;
            if (replaceUpdateRules) {
                neuronRulePanel.fillDefaultValues();
//                if(allNodesNoisy) {
//                    noisePanel.fillDefaultValues();
//                }
            } else {
                // If not we can fill the new panel with values from the
                // neurons being edited.
                List<EditableObject> ruleList = neuronList.stream().map(Neuron::getUpdateRule).collect(Collectors.toList());
                neuronRulePanel.fillFieldValues(ruleList);
                if(allNodesNoisy) {
                    noisePanel.fillFieldValues(randomizerList);
                }
            }

            // Tell the panel whether it will have to replace neuron
            // update rules or edit them upon commit.
            // neuronRulePanel.setReplacingUpdateRules(replaceUpdateRules);

            repaintPanel();
            repaint();
            parent.pack();
        });

    }

    public void commitChanges() {

        NeuronUpdateRule selectedRule = (NeuronUpdateRule) neuronRulePanel.getEditedObject();

        // If an inconsistent set of objects is being edited return with no action
        if (selectedRule == null) {
            return;
        }

        for (Neuron n : neuronList) {
            // Only replace if this is a different rule (otherwise when
            // editing multiple rules with different parameter values which
            // have not been set those values will be replaced with the
            // default).
            if (!n.getUpdateRule().getClass().equals(selectedRule.getClass())) {
                n.setUpdateRule(selectedRule.deepCopy());
            }
        }

        List<EditableObject> ruleList = neuronList.stream().map(Neuron::getUpdateRule).collect(Collectors.toList());
        startingPanel = neuronRulePanel;
        neuronRulePanel.commitChanges(ruleList);

        // TODO: Not working
        // Also note that in case of discrepant neurons we should just ignore this
        if(noisePanel instanceof RandomizerPanel2) {
//            randomizerList = ruleList.stream().
//                map(r -> (NoisyUpdateRule) r).
//                map(NoisyUpdateRule::getNoiseGenerator).
//                map(Randomizer::getPdf).
//                collect(Collectors.toList());
//            ((RandomizerPanel2) noisePanel).commitChanges(randomizerList);
            ((RandomizerPanel2) noisePanel).commitChanges();
        }
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
     * @return the neuron update settings options combo box
     */
    public JComboBox<String> getCbNeuronType() {
        return cbNeuronType;
    }


}
