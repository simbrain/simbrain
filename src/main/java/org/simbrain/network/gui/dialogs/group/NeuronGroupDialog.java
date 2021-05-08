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
package org.simbrain.network.gui.dialogs.group;

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.layouts.Layout;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.math.NumericMatrix;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;

/**
 * Dialog for editing neuron groups.
 *
 * @author Jeff Yoshimi
 * @author Yulin Li
 */
public final class NeuronGroupDialog extends StandardDialog {

    /**
     * Parent network panel.
     */
    private NetworkPanel networkPanel;

    /**
     * Neuron Group being created or edited.
     */
    private NeuronGroup neuronGroup;

    /**
     * Main tabbed pane.
     */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Label Field.
     */
    private JTextField tfNeuronGroupLabel = new JTextField();

    /**
     * Layout panel.
     */
    private AnnotatedPropertyEditor layoutPanel;

    /**
     * If true this is a creation dialog. Otherwise it is an edit dialog.
     */
    private boolean isCreationDialog = false;

    /**
     * The list of components which are stored here so their tabs can be blanked
     * out. This is what allows the panel to resize when tabs are changed.
     */
    private ArrayList<Component> storedComponents = new ArrayList<Component>();

    /**
     * Panel for specific group types. Null for bare neuron group.
     */
    private EditablePanel specificNeuronGroupPanel;

    /**
     * Neuron group summary.
     */
    private AnnotatedPropertyEditor summaryPanel;

    /**
     * Special object to create new neuron groups.
     */
    private NeuronGroup.NeuronGroupCreator ngCreator;

    /**
     * Property editor for creating new neuron groups.
     */
    private AnnotatedPropertyEditor neuronGroupCreationEditor;

    /**
     * Layout object for new or existing neuron groups.
     */
    private Layout layout;

    /**
     * For creating a new neuron group.
     *
     * @param np  parent panel
     */
    // Called via reflection
    public NeuronGroupDialog(final NetworkPanel np) {
        networkPanel = np;
        isCreationDialog = true;
        init();
    }

    /**
     * For editing an existing neuron group.
     *
     * @param np Parent network panel
     * @param ng Neuron group being edited
     */
    public NeuronGroupDialog(final NetworkPanel np, final NeuronGroup ng) {
        networkPanel = np;
        neuronGroup = ng;
        isCreationDialog = false;
        init();
    }

