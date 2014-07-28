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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.EditablePanel;

/**
 * The basic neuron info panel and neuron update settings panel are frequently
 * used together and depend on each other. This panel combines the two and
 * handles changes to one being applied to the other.
 *
 * @author ztosi
 */
@SuppressWarnings("serial")
public class NeuronPropertiesPanel extends JPanel implements EditablePanel {

    /**
     * The default vertical gap between the basic neuron info panel and the
     * neuron update settings panel.
     */
    private static final int DEFAULT_VGAP = 10;

    /**
     * The default initial display state of the neuron update setting panel's
     * neuron update rule panel.
     */
    private static boolean DEFAULT_NUSP_DISPLAY_STATE;

    /** Static initializer */
    static {
        Properties properties = Utils.getSimbrainProperties();
        if (properties.containsKey("useNativeFileChooser")) {
            DEFAULT_NUSP_DISPLAY_STATE = Boolean.parseBoolean(properties
                .getProperty("initializeNeuronDialogToExpandedState"));
        }

    }

    /** The basic neuron info panel. */
    private NeuronPropertiesSimple neuronInfoPanel;

    /** The neuron update settings panel. */
    private SpecificNeuronRulePanel updateInfoPanel;

    /**
     * Creates a combined neuron info panel, which includes the basic neuron
     * info panel and a neuron update settings panel. The panel is automatically
     * built and laid out, such that it is immediately ready for display.
     *
     * @param neuronList
     *            the list of neurons either being edited (editing) or being
     *            used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     */
    public static NeuronPropertiesPanel createCombinedNeuronInfoPanel(
        final List<Neuron> neuronList, final Window parent) {
        return createCombinedNeuronInfoPanel(neuronList, parent,
            DEFAULT_NUSP_DISPLAY_STATE);
    }

    /**
     * Creates a combined neuron info panel, which includes the basic neuron
     * info panel and a neuron update settings panel. The panel is automatically
     * built and laid out, such that it is immediately ready for display. The
     * nusp (neuron update settings panel)'s display state is that extra data is
     * by default hidden.
     *
     * @param neuronList
     *            the list of neurons either being edited (editing) or being
     *            used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     * @param nuspExtendedDisplay
     *            whether or not to display the neuron update rule's details
     *            initially
     */
    public static NeuronPropertiesPanel createCombinedNeuronInfoPanel(
        final List<Neuron> neuronList, final Window parent,
        final boolean nuspExtendedDisplay) {
        NeuronPropertiesPanel cnip = new NeuronPropertiesPanel(neuronList,
            parent, nuspExtendedDisplay);
        cnip.initializeLayout();
        cnip.addListeners();
        return cnip;
    }

    /**
     * Creates a combined neuron info panel, which includes the basic neuron
     * info panel and a neuron update settings panel. The panel is automatically
     * built and laid out, such that it is immediately ready for display. The
     * nusp (neuron update settings panel)'s display state is that extra data is
     * by default hidden. It is incomprehensible to set displayIDInfo to true if
     * multiple neurons are going to be displayed. This is here for cases where
     * the number of neurons is itself a variable and normally ID information
     * would be displayed because there is only one neuron in the list.
     *
     * @param neuronList
     *            the list of neurons either being edited (editing) or being
     *            used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     * @param nuspExtendedDisplay
     *            whether or not to display the neuron update
     * @param displayIDInfo
     *            manually sets whether or not neuron id information is
     *            displayed. rule's details initially
     */
    public static NeuronPropertiesPanel createCombinedNeuronInfoPanel(
        final List<Neuron> neuronList, final Window parent,
        final boolean nuspExtendedDisplay, final boolean displayIDInfo) {
        NeuronPropertiesPanel cnip = new NeuronPropertiesPanel(neuronList,
            parent, nuspExtendedDisplay, displayIDInfo);
        cnip.initializeLayout();
        cnip.addListeners();
        return cnip;
    }

    /**
     * {@link #createCombinedNeuronInfoPanel(List, Window, boolean)}
     *
     * @param neuronList
     *            the list of neurons either being edited (editing) or being
     *            used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     * @param nuspExtendedDisplay
     *            whether or not to display the neuron update rule's details
     *            initially
     */
    private NeuronPropertiesPanel(final List<Neuron> neuronList,
        final Window parent, final boolean nuspExtendedDisplay) {
        neuronInfoPanel = NeuronPropertiesSimple.createBasicNeuronInfoPanel(
            neuronList, parent);
        updateInfoPanel = new SpecificNeuronRulePanel(neuronList, parent,
            nuspExtendedDisplay);
    }

    /**
     * {@link #createCombinedNeuronInfoPanel(List, Window, boolean, boolean)}
     *
     * @param neuronList
     *            the list of neurons either being edited (editing) or being
     *            used to fill the panel with default values (creation).
     * @param parent
     *            the parent window, made available for easy resizing.
     * @param nuspExtendedDisplay
     *            whether or not to display the neuron update
     * @param displayIDInfo
     *            manually sets whether or not neuron id information is
     *            displayed. rule's details initially
     */
    private NeuronPropertiesPanel(final List<Neuron> neuronList,
        final Window parent, final boolean nuspExtendedDisplay,
        final boolean displayIDInfo) {
        neuronInfoPanel = NeuronPropertiesSimple.createBasicNeuronInfoPanel(
            neuronList, parent, displayIDInfo);
        updateInfoPanel = new SpecificNeuronRulePanel(neuronList, parent,
            nuspExtendedDisplay);
    }

    /**
     * Lays out the panel.
     */
    private void initializeLayout() {
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);
        this.add(neuronInfoPanel);
        this.add(Box.createVerticalStrut(DEFAULT_VGAP));
        this.add(updateInfoPanel);
    }

    /**
     * Add listeners to the components of the panel. Specifically, adds a
     * listener so that the basic neuron info panel can alter itself to adhere
     * to the properties of the currently selected neuron update rule in
     * {@link #updateInfoPanel}.
     */
    private void addListeners() {
        updateInfoPanel.getCbNeuronType().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            AbstractNeuronRulePanel np = updateInfoPanel
                                .getNeuronPanel();
                            neuronInfoPanel.getExtraDataPanel()
                                .updateFieldVisibility(
                                    np.getPrototypeRule());
                            repaint();
                        }
                    });
                }
            });
    }

    /**
     * {@inheritDoc} <b>Specifically:</b> Commits changes in the basic neuron
     * info panel and the neuron update settings panel.
     */
    @Override
    public boolean commitChanges() {

        boolean success = true;

        // Commit changes specific to the neuron type
        // This must be the first change committed, as other neuron panels
        // make assumptions about the type of the neuron update rule being
        // edited that can result in ClassCastExceptions otherwise.
        success &= updateInfoPanel.commitChanges();

        success &= neuronInfoPanel.commitChanges();

        return success;

    }

    /*
     * /CHECKSTYLE:OFF************************************** Getters and Setters
     * *****************************************************
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getPanel() {
        return this;
    }

    public NeuronPropertiesSimple getNeuroninfoPanel() {
        return neuronInfoPanel;
    }

    public void setNeuroninfoPanel(NeuronPropertiesSimple neuroninfoPanel) {
        this.neuronInfoPanel = neuroninfoPanel;
    }

    public SpecificNeuronRulePanel getUpdateInfoPanel() {
        return updateInfoPanel;
    }

    public void setUpdateInfoPanel(SpecificNeuronRulePanel updateInfoPanel) {
        this.updateInfoPanel = updateInfoPanel;
    }

    @Override
    public void fillFieldValues() {
    }

}
