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

import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ApplyPanel;
import org.simbrain.util.widgets.EditablePanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A panel allowing synapse learning rules for inhibitory and excitatory neurons
 * to be set within synapse groups.
 *
 * @author Zoë Tosi
 */
@SuppressWarnings("serial")
public class SynapsePropertiesPanel extends EditablePanel {

    /**
     * A synapse info panel containing basic synapse properties and synapse
     * update rule properties for excitatory synapses.
     */
    private AnnotatedPropertyEditor excitatoryInfoPanel;

    /**
     * A synapse info panel containing basic synapse properties and synapse
     * update rule properties for inhibitory synapses.
     */
    private AnnotatedPropertyEditor inhibitoryInfoPanel;

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


    /**
     * The apply panel associated with using {@link #excitatoryInfoPanel} for
     * editing.
     */
    private ApplyPanel exApplyPanel;

    /**
     * The apply panel associated with using {@link #inhibitoryInfoPanel} for
     * editing.
     */
    private ApplyPanel inApplyPanel;

    /**
     * The synapse group being edited. Null, if editing loose synapses.
     */
    private final SynapseGroup synapseGroup;

    /**
     * The main panel which contains the other panels.
     */
    private final JPanel mainPanel = new JPanel();

    /**
     * Whether or not this is a creation panel.
     */
    private final boolean creationPanel;

    /**
     * @param parent       parent panel
     * @param synapseGroup the synapse group whose connection object is set
     * @return the new synapse connection
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(final Window parent, final SynapseGroup synapseGroup) {
        SynapsePropertiesPanel spp = new SynapsePropertiesPanel(parent, synapseGroup);
        if (!spp.creationPanel) {
            spp.initApplyListeners();
        }
        return spp;
    }

    /**
     * @param parent
     * @return
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(final Window parent) {
        SynapsePropertiesPanel spp = new SynapsePropertiesPanel(parent);
        if (!spp.creationPanel) {
            spp.initApplyListeners();
        }
        return spp;
    }

    /**
     * @param parentWindow
     * @param synapseGroup
     */
    private SynapsePropertiesPanel(final Window parentWindow, final SynapseGroup synapseGroup) {
        this.synapseGroup = synapseGroup;
        creationPanel = synapseGroup.isEmpty();
        templateExcitatorySynapse = synapseGroup.getExcitatoryPrototype();
        templateInhibitorySynapse = synapseGroup.getInhibitoryPrototype();
        List<Synapse> excitatorySynapses;
        List<Synapse> inhibitorySynapses;
        if (!synapseGroup.hasExcitatory() || synapseGroup.isUseGroupLevelSettings()) {
            excitatorySynapses = Collections.singletonList(templateExcitatorySynapse);
            double exDelay = templateExcitatorySynapse.getDelay();
            for (Synapse s : synapseGroup.getExcitatorySynapses()) {
                if (s.getDelay() != exDelay) {
                    templateExcitatorySynapse.setDelay(-1);
                    break;
                }
            }
        } else {
            excitatorySynapses = new ArrayList(synapseGroup.getExcitatorySynapses());
            // Ensures that the template is also edited
            excitatorySynapses.add(templateExcitatorySynapse);
        }
        if (!synapseGroup.hasInhibitory() || synapseGroup.isUseGroupLevelSettings()) {
            inhibitorySynapses = Collections.singletonList(templateInhibitorySynapse);
            double inDelay = templateInhibitorySynapse.getDelay();
            for (Synapse s : synapseGroup.getInhibitorySynapses()) {
                if (s.getDelay() != inDelay) {
                    templateInhibitorySynapse.setDelay(-1);
                    break;
                }
            }
        } else {
            inhibitorySynapses = new ArrayList(synapseGroup.getInhibitorySynapses());
            // Ensures that the template is also edited
            inhibitorySynapses.add(templateInhibitorySynapse);
        }
        excitatoryInfoPanel = new AnnotatedPropertyEditor(excitatorySynapses);
        inhibitoryInfoPanel = new AnnotatedPropertyEditor(inhibitorySynapses);
        init();
    }

    /**
     * @param parentWindow
     * @param synapses
     */
    private SynapsePropertiesPanel(final Window parentWindow, final Collection<Synapse> synapses) {
        synapseGroup = null;
        templateExcitatorySynapse = Synapse.getTemplateSynapse();
        templateInhibitorySynapse = Synapse.getTemplateSynapse();
        creationPanel = synapses.isEmpty();
        List<Synapse> excitatorySynapses;
        List<Synapse> inhibitorySynapses;
        if (!creationPanel) {
            excitatorySynapses = ConnectionUtilities.getExcitatorySynapses(synapses);
            inhibitorySynapses = ConnectionUtilities.getInhibitorySynapses(synapses);
        } else {
            excitatorySynapses = Collections.singletonList(getTemplateExcitatorySynapse());
            inhibitorySynapses = Collections.singletonList(getTemplateInhibitorySynapse());
        }
        excitatoryInfoPanel = new AnnotatedPropertyEditor(excitatorySynapses);
        inhibitoryInfoPanel = new AnnotatedPropertyEditor(inhibitorySynapses);
        init();
    }

