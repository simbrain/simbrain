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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.gui.dialogs.network.CompetitivePropertiesPanel;
import org.simbrain.network.gui.dialogs.network.SOMPropertiesPanel;
import org.simbrain.network.gui.dialogs.network.WTAPropertiesPanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronPropertiesPanel;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.subnetworks.CompetitiveGroup;
import org.simbrain.network.subnetworks.SOMGroup;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.math.NumericMatrix;
import org.simbrain.util.widgets.ApplyPanel;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * Main tabbed panel for editing neuron groups.
 *
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class NeuronGroupPanel extends JPanel implements GroupPropertiesPanel,
        EditablePanel {

    /** Parent network panel. */
    private NetworkPanel networkPanel;

    /** Neuron Group. */
    private NeuronGroup neuronGroup;

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** A panel that provides a high-level summary of the neuron group. */
    private EditablePanel summaryPanel;

    /** Layout panel. */
    private MainLayoutPanel layoutPanel;

    /** A wrapper for the layout panel. */
    private JPanel layoutPanelWrapper;

    /** Panel for specific group types. Null for bare neuron group. */
    private EditablePanel specificNeuronGroupPanel;

    /** Panel for editing test input data for this group. */
    private TestInputPanel inputDataPanel;

    /**
     * The neuron group panel containing both a basic neuron info panel and a
     * neuron update settings panel.
     */
    private EditablePanel combinedNeuronInfoPanel;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * A list of components for storage while tabs are being switched. The
     * parent to this panel is able to resize according to the displayed tab
     * because all invisible tabs are replaced by dummy panels with the same
     * preferred size as the currently displayed panel. When panels aren't being
     * displayed, they are stored here and when a new tab is selected the
     * appropriate panel from this list is displayed.
     */
    private ArrayList<Component> storedComponents = new ArrayList<Component>();

    /** The parent window for easy resizing. */
    private final Window parent;

    /**
     * Creates a neuron group panel meant for creating neuron groups.
     *
     * @param np
     *            the parent network of the prospective neuron group
     * @param parent
     *            the parent window for resizing
     * @return a neuron group panel
     */
    public static NeuronGroupPanel createNeuronGroupPanel(
            final NetworkPanel np, final Window parent) {
        NeuronGroupPanel ngp = new NeuronGroupPanel(np, parent);
        ngp.initializeLayout();
        ngp.addListeners();
        return ngp;
    }

    /**
     * Creates a neuron group panel meant for editing an existing neuron group.
     *
     * @param np
     *            the parent network of the neuron group
     * @param ng
     *            the neuron group the created panel will edit/display
     * @param parent
     *            the parent window for easy resizing
     * @return a neuron group panel to edit the given neuron group
     */
    public static NeuronGroupPanel createNeuronGroupPanel(
            final NetworkPanel np, final NeuronGroup ng, final Window parent) {
        NeuronGroupPanel ngp = new NeuronGroupPanel(np, ng, parent);
        ngp.initializeLayout();
        ngp.addListeners();
        return ngp;
    }

    /**
     * Constructor for the case where a neuron group is being created.
     *
     * @param np
     *            Parent network panel
     * @param parent
     *            parent dialog containing this panel.
     */
    private NeuronGroupPanel(final NetworkPanel np, final Window parent) {
        networkPanel = np;
        this.parent = parent;
        isCreationPanel = true;
        layoutPanel = new MainLayoutPanel(false, parent);
    }

    /**
     * Constructor for case where an existing neuron group is being edited.
     *
     * @param np
     *            Parent network panel
     * @param ng
     *            neuron group being edited
     * @param parent
     *            parent dialog containing this panel.
     */
    private NeuronGroupPanel(final NetworkPanel np, final NeuronGroup ng,
            final Window parent) {
        networkPanel = np;
        this.parent = parent;
        neuronGroup = ng;
        isCreationPanel = false;
        layoutPanel = new MainLayoutPanel(ng.getLayout(), false, parent);
    }

    /**
     * Wraps the components in apply panels if we are editing rather than
     * creating a neuron group.
     */
    private void editWrapComponents() {
        if (isCreationPanel) {
            throw new IllegalStateException("editWrapComponents is not"
                    + " appropriate for creation panels. Components are"
                    + " only wrapped in an ApplyPanel if this panel is"
                    + "editing, not creating a neuron group.");
        }

        summaryPanel = ApplyPanel.createApplyPanel(new SummaryPanel(
                neuronGroup, false));

        specificNeuronGroupPanel = getSpecificGroupPanel();
        if (specificNeuronGroupPanel != null) {
            specificNeuronGroupPanel = ApplyPanel
                    .createApplyPanel(specificNeuronGroupPanel);
        }

        combinedNeuronInfoPanel = ApplyPanel
                .createApplyPanel(NeuronPropertiesPanel
                        .createNeuronPropertiesPanel(
                                neuronGroup.getNeuronList(), parent));
        ((ApplyPanel) combinedNeuronInfoPanel).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                boolean spiking = true;
                                for (Neuron n : neuronGroup.getNeuronList()) {
                                    if (!n.getUpdateRule().isSpikingNeuron()) {
                                        spiking = false;
                                        break;
                                    }
                                }
                                neuronGroup.setSpikingNeuronGroup(spiking);
                            }
                        });
                    }
                });

        layoutPanelWrapper = ApplyPanel.createApplyPanel(new EditablePanel() {
            @Override
            public boolean commitChanges() {
                try {
                    applyLayout();
                    networkPanel.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            public JPanel getPanel() {
                return layoutPanel;
            }

            @Override
            public void fillFieldValues() {
            }
        });

    }

    /**
     * Initialize the panel's layout.
     */
    private void initializeLayout() {

        setLayout(new BorderLayout());
        this.setMinimumSize(new Dimension(200, 300));

        specificNeuronGroupPanel = getSpecificGroupPanel();

        if (isCreationPanel) {

            if (parent instanceof StandardDialog) {
                ((StandardDialog) parent).setTitle("Create neuron group");
            }
            neuronGroup = new NeuronGroup(networkPanel.getNetwork(),
                    networkPanel.getWhereToAdd(), 25);

            summaryPanel = new SummaryPanel(neuronGroup, true);

            combinedNeuronInfoPanel = NeuronPropertiesPanel
                    .createNeuronPropertiesPanel(neuronGroup.getNeuronList(),
                            parent);

            layoutPanelWrapper = new JPanel();
            layoutPanelWrapper.add(layoutPanel);

        } else {
            if (parent instanceof StandardDialog) {
                ((StandardDialog) parent).setTitle("Edit "
                        + neuronGroup.getLabel());
            }
            editWrapComponents();
        }

        fillFieldValues();

        storedComponents.add((JPanel) summaryPanel); // Not being added to the
                                                     // tabbed pane
        if (specificNeuronGroupPanel != null) {
            storedComponents.add((JPanel) specificNeuronGroupPanel);
        }
        storedComponents.add((JPanel) combinedNeuronInfoPanel); // Not being
                                                                // added to the
                                                                // tabbed pane
        storedComponents.add(layoutPanelWrapper); // Not being added to the
                                                  // tabbed pane
        tabbedPane.addTab(SummaryPanel.DEFAULT_PANEL_NAME,
                (JPanel) summaryPanel);
        if (specificNeuronGroupPanel != null) {
            tabbedPane.addTab("Properties", (JPanel) specificNeuronGroupPanel);
        }
        tabbedPane.addTab("Neurons", new JPanel()); // Holder
        tabbedPane.addTab("Layout", new JPanel()); // Holder

        if (!isCreationPanel) {
            NumericMatrix matrix = new NumericMatrix() {

                @Override
                public void setData(double[][] data) {
                    neuronGroup.setTestData(data);
                }

                @Override
                public double[][] getData() {
                    return neuronGroup.getTestData();
                }
            };
            inputDataPanel = TestInputPanel.createTestInputPanel( networkPanel,
            		neuronGroup.getNeuronList(), matrix);

            storedComponents.add(inputDataPanel);
            tabbedPane.addTab("Input Data", new JPanel()); // Holder
        }



        add(BorderLayout.CENTER, tabbedPane);

        if (parent instanceof StandardDialog) {
            // Set up help button
            Action helpAction;
            if (specificNeuronGroupPanel != null) {
                helpAction = new ShowHelpAction(
                        ((GroupPropertiesPanel) specificNeuronGroupPanel
                                .getPanel()).getHelpPath());
            } else {
                helpAction = new ShowHelpAction(this.getHelpPath());
            }
            ((StandardDialog) parent).addButton(new JButton(helpAction));
        }
    }

    /**
     * Handle tab changes.
     */
    private void addListeners() {
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int selectedTab = ((JTabbedPane) e.getSource())
                        .getSelectedIndex();
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
                        // 90 is a guess as to average px length of tabs
                        // (not their panels, just the tabs)
                        // This is here to prevent "scrunching" of the
                        // tabs when one of the panel's widths is too small
                        // to accommodate all the tabs on one line
                        int minPx = tabbedPane.getTabCount() * 90;
                        if (current.getPreferredSize().width < minPx) {
                            tmpPanel.setPreferredSize(new Dimension(minPx,
                                    current.getPreferredSize().height));
                        } else {
                            tmpPanel.setPreferredSize(current
                                    .getPreferredSize());
                        }
                        tabbedPane.setComponentAt(i, tmpPanel);
                    }
                }
                tabbedPane.invalidate();
                parent.pack();
            }
        });
    }

    /**
     * Gets the specificNeuronGroupPanel based on the underlying group.
     *
     * @return the committable neuron group panel corresponding to the specific
     *         type of the neuron group being edited if it has one.
     */
    private EditablePanel getSpecificGroupPanel() {
        if (neuronGroup instanceof CompetitiveGroup) {
            return CompetitivePropertiesPanel.createCompetitivePropertiesPanel(
                    networkPanel, (CompetitiveGroup) neuronGroup);
        } else if (neuronGroup instanceof WinnerTakeAll) {
            return new WTAPropertiesPanel(networkPanel,
                    (WinnerTakeAll) neuronGroup);
        } else if (neuronGroup instanceof SOMGroup) {
            return new SOMPropertiesPanel(networkPanel, (SOMGroup) neuronGroup);
        } else {
            return null;
        }
    }

    @Override
    public void fillFieldValues() {
        if (specificNeuronGroupPanel != null) {
            ((GroupPropertiesPanel) specificNeuronGroupPanel.getPanel())
                    .fillFieldValues();
        }
    }

    @Override
    public boolean commitChanges() {
        boolean success = true;
        if (isCreationPanel) {
            success &= summaryPanel.commitChanges();
            success &= combinedNeuronInfoPanel.commitChanges();
            Neuron template = neuronGroup.getNeuronList().get(0);
            try {
                neuronGroup.clearNeuronList();
                int numNeurons = Integer.parseInt(((SummaryPanel) summaryPanel)
                        .getEditablePopField().getText());
                if (numNeurons < 1) {
                    throw new NumberFormatException();
                }
                for (int i = 0; i < numNeurons; i++) {
                    neuronGroup.addNeuron(template.deepCopy(), false);
                }
                layoutPanel.commitChanges();
                neuronGroup.setLayout(layoutPanel.getCurrentLayout());
                neuronGroup.applyLayout();
            } catch (NumberFormatException nfe) {
                System.err.println("Class: " + this.getClass().getSimpleName()
                        + " Method: commitChanges " + "NumberFormatException: "
                        + nfe.getMessage());
                ((SummaryPanel) summaryPanel.getPanel()).getPopLabel()
                        .setForeground(Color.RED);
                repaint();
                return false;
            }
            return success;
        }

        if (specificNeuronGroupPanel != null) {
            success &= ((GroupPropertiesPanel) specificNeuronGroupPanel
                    .getPanel()).commitChanges();
        }
        networkPanel.repaint();
        return success;

    }

    /**
     * A helper method to perform all the steps necessary to apply a layout and
     * have those layouts reflected.
     */
    private void applyLayout() {
        layoutPanel.commitChanges();
        neuronGroup.setLayout(layoutPanel.getCurrentLayout());
        neuronGroup.applyLayout();
    }

    @Override
    public String getHelpPath() {
        return "Pages/Network/groups.html";
    }

    @Override
    public Group getGroup() {
        return neuronGroup;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}