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
package org.simbrain.network.gui.dialogs.connect;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.dialogs.synapse.CombinedSynapseInfoPanel;
import org.simbrain.util.widgets.EditablePanel;

/**
 * A panel allowing synapse learning rules to be set and random weights to be
 * activated/adjusted, designed with segregation of inhibitory and excitatory
 * weights in mind.
 * 
 * @author Zach Tosi
 * 
 */
@SuppressWarnings("serial")
public class SynapsePropertiesPanel extends JPanel implements EditablePanel {

    /**
     * A synapse info panel containing basic synapse properties and synapse
     * update rule properties for excitatory synapses.
     */
    private CombinedSynapseInfoPanel excitatoryInfoPanel;

    /**
     * A synapse info panel containing basic synapse properties and synapse
     * update rule properties for inhibitory synapses.
     */
    private CombinedSynapseInfoPanel inhibitoryInfoPanel;

    /** The apply button for editing associated with excitatory synapses. */
    private JButton exApplyButton = new JButton("Apply");

    /** The apply button for editing associated with inhibitory synapses. */
    private JButton inApplyButton = new JButton("Apply");

    /**
     * A template excitatory synapse used to store committed information if a
     * synapse group doesn't have any connections yet.
     */
    private final Synapse templateExcitatorySynapse;

    /**
     * A template inhibitory synapse used to store committed information if a
     * synapse group doesn't have any connections yet.
     */
    private final Synapse templateInhibitorySynapse;

    /** The synapse group being edited. Null, if editing loose synapses. */
    private final SynapseGroup synapseGroup;

    /** The main panel which contains the other panels. */
    private final JPanel mainPanel = new JPanel();

    /** Whether or not this is a creation panel. */
    private final boolean creationPanel;

    /**
     * 
     * @param parent
     * @param synapseGroup
     * @return
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(
        final Window parent, final SynapseGroup synapseGroup) {
        SynapsePropertiesPanel spp = new SynapsePropertiesPanel(parent,
            synapseGroup);
        spp.initApplyListeners();
        return spp;
    }

    /**
     * 
     * @param parent
     * @param synapses
     * @return
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(
        final Window parent, final Collection<Synapse> synapses) {
        SynapsePropertiesPanel spp = new SynapsePropertiesPanel(parent,
            synapses);
        spp.initApplyListeners();
        return spp;
    }

    /**
     * 
     * @param parent
     * @param synapses
     * @return
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(
        final Window parent) {
        SynapsePropertiesPanel spp = new SynapsePropertiesPanel(parent);
        spp.initApplyListeners();
        return spp;
    }

    /**
     * 
     * @param parentWindow
     * @param synapseGroup
     */
    private SynapsePropertiesPanel(final Window parentWindow,
        final SynapseGroup synapseGroup) {
        this.synapseGroup = synapseGroup;
        creationPanel = synapseGroup.isEmpty();
        templateExcitatorySynapse = synapseGroup.getExcitatoryPrototype();
        templateInhibitorySynapse = synapseGroup.getInhibitoryPrototype();
        Set<Synapse> excitatorySynapses;
        Set<Synapse> inhibitorySynapses;
        excitatorySynapses = synapseGroup.hasExcitatory()
            ? synapseGroup.getExcitatorySynapses()
            : Collections.singleton(synapseGroup.getExcitatoryPrototype());
        inhibitorySynapses = synapseGroup.hasInhibitory()
            ? synapseGroup.getInhibitorySynapses()
            : Collections.singleton(synapseGroup.getInhibitoryPrototype());
        excitatoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(excitatorySynapses, parentWindow);
        inhibitoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(inhibitorySynapses, parentWindow);
        init();
    }

    /**
     * 
     * @param parentWindow
     * @param synapses
     */
    private SynapsePropertiesPanel(final Window parentWindow,
        final Collection<Synapse> synapses) {
        synapseGroup = null;
        templateExcitatorySynapse = Synapse.getTemplateSynapse();
        templateInhibitorySynapse = Synapse.getTemplateSynapse();
        creationPanel = synapses.isEmpty();
        Collection<Synapse> excitatorySynapses;
        Collection<Synapse> inhibitorySynapses;
        if (!creationPanel) {
            excitatorySynapses = ConnectionUtilities
                .getExcitatorySynapses(synapses);
            inhibitorySynapses = ConnectionUtilities
                .getInhibitorySynapses(synapses);
        } else {
            excitatorySynapses = Collections.singleton(
                getTemplateExcitatorySynapse());
            inhibitorySynapses = Collections.singleton(
                getTemplateInhibitorySynapse());
        }
        excitatoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(excitatorySynapses, parentWindow);
        inhibitoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(inhibitorySynapses, parentWindow);
        init();
    }