    /**
     * A constructor specifically for creating loose connections.
     *
     * @param parentWindow
     */
    private SynapsePropertiesPanel(final Window parentWindow) {
        synapseGroup = null;
        creationPanel = true;
        templateExcitatorySynapse = Synapse.getTemplateSynapse();
        templateInhibitorySynapse = Synapse.getTemplateSynapse();
        templateExcitatorySynapse.setStrength(ConnectionUtilities.DEFAULT_EXCITATORY_STRENGTH);
        templateInhibitorySynapse.setStrength(ConnectionUtilities.DEFAULT_INHIBITORY_STRENGTH);
        List<Synapse> excitatorySynapses;
        List<Synapse> inhibitorySynapses;
        excitatorySynapses = Collections.singletonList(getTemplateExcitatorySynapse());
        inhibitorySynapses = Collections.singletonList(getTemplateInhibitorySynapse());
        excitatoryInfoPanel = new AnnotatedPropertyEditor(excitatorySynapses);
        inhibitoryInfoPanel = new AnnotatedPropertyEditor(inhibitorySynapses);
        init();
    }

    /**
     * Initializes/Lays out the panel.
     */
    private void init() {

        // Excitatory Border
        Border redBorder = BorderFactory.createLineBorder(Color.RED);
        Border exBorder = BorderFactory.createTitledBorder(redBorder, "Excitatory");

        // Inhibitory Border
        Border blueBorder = BorderFactory.createLineBorder(Color.BLUE);
        Border inBorder = BorderFactory.createTitledBorder(blueBorder, "Inhibitory");

        // Layout panels
        BoxLayout bxLayout = new BoxLayout(mainPanel, BoxLayout.X_AXIS);
        mainPanel.setLayout(bxLayout);

        // Layout inhibitory panel
        if (creationPanel) { // No apply button, creating...
            Box inBox = Box.createVerticalBox();
            inBox.setAlignmentY(Component.TOP_ALIGNMENT);
            inBox.add(inhibitoryInfoPanel);
            inBox.add(Box.createVerticalGlue());
            inBox.setBorder(inBorder);
            mainPanel.add(inBox);
        } else { // Apply button/panel, editing...
            inApplyPanel = ApplyPanel.createApplyPanel(inhibitoryInfoPanel);
            inApplyPanel.setBorder(inBorder);
            mainPanel.add(inApplyPanel);
        }

        // Create a space between the inhibitory and excitatory panels
        mainPanel.add(Box.createHorizontalGlue(), bxLayout);
        mainPanel.add(Box.createHorizontalStrut(10), bxLayout);

        // Layout excitatory panel
        if (creationPanel) { // No apply button, creating...
            Box exBox = Box.createVerticalBox();
            exBox.setAlignmentY(Component.TOP_ALIGNMENT);
            exBox.add(excitatoryInfoPanel);
            exBox.add(Box.createVerticalGlue());
            exBox.setBorder(exBorder);
            mainPanel.add(exBox);
        } else { // Apply button/panel, editing...
            exApplyPanel = ApplyPanel.createApplyPanel(excitatoryInfoPanel);
            exApplyPanel.setBorder(exBorder);
            mainPanel.add(exApplyPanel);
        }

        // Add the main panel to this panel
        this.add(mainPanel);

    }

    /**
     * Initializes the listeners associated with the apply buttons for editing.
     */
    private void initApplyListeners() {
        exApplyPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                excitatoryInfoPanel.commitChanges();
                if (synapseGroup.isUseGroupLevelSettings()) {
                    synapseGroup.setAndConformToTemplate(templateExcitatorySynapse, Polarity.EXCITATORY);
                    //                    double exStr = excitatoryInfoPanel.getStrength();
                    //                    if (!Double.isNaN(exStr)) {
                    //                        synapseGroup.setStrength(exStr, Polarity.EXCITATORY);
                    //                    }
                }
            }
        });
        inApplyPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inhibitoryInfoPanel.commitChanges();
                if (synapseGroup.isUseGroupLevelSettings()) {
                    synapseGroup.setAndConformToTemplate(templateInhibitorySynapse, Polarity.INHIBITORY);
                    //                    double inStr = inhibitoryInfoPanel.getStrength();
                    //                    if (!Double.isNaN(inStr)) {
                    //                        synapseGroup.setStrength(inStr, Polarity.INHIBITORY);
                    //                    }
                }

            }
        });
    }

    /**
     * Add a special action to the "apply" action for editing excitatory
     * synapses.
     *
     * @param l
     */
    public void addApplyListenerEx(ActionListener l) {
        if(exApplyPanel != null) {
            exApplyPanel.addActionListener(l);
        }
    }

    /**
     * Add a special action to the "apply" action for editing inhibitory
     * synapses.
     *
     * @param l
     */
    public void addApplyListenerIn(ActionListener l) {
        if(inApplyPanel != null) {
            inApplyPanel.addActionListener(l);
        }
    }

    @Override
    public boolean commitChanges() {
        boolean success = true;
        success &= excitatoryInfoPanel.commitChanges();
        success &= inhibitoryInfoPanel.commitChanges();
        if (synapseGroup != null) {
            synapseGroup.setAndConformToTemplate(templateExcitatorySynapse, Polarity.EXCITATORY);
            synapseGroup.setAndConformToTemplate(templateInhibitorySynapse, Polarity.INHIBITORY);
        }
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
        if (synapseGroup != null) {
            excitatoryInfoPanel.fillFieldValues();
            inhibitoryInfoPanel.fillFieldValues();
        }
    }

    /**
     * @return the template excitatory synapse, used to store parameters from
     * this panel for creation, when no synapses exist yet to edit.
     */
    public Synapse getTemplateExcitatorySynapse() {
        return templateExcitatorySynapse;
    }

    /**
     * @return the template inhibitory synapse, used to store parameters from
     * this panel for creation, when no synapses exist yet to edit.
     */
    public Synapse getTemplateInhibitorySynapse() {
        return templateInhibitorySynapse;
    }

}
