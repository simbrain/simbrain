package org.simbrain.network.gui.dialogs.neuron;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.layouts.Layout;
import org.simbrain.util.DetailTrianglePanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Theme;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * Dialog for adding multiple neurons to the network.
 */
public class AddNeuronsDialog extends StandardDialog {

    private static final int DEFAULT_NUM_NEURONS = 100;

    private final NetworkPanel networkPanel;
    private final Neuron baseNeuron;
    private final Box mainPanel = Box.createVerticalBox();
    private final JTextField numNeurons = new JTextField("" + DEFAULT_NUM_NEURONS);
    private final Layout.LayoutEditor layoutEditor = new Layout.LayoutEditor();
    
    private AnnotatedPropertyEditor neuronEditor;
    private AnnotatedPropertyEditor layoutPanel;

    public AddNeuronsDialog(NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        this.baseNeuron = new Neuron();
        networkPanel.getSelectionManager().clear();
        init();
    }

    private void init() {
        setTitle("Add neurons...");

        // Quantity panel
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        quantityPanel.setBorder(Theme.sectionBorder("Quantity"));
        quantityPanel.add(new JLabel("Number of Neurons:"));
        quantityPanel.add(numNeurons);
        mainPanel.add(quantityPanel);

        mainPanel.add(Box.createVerticalStrut(Theme.sectionGap));

        // Neuron type panel
        neuronEditor = new AnnotatedPropertyEditor<>(Collections.singletonList(baseNeuron));
        DetailTrianglePanel neuronPanel = new DetailTrianglePanel(neuronEditor, false);
        neuronPanel.setBorder(Theme.sectionBorder("Neuron Type"));
        mainPanel.add(neuronPanel);

        mainPanel.add(Box.createVerticalStrut(Theme.sectionGap));

        // Layout panel
        layoutPanel = new AnnotatedPropertyEditor<>(layoutEditor);
        DetailTrianglePanel layoutDetailPanel = new DetailTrianglePanel(layoutPanel, false);
        layoutDetailPanel.setBorder(Theme.sectionBorder("Neuron Layout"));
        mainPanel.add(layoutDetailPanel);

        // The dialog already supplies its own margin (Theme.dialogBorder), so let the scroll
        // pane be borderless rather than drawing a redundant box around the sections.
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        setContentPane(scrollPane);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        neuronEditor.commitChanges();
        layoutPanel.commitChanges();
        networkPanel.addNeuronsAsync(Integer.parseInt(numNeurons.getText()), baseNeuron, layoutEditor.getLayout());
        dispose();
    }

    @Override
    protected void closeDialogCancel() {
        super.closeDialogCancel();
        dispose();
    }

}
