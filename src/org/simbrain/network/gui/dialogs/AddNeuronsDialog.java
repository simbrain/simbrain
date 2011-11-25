package org.simbrain.network.gui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.LayoutDialog;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * A dialog for adding multiple neurons to the network at once giving options
 * on how and what kind of neurons are laid out.
 *
 * @author ztosi
 *
 */
public class AddNeuronsDialog extends StandardDialog {

    /**
     * Default.
     */
    private static final long serialVersionUID = 1L;

    /**The default layout.*/
    private static final Layout DEFAULT_LAYOUT = new GridLayout();

    /**The default neuron.*/
    private static final NeuronUpdateRule DEFAULT_NEURON = new LinearNeuron();

    /**The network panel neurons will be added to.*/
    private final NetworkPanel networkPanel;

    /**Item panel where options will be displayed.*/
    private LabelledItemPanel addNeuronsPanel = new LabelledItemPanel();

    /**Button allowing selection of type of neuron to add.**/
    private JButton selectNeuronType = new JButton();

    /**Button allowing selection of Layout.*/
    private JButton selectLayout = new JButton();

    /**A button that adds the specified number of neurons to the network.*/
    private JButton addButton = new JButton("add");

    /**Text field where desired number of neurons is entered.*/
    private JTextField numNeurons = new JTextField("0");

    /**The layout to be used on the neurons.*/
    private Layout layout = DEFAULT_LAYOUT;

    /**The type of neuron being laid out.*/
    private NeuronUpdateRule neuronUpdateRule = DEFAULT_NEURON;

    /**An ArrayList containing the GUI neurons.*/
    private final ArrayList<NeuronNode> nodes = new ArrayList<NeuronNode>();

    /**
     * Constructs the dialog.
     * @param networkPanel the panel the neurons are being added to.
     */
    public AddNeuronsDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        init();
    }

    /**
     * Initializes the add neurons panel with default settings.
     */
    private void init() {
        setTitle("Add Neurons");

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        addNeuronsPanel.setMyNextItemRow(1);
        c.gridx = 0;
        c.gridy = addNeuronsPanel.getMyNextItemRow();

        networkPanel.clearSelection();

        selectNeuronType.setEnabled(false);
        selectLayout.setEnabled(false);

        instantiateButtons();

        selectNeuronType.setText(neuronUpdateRule.getDescription());
        selectLayout.setText(layout.getLayoutName());
        addNeuronsPanel.addItem("", addButton, 2);
        addNeuronsPanel.addItem("Number of Neurons: ", numNeurons);
        addNeuronsPanel.addItem("Select Neuron Type: ", selectNeuronType);
        addNeuronsPanel.addItem("Select Layout: ", selectLayout);
        numNeurons.setVisible(true);
        setContentPane(addNeuronsPanel);
    }


    /**
     * Instantiates the panel's buttons and sets their action listeners.
     */
    private void instantiateButtons() {
        selectNeuronType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                networkPanel.showSelectedNeuronProperties();
                neuronUpdateRule = networkPanel.getSelectedModelNeurons()
                        .get(0).getUpdateRule();
                selectNeuronType.setText(neuronUpdateRule.
                        getDescription());
                selectNeuronType.setContentAreaFilled(true);
                selectNeuronType.repaint();
                addNeuronsPanel.repaint();
            }
        });

        selectLayout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                LayoutDialog lDialog = new LayoutDialog(networkPanel);
	            lDialog.pack();
	            lDialog.setLocationRelativeTo(null);
	            lDialog.setVisible(true);
	            layout = lDialog.getCurrentLayout();
	            selectLayout.setText(layout.getLayoutName());
	            selectLayout.setContentAreaFilled(true);
	            selectLayout.repaint();
	        }
	    });

	    addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!nodes.isEmpty()) {
                    nodes.clear();
                    networkPanel.deleteSelectedObjects();
                }
                try {
                    
                    addNeuronsToPanel();
                    if (addButton.getText() == "add" && !nodes.isEmpty()) {
                    addButton.setText("change");
                    addButton.setContentAreaFilled(true);
                    addButton.repaint();
                    }
                    if (!selectLayout.isEnabled() && !nodes.isEmpty()) {
                        selectLayout.setEnabled(true);
                    }
                    if (!selectNeuronType.isEnabled() && !nodes.isEmpty()) {
                        selectNeuronType.setEnabled(true);
                    }
               } catch (NumberFormatException nfe) {
                   JOptionPane.showMessageDialog(null,
                       "Inappropriate Field Values:"
                       + "\nIntegers only.", "Error",
                       JOptionPane.ERROR_MESSAGE);
                }

            }
        });

	}

    /**
     * Lays out the neurons in their current state.
     */
	private void addNeuronsToPanel() {
	    int number = Integer.parseInt(numNeurons.getText());
        for (int i = 0; i < number; i++) {
            Neuron neuron = new Neuron(
                networkPanel.getRootNetwork(),
                neuronUpdateRule);
            nodes.add(new NeuronNode(networkPanel, neuron));
            networkPanel.getRootNetwork().addNeuron(neuron);
        }
        networkPanel.setSelection(nodes);
        layout.setInitialLocation(networkPanel.getLastClickedPosition());
        layout.layoutNeurons(networkPanel.getSelectedModelNeurons());
        networkPanel.repaint();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void closeDialogOk() {
		super.closeDialogOk();
		addNeuronsPanel.setVisible(false);
		dispose();
	}

	/**
     * {@inheritDoc}
     */
	protected void closeDialogCancel(){
	    super.closeDialogCancel();
	    nodes.clear();
        networkPanel.deleteSelectedObjects();
        addNeuronsPanel.setVisible(false);
        dispose();
	}

}
