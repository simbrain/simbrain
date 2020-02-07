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
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor.ObjectTypeEditor;
import org.simbrain.util.widgets.ParameterWidget;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import java.util.List;

/**
 * <b>NeuronDialog</b> is a dialog box for setting the properties of neurons.
 */
@SuppressWarnings("serial")
public final class NeuronDialog extends StandardDialog {

    /**
     * The neurons being modified.
     */
    private final List<Neuron> neuronList;

    /**
     * The main panel for editing neuron properties.
     */
    private AnnotatedPropertyEditor neuronPropertiesPanel;

    /**
     * Help Button. Links to information about the currently selected neuron
     * update rule.
     */
    private final JButton helpButton = new JButton("Help");

    /**
     * Show Help Action. The action executed by the help button
     */
    private ShowHelpAction helpAction;

    /**
     * Construct a dialog for a set of neurons.
     *
     * @param neurons
     */
    public NeuronDialog(final List<Neuron> neurons) {

        neuronList = neurons;

        neuronPropertiesPanel = new AnnotatedPropertyEditor(neuronList);
        setTitle(neuronPropertiesPanel.getTitleString());

        JScrollPane scroller = new JScrollPane(neuronPropertiesPanel);
        scroller.setBorder(null);
        setContentPane(scroller);
        this.addButton(helpButton);
        addListeners();
        updateHelp();
    }

    /**
     * Add listeners to the components of the dialog. Specifically alters the
     * destination of the help button to reflect the currently selected neuron
     * update rule.
     */
    private void addListeners() {
        JComponent component = neuronPropertiesPanel.getWidget("Update Rule").getComponent();
        ((ObjectTypeEditor) component).getDropDown().addActionListener(
            e -> SwingUtilities.invokeLater(() -> updateHelp()));
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Set the help page based on the currently selected neuron type.
     */
    private void updateHelp() {

        ParameterWidget pw = neuronPropertiesPanel.getWidget("Update Rule");
        String selection = (String) ((ObjectTypeEditor) pw.getComponent()).getDropDown().getSelectedItem();

        if (selection == SimbrainConstants.NULL_STRING) {
            helpAction = new ShowHelpAction("Pages/Network/neuron.html");
        } else if (selection == null) {
            helpButton.setEnabled(false);
        } else {

            // Use combo box label (with spaces removed) for doc page.
            String name = selection.replaceAll("\\s", ""); // Remove white space

            // Docs are in different places for activity generators and neurons
            String docFolder = "";
            if (neuronList.get(0).getUpdateRule() instanceof ActivityGenerator) {
                docFolder = "activity_generator";
            } else {
                docFolder = "neuron";
            }

            // Create the help action
            helpAction = new ShowHelpAction("Pages/Network/" + docFolder + "/" + name + ".html");
        }
        helpButton.setAction(helpAction);
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        neuronPropertiesPanel.commitChanges();

        neuronList.forEach(n ->{
            n.getEvents().fireActivationChange(0, n.getActivation());
            n.getEvents().fireLabelChange();
            n.getEvents().fireClampedChange(false, n.isClamped() );
        });

    }

}
