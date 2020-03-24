package org.simbrain.network.gui.dialogs.dl4j;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.network.dl4j.MultiLayerNet;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.dialogs.network.SupervisedTrainingDialog;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.gui.trainer.IterativeControlsPanel;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.math.NumericMatrix;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Train a dl4j network.
 */
public class MultiLayerTrainerDialog extends JDialog {

    // TODO: Merge with SupervisedTrainingDialog?  Or abstract the tab handling if used

    /**
     * Main tabbed pane.
     */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Reference to input data panel.
     */
    private DataPanel inputPanel;

    /**
     * Reference to training data panel.
     */
    private DataPanel trainingPanel;

    /**
     * List of tabs in the dialog.
     */
    private List<Component> tabs = new ArrayList<Component>();


    /**
     * Construct the dialog.
     */
    public MultiLayerTrainerDialog(MultiLayerNet multiLayer) {


        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        this.setContentPane(mainPanel);

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Add training tab (TODO)
        //IterativeControlsPanel iterativeControls = new IterativeControlsPanel(multiLayer);
        //addTab("Train", iterativeControls);

        // Set up initial data
        INDArray inputs = Nd4j.zeros(100, multiLayer.inputSize());
        NumericMatrix inputMatrix = new NumericMatrix() {
            @Override
            public void setData(double[][] data) {
                inputs.data().setData(Utils.flatten(data));
            }

            @Override
            public double[][] getData() {
                return inputs.toDoubleMatrix();
            }
        };

        inputPanel = new DataPanel(inputMatrix, (int) multiLayer.inputSize(), 5, "Input");
        inputPanel.setFrame(this);
        addTab("Input data", inputPanel);

        // Training data tab

        // Set up initial data
        INDArray targets = Nd4j.zeros(100, multiLayer.outputSize());
        NumericMatrix trainingMatrix = new NumericMatrix() {
            @Override
            public void setData(double[][] data) {
                targets.data().setData(Utils.flatten(data));
            }

            @Override
            public double[][] getData() {
                return targets.toDoubleMatrix();
            }
        };


        trainingPanel = new DataPanel(trainingMatrix, (int) multiLayer.outputSize(), 5, "Targets");
        trainingPanel.setFrame(this);
        addTab("Target data", trainingPanel);

        // Testing tab
        //validateInputsPanel = TestInputPanel.createTestInputPanel(networkPanel, trainable.getInputNeurons(), trainable.getTrainingSet().getInputDataMatrix());
        //addTab("Validate Input Data", validateInputsPanel);

        // Finalize
        setContentPane(tabbedPane);

        // Listen for tab changed events.
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
                int index = sourceTabbedPane.getSelectedIndex();
                Component current = tabs.get(index);
                int numTabs = tabs.size();
                for (int i = 0; i < numTabs; i++) {
                    if (i == index) {
                        tabbedPane.setComponentAt(i, current);
                        tabbedPane.repaint();
                        continue;
                    } else {
                        JPanel tmpPanel = new JPanel();
                        int minPx = tabbedPane.getTabCount() * 120;
                        if (current.getPreferredSize().width < minPx) {
                            tmpPanel.setPreferredSize(new Dimension(minPx, current.getPreferredSize().height));
                        } else {
                            tmpPanel.setPreferredSize(current.getPreferredSize());
                        }
                        tabbedPane.setComponentAt(i, tmpPanel);
                    }
                }
                tabbedPane.revalidate();

                if (index == 0) {
                    // When entering training tab, commit table changes
                    inputPanel.commitChanges();
                    trainingPanel.commitChanges();
                    multiLayer.initData(inputs, targets);
                } else if (index == 3) {
                    // Set validation data to whatever input data currently is
                    if (inputPanel.getTable().getData() != null) {
                        //validateInputsPanel.setData(((NumericTable) inputPanel.getTable().getData()).asDoubleArray());
                    }
                }
                //updateData();
                pack();
            }
        };

        tabbedPane.addChangeListener(changeListener);


    }

    /**
     * Add a tab to the dialog.
     *
     * @param name name to be displayed
     * @param tab  the tab itself
     */
    public void addTab(String name, Component tab) {
        if (tabs.size() == 0) {
            tabbedPane.addTab(name, tab);
        } else {
            tabbedPane.addTab(name, new JPanel());
        }
        tabs.add(tab);
    }


}