    /**
     * A constructor specifically for creating loose connections.
     * 
     * @param parentWindow
     * @param synapses
     */
    private SynapsePropertiesPanel(final Window parentWindow) {
        synapseGroup = null;
        creationPanel = true;
        templateExcitatorySynapse = Synapse.getTemplateSynapse();
        templateInhibitorySynapse = Synapse.getTemplateSynapse();
        templateExcitatorySynapse.setStrength(
            ConnectionUtilities.DEFAULT_EXCITATORY_STRENGTH);
        templateInhibitorySynapse.setStrength(
            ConnectionUtilities.DEFAULT_INHIBITORY_STRENGTH);
        Collection<Synapse> excitatorySynapses;
        Collection<Synapse> inhibitorySynapses;
        excitatorySynapses = Collections.singleton(
            getTemplateExcitatorySynapse());
        inhibitorySynapses = Collections.singleton(
            getTemplateInhibitorySynapse());
        excitatoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(excitatorySynapses, parentWindow);
        inhibitoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(inhibitorySynapses, parentWindow);
        init();
    }

    /**
     * Initializes/Lays out the panel.
     */
    private void init() {
        // Excitatory Border
        Border redBorder =
            BorderFactory.createLineBorder(Color.RED);
        Border exBorder = BorderFactory.createTitledBorder(redBorder,
            "Excitatory");

        // Inhibitory Border
        Border blueBorder =
            BorderFactory.createLineBorder(Color.BLUE);
        Border inBorder = BorderFactory.createTitledBorder(blueBorder,
            "Inhibitory");

        // Layout panels
        BoxLayout bxLayout = new BoxLayout(mainPanel, BoxLayout.X_AXIS);
        mainPanel.setLayout(bxLayout);

        Box inBox = Box.createVerticalBox();
        inBox.setAlignmentY(Component.TOP_ALIGNMENT);
        inBox.add(inhibitoryInfoPanel);
        JPanel inApButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        inApButtonPanel.add(inApplyButton);
        inBox.add(Box.createVerticalGlue());
        inBox.add(inApButtonPanel);
        inApButtonPanel.setVisible(!creationPanel);

        inBox.add(new JPanel());
        inBox.setBorder(inBorder);

        Box exBox = Box.createVerticalBox();
        exBox.setAlignmentY(Component.TOP_ALIGNMENT);
        exBox.add(excitatoryInfoPanel);
        JPanel exApButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        exApButtonPanel.add(exApplyButton);
        exBox.add(Box.createVerticalGlue());
        exBox.add(exApButtonPanel);
        exApButtonPanel.setVisible(!creationPanel);
        exBox.add(new JPanel());
        exBox.setBorder(exBorder);

        mainPanel.add(inBox);
        mainPanel.add(Box.createHorizontalGlue(), bxLayout);
        mainPanel.add(Box.createHorizontalStrut(10), bxLayout);
        mainPanel.add(exBox);
        this.add(mainPanel);

    }

    /**
     * Initializes the listeners associated with the apply buttons for editing.
     */
    private void initApplyListeners() {
        exApplyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                excitatoryInfoPanel.commitChanges();
                double exStrength = excitatoryInfoPanel.getStrength();
                if (!Double.isNaN(exStrength) && synapseGroup != null) {
                    synapseGroup.setAllExcitatoryStrengths(exStrength);
                }
            }
        });
        inApplyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inhibitoryInfoPanel.commitChanges();
                double inStrength = inhibitoryInfoPanel.getStrength();
                if (!Double.isNaN(inStrength) && synapseGroup != null) {
                    synapseGroup.setAllInhibitoryStrengths(inStrength);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public boolean commitChanges() {
        boolean success = true;
        success &= excitatoryInfoPanel.commitChanges();
        success &= inhibitoryInfoPanel.commitChanges();
        // Ensure that strengths have been set within appropriate boundaries...
        templateExcitatorySynapse.setStrength( // Always positive
            Math.abs(templateExcitatorySynapse.getStrength()));
        templateInhibitorySynapse.setStrength( // Always negative
            -Math.abs(templateInhibitorySynapse.getStrength()));
        return success;
    }

    /**
     * Does nothing. This panel contains two CombinedSynapseInfo panels, both of
     * which call their own fillFieldValues() method in their constructors.
     * Since there are no fields outside these panels in this panel
     */
    @Override
    public void fillFieldValues() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * @return the template excitatory synapse, used to store parameters from
     *         this panel for creation, when no synapses exist yet to edit.
     */
    public Synapse getTemplateExcitatorySynapse() {
        return templateExcitatorySynapse;
    }

    /**
     * @return the template inhibitory synapse, used to store parameters from
     *         this panel for creation, when no synapses exist yet to edit.
     */
    public Synapse getTemplateInhibitorySynapse() {
        return templateInhibitorySynapse;
    }

}