    /**
     * Initialize the panel.
     */
    private void init() {

        if (isCreationDialog) {
            setTitle("Create Neuron Group");
        } else {
            setTitle("Edit " + neuronGroup.getLabel());
        }

        setMinimumSize(new Dimension(300, 200));
        fillFieldValues();
        setContentPane(tabbedPane);

        // Summary Info
        JPanel firstTab = new JPanel();
        if (!isCreationDialog) {
            summaryPanel = new AnnotatedPropertyEditor(neuronGroup);
            firstTab = summaryPanel;

        } else {
            ngCreator =
                new NeuronGroup.NeuronGroupCreator(
                        networkPanel.getNetwork().getIdManager().getProposedId(NeuronGroup.class));
            neuronGroupCreationEditor = new AnnotatedPropertyEditor(ngCreator);
            firstTab = neuronGroupCreationEditor;
        }
        JScrollPane summaryScrollWrapper = new JScrollPane(firstTab);
        summaryScrollWrapper.setBorder(null);
        storedComponents.add(summaryScrollWrapper);
        tabbedPane.addTab("Summary", summaryScrollWrapper);

        // Layout panel
        if(isCreationDialog) {
            layout = NeuronGroup.DEFAULT_LAYOUT;
        } else {
            layout = neuronGroup.getLayout();
        }
        layoutPanel = new AnnotatedPropertyEditor(layout);
        tabbedPane.addTab("Layout", layoutPanel);
        JScrollPane layoutWrapper = new JScrollPane(layoutPanel);
        layoutWrapper.setBorder(null);
        storedComponents.add(layoutWrapper);
        tabbedPane.addTab("Layout", layoutWrapper);

        if(!isCreationDialog) {
            // Input panel
            NumericMatrix matrix = new NumericMatrix() {

                @Override
                public void setData(double[][] data) {
                    neuronGroup.getInputManager().setData(data);
                }

                @Override
                public double[][] getData() {
                    return neuronGroup.getInputManager().getData();
                }
            };
            JPanel inputDataPanel = TestInputPanel.createTestInputPanel(networkPanel, neuronGroup.getNeuronList(), matrix);
            storedComponents.add(inputDataPanel);
            tabbedPane.addTab("Input Data", inputDataPanel);
        }

        // Set up help button
        Action helpAction = new ShowHelpAction("Pages/Network/groups/NeuronGroup.html");

        // TODO
        //if (specificNeuronGroupPanel != null) {
        //    helpAction = new ShowHelpAction(((GroupPropertiesPanel) specificNeuronGroupPanel).getHelpPath());
        //}
        addButton(new JButton(helpAction));

        // Tab-change events
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateTabSizes(((JTabbedPane) e.getSource()).getSelectedIndex());
            }
        });
        updateTabSizes(0);

        if (!isCreationDialog) {
            // If editing, make this dialog based on a done button, rather than
            // ok and cancel. All edits are done with apply
            setAsDoneDialog();
        }
    }

    /**
     * Add listeners.
     */
    private void updateTabSizes(int selectedTab) {
        Component current = storedComponents.get(selectedTab);
        int numTabs = storedComponents.size();
        for (int i = 0; i < numTabs; i++) {
            if (i == selectedTab) {
                tabbedPane.setComponentAt(i, current);
                tabbedPane.repaint();
                continue;
            } else {
                JPanel tmpPanel = new JPanel();
                // Hack...
                // 120 is a guess as to average px length of tabs
                // (not their panels, just the tabs)
                // This is here to prevent "scrunching" of the
                // tabs when one of the panel's widths is too small
                // to accommodate all the tabs on one line
                int minPx = tabbedPane.getTabCount() * 120;
                if (current.getPreferredSize().width < minPx) {
                    tmpPanel.setPreferredSize(new Dimension(minPx, current.getPreferredSize().height));
                } else {
                    tmpPanel.setPreferredSize(current.getPreferredSize());
                }
                tabbedPane.setComponentAt(i, tmpPanel);
            }
        }
        // tabbedPane.invalidate();
        pack();
    }

    /**
     * Set the initial values of dialog components.
     */
    public void fillFieldValues() {
        if (!isCreationDialog) {
            tfNeuronGroupLabel.setText(neuronGroup.getLabel());
        } else {
            tfNeuronGroupLabel.setText("Neuron group");
        }
    }

    /**
     * Commit changes.
     */
    public void commitChanges() {

        if (isCreationDialog) {
            neuronGroupCreationEditor.commitChanges();
            neuronGroup = ngCreator.create(networkPanel.getNetwork());
            layoutPanel.commitChanges();
            neuronGroup.setLayout(layout);
            neuronGroup.applyLayout();
            networkPanel.getNetwork().addNetworkModel(neuronGroup);
            networkPanel.getPlacementManager().addNewModelObject(neuronGroup);
        } else {
            summaryPanel.commitChanges();
            layoutPanel.commitChanges();
            neuronGroup.applyLayout();
        }

        if (specificNeuronGroupPanel != null) {
            specificNeuronGroupPanel.commitChanges();
        }

        networkPanel.repaint();
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    @Override
    public void pack() {
        super.pack();
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int height = gd.getDisplayMode().getHeight();
        if(this.getLocation().y + this.getBounds().height > height) {
            this.setBounds(getLocation().x, getLocation().y, getWidth(), height - getLocation().y);
        }
    }


}