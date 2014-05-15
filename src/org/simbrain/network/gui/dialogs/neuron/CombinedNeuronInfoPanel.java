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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.widgets.EditablePanel;

/**
 * The basic neuron info panel and neuron update settings panel are frequently
 * used together and depend on each other. This panel combines the two and
 * handles changes to one being applied to the other.
 *
 * @author ztosi
 */
@SuppressWarnings("serial")
public class CombinedNeuronInfoPanel extends JPanel
    implements EditablePanel {

    /**
     * The default vertical gap between the basic neuron info panel and the
     * neuron update settings panel.
     */
    private static final int DEFAULT_VGAP = 10;

    /**
     * The default initial display state of the neuron update setting panel's
     * neuron update rule panel.
     */
    private static final boolean DEFAULT_NUSP_DISPLAY_STATE = false;

    /** The basic neuron info panel. */
    private BasicNeuronInfoPanel neuroninfoPanel;

    /** The neuron update settings panel. */
    private NeuronUpdateSettingsPanel updateInfoPanel;

    /**
     * The initial display state of the neuron update rule panel in
     * {@link #updateInfoPanel}.
     */
    private boolean nuspExtendedDisplay = DEFAULT_NUSP_DISPLAY_STATE;

    /**
     * Constructs a combined neuron info panel, which includes the basic neuron
     * info panel and a neuron update settings panel. The constructor
     * automatically builds and lays out the panel, such that it is immediately
     * ready for display. The nusp (neuron update settings panel)'s display
     * state is that extra data is by default hidden. This constructor adheres
     * to that behavior.
     * @param neuronList the list of neurons either being edited (editing) or
     * being used to fill the panel with default values (creation).
     * @param parent the parent window, made available for easy resizing.
     */
    public CombinedNeuronInfoPanel(List<Neuron> neuronList, Window parent) {
        neuroninfoPanel = new BasicNeuronInfoPanel(neuronList, parent);
        updateInfoPanel = new NeuronUpdateSettingsPanel(neuronList, parent,
                nuspExtendedDisplay);
        initializeLayout();
        addListeners();
    }

    /**
     * Constructs a combined neuron info panel, which includes the basic neuron
     * info panel and a neuron update settings panel. The constructor
     * automatically builds and lays out the panel, such that it is immediately
     * ready for display. The nusp (neuron update settings panel)'s display
     * state is that extra data is by default hidden. This constructor allows
     * this behavior to be set manually.
     *
     * @param neuronList the list of neurons either being edited (editing) or
     * being used to fill the panel with default values (creation).
     * @param parent the parent window, made available for easy resizing.
     * @param nuspExtendedDisplay whether or not to display the neuron update
     * rule's details initially
     */
    public CombinedNeuronInfoPanel(List<Neuron> neuronList, Window parent,
            boolean nuspExtendedDisplay) {
        this.nuspExtendedDisplay = nuspExtendedDisplay;
        neuroninfoPanel = new BasicNeuronInfoPanel(neuronList, parent);
        updateInfoPanel = new NeuronUpdateSettingsPanel(neuronList, parent,
                nuspExtendedDisplay);
        initializeLayout();
        addListeners();
    }

    /**
     * Lays out the panel.
     */
    private void initializeLayout() {
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);
        this.add(neuroninfoPanel);
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
                                AbstractNeuronPanel np = updateInfoPanel
                                        .getNeuronPanel();
                                neuroninfoPanel.getExtraDataPanel()
                                    .fillDefaultValues(np.getPrototypeRule());
                                repaint();
                            }
                        });
                    }
                });

    }

    /**
     * {@inheritDoc}
     * <b>Specifically:</b> Commits changes in the basic neuron info panel and
     * the neuron update settings panel.
     */
    @Override
    public boolean commitChanges() {

        boolean success = true;

        // Commit changes specific to the neuron type
        // This must be the first change committed, as other neuron panels
        // make assumptions about the type of the neuron update rule being
        // edited that can result in ClassCastExceptions otherwise.
        success &= updateInfoPanel.commitChanges();

        success &= neuroninfoPanel.commitChanges();

        return success;

    }

    /*/CHECKSTYLE:OFF**************************************
     *              Getters and Setters                   *
     ******************************************************/

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getPanel() {
        return this;
    }

    public BasicNeuronInfoPanel getNeuroninfoPanel() {
        return neuroninfoPanel;
    }

    public void setNeuroninfoPanel(BasicNeuronInfoPanel neuroninfoPanel) {
        this.neuroninfoPanel = neuroninfoPanel;
    }

    public NeuronUpdateSettingsPanel getUpdateInfoPanel() {
        return updateInfoPanel;
    }

    public void setUpdateInfoPanel(NeuronUpdateSettingsPanel updateInfoPanel) {
        this.updateInfoPanel = updateInfoPanel;
    }

    @Override
    public void fillFieldValues() {
        // Not Currently Used        
    }

}
