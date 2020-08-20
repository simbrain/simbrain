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
package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.gui.trainer.IterativeControlsPanel;
import org.simbrain.network.gui.trainer.subnetworkTrainingPanels.LMSOfflineControlPanel;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.trainers.LMSIterative;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
public class LMSEditorDialog2  extends StandardDialog {

    /**
     * Reference to iterative trainer.
     */
    private LMSIterative trainer;

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
     * Reference to validate inputs panel
     */
    private TestInputPanel validateInputsPanel;

    /**
     * List of tabs in the dialog.
     */
    private List<Component> tabs = new ArrayList<Component>();

    /**
     * Network panel.
     */
    protected NetworkPanel networkPanel;

    /**
     * Default constructor.
     *
     * @param np  parent panel
     * @param lms edited network
     */
    public LMSEditorDialog2(final NetworkPanel np, final LMSIterative lms) {
        this.trainer = lms;
        this.networkPanel = np;
        init();
        initDefaultTabs();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("Edit LMS Network");

        // Set up combo box
        String[] lmsTypes = {"Iterative", "Offline"};
        final JComboBox<String> selectType = new JComboBox<String>(lmsTypes);
        Box cbHolder = Box.createHorizontalBox();
        cbHolder.add(Box.createHorizontalGlue());
        cbHolder.add(new JLabel("Select training type: "));
        cbHolder.add(selectType);
        cbHolder.add(Box.createHorizontalGlue());

        // Main vertical box
        Box trainerPanel = Box.createVerticalBox();
        trainerPanel.setOpaque(true);
        trainerPanel.add(Box.createVerticalStrut(5));
        trainerPanel.add(cbHolder);
        trainerPanel.add(Box.createVerticalStrut(5));
        final JPanel trainerContainer = new JPanel();
        trainerContainer.setLayout(new BorderLayout());
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        trainerPanel.add(separator);
        trainerPanel.add(trainerContainer);

        // Add to tabbed pane
        addTab("Train", trainerPanel);
        updateComboBox(selectType, trainerContainer);

        selectType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateComboBox(selectType, trainerContainer);
            }

        });

    }

    /**
     * Update trainer panel based on the combo box.
     *
     * @param selectType       the combo box
     * @param trainerContainer the container
     */
    private void updateComboBox(JComboBox<String> selectType, final JPanel trainerContainer) {
        trainerContainer.removeAll();
        IterativeControlsPanel iterativeControls = new IterativeControlsPanel(trainer);
        trainerContainer.add(iterativeControls, BorderLayout.CENTER);
        trainerContainer.setPreferredSize(iterativeControls.getPreferredSize());
        trainerContainer.revalidate();
        trainerContainer.repaint();
        repaint();
        pack();
        setLocationRelativeTo(null);
    }

    protected void stopTrainer() {
        if (trainer != null) {
            trainer.setUpdateCompleted(true);
        }
    }

    /**
     * This method initializes the components on the panel.
     */
    protected void initDefaultTabs() {

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Input data tab
        inputPanel = new DataPanel(trainer.getInputs(), trainer.getTrainingSet().getInputDataMatrix(), 5,
                "Input");
        inputPanel.setFrame(this);
        addTab("Input data", inputPanel);

        // Training data tab
        trainingPanel = new DataPanel(trainer.getOutputs(), trainer.getTrainingSet().getTargetDataMatrix(), 5, "Targets");
        trainingPanel.setFrame(this);
        addTab("Target data", trainingPanel);

        // Testing tab
        validateInputsPanel = TestInputPanel.createTestInputPanel(networkPanel, trainer.getInputs(),
                trainer.getTrainingSet().getInputDataMatrix());
        addTab("Validate Input Data", validateInputsPanel);

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
                } else if (index == 3) {
                    // Set validation data to whatever input data currently is
                    if (inputPanel.getTable().getData() != null) {
                        validateInputsPanel.setData(((NumericTable) inputPanel.getTable().getData()).asDoubleArray());
                    }
                }
                pack();
                //setLocationRelativeTo(null);
                updateData();
            }
        };

        tabbedPane.addChangeListener(changeListener);


        // TODO
        //
        // // Set up help
        // if (trainable instanceof LMSNetwork) {
        //     Action helpAction = new ShowHelpAction("Pages/Network/network/lmsnetwork.html");
        //     addButton(new JButton(helpAction));
        // } else if (trainable instanceof BackpropNetwork) {
        //     Action helpAction = new ShowHelpAction("Pages/Network/network/backpropnetwork.html");
        //     addButton(new JButton(helpAction));
        // }

    }


    /**
     * Called when switching tabs.  Intended to be overridden.
     */
    void updateData() {
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

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        super.closeDialogOk();
        inputPanel.commitChanges();
        trainingPanel.commitChanges();
    }
}
