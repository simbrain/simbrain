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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.EditablePanel;

/**
 * A panel for setting the neuron type and changing the parameters of the
 * selected update rule.
 *
 * @author ztosi
 * @author jyoshimi
 */
@SuppressWarnings("serial")
public class NeuronUpdateSettingsPanel extends JPanel implements EditablePanel {

    /** Null string. */
    public static final String NULL_STRING = "...";

    /**
     * The default display state of the neuron panel. Currently, True, that is,
     * by default, the neuron panel corresponding to the rule in the combo box
     * is visible.
     */
    private static final boolean DEFAULT_NP_DISPLAY_STATE = true;

    /** Neuron type combo box. */
    private final JComboBox<String> cbNeuronType;

    /** Associations between names of rules and panels for editing them. */
    private final LinkedHashMap<String, AbstractNeuronPanel> ruleMap;

    /** The neurons being modified. */
    private final List<Neuron> neuronList;

    /** Neuron panel. */
    private AbstractNeuronPanel neuronPanel;

    /** For showing/hiding the neuron panel. */
    public final DropDownTriangle displayNPTriangle;

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
    private final AbstractNeuronPanel startingPanel;

    /**
     * Create a the panel with the default starting visibility (visible) for the
     * neuron panel.
     *
     * @param neuronList
     *            the list of neurons being edited
     * @param parent
     *            the parent window referenced for resizing purposes
     */
    public NeuronUpdateSettingsPanel(List<Neuron> neuronList, Window parent) {
        this(neuronList, parent, DEFAULT_NP_DISPLAY_STATE);
    }

    /**
     * Create the panel with specified starting visibility.
     *
     * @param neuronList
     *            the list of neurons being edited
     * @param parent
     *            the parent window referenced for resizing purposes
     * @param startingState
     *            the starting state of whether or not details of the rule are
     *            initially visible
     */
    public NeuronUpdateSettingsPanel(List<Neuron> neuronList, Window parent,
            boolean startingState) {
        this.neuronList = neuronList;
        this.parent = parent;
        ruleMap = AbstractNeuronPanel.RULE_MAP;
        cbNeuronType = new JComboBox<String>(AbstractNeuronPanel.getRulelist());
        displayNPTriangle = new DropDownTriangle(UpDirection.LEFT,
                startingState, "Settings", "Settings", parent);
        initNeuronType();
        startingPanel = neuronPanel;
        initializeLayout();
        addListeners();
    }

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

        tPanel.add(displayNPTriangle);
        tPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tPanel.setBorder(padding);
        this.add(tPanel);

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        neuronPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        neuronPanel.setBorder(padding);
        neuronPanel.setVisible(displayNPTriangle.isDown());
        this.add(neuronPanel);

        TitledBorder tb2 = BorderFactory.createTitledBorder("Update Rule");
        this.setBorder(tb2);

    }

    /**
     * Adds the listeners to this dialog.
     */
    private void addListeners() {

        displayNPTriangle.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                neuronPanel.setVisible(displayNPTriangle.isDown());
                repaint();
                parent.pack();
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }

        });

        cbNeuronType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                neuronPanel = ruleMap.get(cbNeuronType.getSelectedItem());

                // Is the current panel different from the starting panel?
                boolean replace = neuronPanel != startingPanel;

                // If so we have to fill the new panel with default values
                if (replace) {
                    neuronPanel.fillDefaultValues();
                } else {
                    // If not we can fill the new panel with values from the
                    // neurons being edited.
                    neuronPanel.fillFieldValues(Neuron.getRuleList(neuronList));
                }

                // Tell the panel whether it will have to replace neuron
                // update rules or edit them upon commit.
                neuronPanel.setReplace(replace);

                repaintPanel();
                repaint();
                parent.pack();
            }

        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean commitChanges() {
        neuronPanel.commitChanges(neuronList);
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
     * Initialize the main neuron panel based on the type of the selected
     * neurons.
     */
    private void initNeuronType() {

        if (!NetworkUtils.isConsistent(neuronList, Neuron.class, "getType")) {
            cbNeuronType.addItem(SimbrainConstants.NULL_STRING);
            cbNeuronType.setSelectedIndex(cbNeuronType.getItemCount() - 1);
            neuronPanel = new EmptyRulePanel();
        } else {
            String neuronName = neuronList.get(0).getUpdateRule()
                    .getDescription();
            neuronPanel = ruleMap.get(neuronName);
            neuronPanel.setReplace(false);
            neuronPanel.fillFieldValues(Neuron.getRuleList(neuronList));
            cbNeuronType.setSelectedItem(neuronName);

        }
    }

    /**
     * Directly access the neuron panel to utilize its methods without using
     * this class as an intermediary. An example of this can be seen in
     *
     * @see org.simbrain.network.gui.dialogs.AddNeuronsDialog.java
     * @return the currently displayed neuron update rule panel
     */
    public AbstractNeuronPanel getNeuronPanel() {
        return neuronPanel;
    }

    /**
     * @param neuronPanel
     *            the desired neuron update rule panel to be displayed
     */
    public void setNeuronPanel(AbstractNeuronPanel neuronPanel) {
        this.neuronPanel = neuronPanel;
    }

    /**
     * @return the neuron update settings options combo box
     */
    public JComboBox<String> getCbNeuronType() {
        return cbNeuronType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getPanel() {
        return this;
    }

    /**
     * An empty panel displayed in cases where the selected neurons have more
     * than one type of update rule.
     *
     * @author ztosi
     */
    private class EmptyRulePanel extends AbstractNeuronPanel {

        @Override
        public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        }

        @Override
        public void fillDefaultValues() {
        }

        @Override
        public void commitChanges(Neuron neuron) {
        }

        @Override
        public void commitChanges(List<Neuron> neuron) {
        }

        @Override
        public NeuronUpdateRule getPrototypeRule() {
            return null;
        }

        @Override
        protected void writeValuesToRules(List<Neuron> neurons) {
        }

    }

    @Override
    public void fillFieldValues() {
    }

}
