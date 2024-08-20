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
import org.simbrain.network.updaterules.NeuronUpdateRule;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
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
    private AnnotatedPropertyEditor<Neuron> neuronPropertiesPanel;

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
     */
    public NeuronDialog(final List<Neuron> neurons) {
        neuronList = neurons;
        neuronPropertiesPanel = new AnnotatedPropertyEditor<>(neuronList);
        setTitle(neuronPropertiesPanel.getTitleString());

        JScrollPane scroller = new JScrollPane(neuronPropertiesPanel);
        scroller.setBorder(null);
        setContentPane(scroller);
        this.addButton(helpButton);
        neuronPropertiesPanel.getWidgetEventsByLabel("Update Rule").getValueChanged().on(newValue -> {
            updateHelp((NeuronUpdateRule<?, ?>) neuronPropertiesPanel.getWidgetValueByLabel("Update Rule"));
        });
        updateHelp(neurons.stream().findFirst().get().getUpdateRule());
    }


    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Set the help page based on the currently selected neuron type.
     */
    private void updateHelp(NeuronUpdateRule<?, ?> updateRule) {

        if (updateRule == null) {
            helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/network/neurons/");
        } else if (updateRule instanceof NeuronUpdateRule<?,?>) {
            String name = updateRule.getName().toLowerCase();

            // Create the help action
            helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/network/neurons/" + name + ".html");
        }
        helpButton.setAction(helpAction);
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        neuronPropertiesPanel.commitChanges();

        neuronList.forEach(n ->{
            n.getEvents().getLabelChanged().fire("", n.getLabel());
            n.getEvents().getColorChanged().fire();
            n.getEvents().getClampChanged().fire();
        });

    }

}
