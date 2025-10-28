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
    private final AnnotatedPropertyEditor<Neuron> neuronPropertiesPanel;

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
        neuronPropertiesPanel.getWidgetEventsByLabel("Update rule").getValueChanged().on(newValue -> {
            updateHelp((NeuronUpdateRule<?, ?>) neuronPropertiesPanel.getWidgetValueByLabel("Update rule"));
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
