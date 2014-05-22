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
package org.simbrain.network.gui.dialogs.synapse;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.StaticSynapsePanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.EditablePanel;

/**
 * A panel for setting the synapse type and changing the parameters of the
 * selected update rule.
 *
 * * @author ztosi
 *
 */
@SuppressWarnings("serial")
public class SynapseUpdateSettingsPanel extends JPanel implements EditablePanel {

    /**
     * The default display state of the synapse panel. Currently, True, that is,
     * by default, the synapse panel corresponding to the rule in the combo box
     * is visible.
     */
    private static final boolean DEFAULT_SP_DISPLAY_STATE = true;

    /** Synapse type combo box. */
    private final JComboBox<String> cbSynapseType = new JComboBox<String>(
            AbstractSynapsePanel.getRuleList());

    /** The synapses being modified. */
    private final List<Synapse> synapseList;

    /** Synapse panel. */
    private AbstractSynapsePanel synapsePanel;

    /** For showing/hiding the synapse panel. */
    private final DropDownTriangle displaySPTriangle;

    /**
     * The originally displayed abstract synapse panel. If the currently
     * displayed panel is not the same as the starting panel, then we can be
     * sure that we are not editing synapse update rules, but rather are
     * replacing them.
     */
    private final AbstractSynapsePanel startingPanel;

    /**
     * A reference to the parent window, for resizing after panel content
     * changes.
     */
    private final Window parent;

    /**
     * Constructs a synapse update settings panel for a given synapse list and
     * within a specified parent window. Starts in the default display state for
     * the actual neuron update panel.
     *
     * @param synapseList the list of synapses which will be edited by the
     *            displayed neuron update rule panel
     * @param parent the swing window within which this panel will be placed.
     *            Here so that "pack()" can be called when this panel resizes
     *            itself.
     */
    public SynapseUpdateSettingsPanel(List<Synapse> synapseList,
            final Window parent) {
        this(synapseList, parent, DEFAULT_SP_DISPLAY_STATE);
    }

    /**
     * Constructs a synapse update settings panel for a given synapse list and
     * within a specified parent window, and with the starting display state of
     * the neuron update panel specified.
     *
     * @param synapseList the list of synapses which will be edited by the
     *            displayed neuron update rule panel
     * @param startingState whether or not the neuron update rule panel starts
     *            off displayed or hidden
     * @param parent the swing window within which this panel will be placed.
     *            Here so that "pack()" can be called when this panel resizes
     *            itself.
     */
    public SynapseUpdateSettingsPanel(List<Synapse> synapseList,
            final Window parent, boolean startingState) {
        this.synapseList = synapseList;
        this.parent = parent;
        displaySPTriangle = new DropDownTriangle(UpDirection.LEFT,
                startingState, "Settings", "Settings", parent);
        initSynapseType();
        startingPanel = synapsePanel;
        initializeLayout();
        addListeners();
    }

    /**
     * Lays out this panel.
     */
    private void initializeLayout() {

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        JPanel tPanel = new JPanel();
        tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.X_AXIS));
        tPanel.add(cbSynapseType);
        tPanel.add(Box.createHorizontalStrut(20));
        tPanel.add(displaySPTriangle);

        tPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tPanel.setBorder(padding);
        this.add(tPanel);

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        synapsePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        synapsePanel.setBorder(padding);
        synapsePanel.setVisible(displaySPTriangle.isDown());
        this.add(synapsePanel);

        TitledBorder tb2 = BorderFactory.createTitledBorder("Update Rule");
        this.setBorder(tb2);

    }

    /**
     * Adds the listeners to this dialog.
     */
    private void addListeners() {
        displaySPTriangle.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                synapsePanel.setVisible(displaySPTriangle.isDown());
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

        cbSynapseType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                synapsePanel = AbstractSynapsePanel.RULE_MAP.get(cbSynapseType
                        .getSelectedItem());

                // Is the current panel different from the starting panel?
                boolean replace = synapsePanel != startingPanel;

                if (replace) {
                    // If so we have to fill the new panel with default values
                    synapsePanel.fillDefaultValues();
                }

                // Tell the new panel whether it will have to replace
                // synapse update rules or edit them upon commit.
                synapsePanel.setReplace(replace);

                repaintPanel();
                parent.pack();

            }

        });

    }

    /**
     * Called to repaint the panel based on changes in the to the selected
     * synapse type.
     */
    public void repaintPanel() {
        removeAll();
        initializeLayout();
        repaint();
    }

    /**
     * Initialize the main synapse panel based on the type of the selected
     * synapses.
     */
    private void initSynapseType() {

        if (!NetworkUtils.isConsistent(synapseList, Synapse.class, "getType")) {
            cbSynapseType.addItem(SimbrainConstants.NULL_STRING);
            cbSynapseType.setSelectedIndex(cbSynapseType.getItemCount() - 1);
            // Simply to serve as an empty panel
            synapsePanel = new StaticSynapsePanel();
        } else {
            String synapseName = synapseList.get(0).getLearningRule()
                    .getDescription();
            synapsePanel = AbstractSynapsePanel.RULE_MAP.get(synapseName);
            synapsePanel.fillFieldValues(Synapse.getRuleList(synapseList));
            cbSynapseType.setSelectedItem(synapseName);
        }

    }

    /**
     * @return the name of the selected synapse update rule
     */
    public JComboBox<String> getCbSynapseType() {
        return cbSynapseType;
    }

    /**
     * @return the currently displayed synapse panel
     */
    public AbstractSynapsePanel getSynapsePanel() {
        return synapsePanel;
    }

    /**
     * @param synapsePanel set the currently displayed synapse panel to the
     *            specified panel
     */
    public void setSynapsePanel(AbstractSynapsePanel synapsePanel) {
        this.synapsePanel = synapsePanel;
    }

    @Override
    public void fillFieldValues() {
    }

    @Override
    public boolean commitChanges() {
        synapsePanel.commitChanges(synapseList);
        return true; // TODO:Finish implementation of CommittablePanel interface
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}
