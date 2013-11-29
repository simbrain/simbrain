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
import java.awt.FlowLayout;
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
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;

@SuppressWarnings("serial")
public class SynapseUpdateSettingsPanel extends JPanel {

    /** Null string. */
    public static final String NULL_STRING = "...";

    /**
     * The default display state of the synapse panel. Currently, True, that is,
     * by default, the synapse panel corresponding to the rule in the combo box
     * is visible.
     */
    private static final boolean DEFAULT_SP_DISPLAY_STATE = true;

    /** Synapse type combo box. */
    private final JComboBox<String> cbSynapseType =
            new JComboBox<String>(AbstractSynapsePanel.getRuleList());

    /** The synapses being modified. */
    private final List<Synapse> synapseList;

    /** Synapse panel. */
    private AbstractSynapsePanel synapsePanel;

    /** For showing/hiding the synapse panel. */
    private final DropDownTriangle displaySPTriangle;

    private final AbstractSynapsePanel startingPanel;

    /**
     * A reference to the parent window, for resizing after panel content
     * changes.
     */
    private final Window parent;

    /**
     * 
     * @param synapseList
     */
    public SynapseUpdateSettingsPanel(List<Synapse> synapseList,
            final Window parent) {
        this(synapseList, DEFAULT_SP_DISPLAY_STATE, parent);
    }

    /**
     * 
     * @param synapseList
     * @param startingState
     */
    public SynapseUpdateSettingsPanel(List<Synapse> synapseList,
            boolean startingState, final Window parent) {
        this.synapseList = synapseList;
        this.parent = parent;
        displaySPTriangle =
                new DropDownTriangle(UpDirection.LEFT, startingState,
                        "Settings", "Settings", parent);
        initSynapseType();
        startingPanel = synapsePanel;
        initializeLayout();
        addListeners();
    }

    /**
     * Lays out this panel.
     * 
     * @return
     */
    private void initializeLayout() {

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        JPanel tPanel = new JPanel();
        tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.X_AXIS));
        tPanel.add(cbSynapseType);
        tPanel.add(Box.createHorizontalStrut(20));

        JPanel supP = new JPanel(new FlowLayout());
        supP.add(displaySPTriangle);

        tPanel.add(supP);
        tPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tPanel.setBorder(padding);
        this.add(tPanel);

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        synapsePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        synapsePanel.setBorder(padding);
        synapsePanel.setVisible(displaySPTriangle.isDown());
        this.add(synapsePanel);

        TitledBorder tb2 =
                BorderFactory.createTitledBorder("Update Rule");
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
                synapsePanel =
                        AbstractSynapsePanel.RULE_MAP.get(cbSynapseType
                                .getSelectedItem());

                // Is the current panel different from the starting panel?
                boolean replace = synapsePanel != startingPanel;

                if(replace) {
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

        if (!NetworkUtils.isConsistent(synapseList, Synapse.class,
                "getType")) {
            cbSynapseType.addItem(AbstractSynapsePanel.NULL_STRING);
            cbSynapseType
                    .setSelectedIndex(cbSynapseType.getItemCount() - 1);
            // Simply to serve as an empty panel
            synapsePanel = new StaticSynapsePanel();
        } else {
            String synapseName =
                    synapseList.get(0).getLearningRule().getDescription();
            synapsePanel = AbstractSynapsePanel.RULE_MAP.get(synapseName);
            synapsePanel
                    .fillFieldValues(Synapse.getRuleList(synapseList));
            cbSynapseType.setSelectedItem(synapseName);
        }

    }

    /**
     * 
     * @return the name of the selected synapse update rule
     */
    public JComboBox<String> getCbSynapseType() {
        return cbSynapseType;
    }

    /**
     * 
     * @return the currently displayed synapse panel
     */
    public AbstractSynapsePanel getSynapsePanel() {
        return synapsePanel;
    }

    /**
     * 
     * @param synapsePanel
     *            set the currently displayed synapse panel to the specified
     *            panel
     */
    public void setSynapsePanel(AbstractSynapsePanel synapsePanel) {
        this.synapsePanel = synapsePanel;
    }

}
