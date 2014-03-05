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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.gui.dialogs.network.CompetitivePropertiesPanel;
import org.simbrain.network.gui.dialogs.network.SOMPropertiesPanel;
import org.simbrain.network.gui.dialogs.network.WTAPropertiesPanel;
import org.simbrain.network.gui.dialogs.neuron.CombinedNeuronInfoPanel;
import org.simbrain.network.subnetworks.CompetitiveGroup;
import org.simbrain.network.subnetworks.SOMGroup;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ApplyPanel;
import org.simbrain.util.widgets.CommittablePanel;

/**
 * Main tabbed panel for editing all neuron groups. Specific neuron panels are
 * included as part of this.
 *
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class NeuronGroupPanel extends JPanel implements GroupPropertiesPanel,
    CommittablePanel {

    // TODO
    // - Change in a way that prepares for possible future where multiple can be
    // edited at once?

    /** Parent network panel. */
    private NetworkPanel networkPanel;

    /** Neuron Group. */
    private NeuronGroup neuronGroup;

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** A pannel that provides a high-level summary of the neuron group. */
    private CommittablePanel summaryPanel;

    /** Layout panel. */
    private MainLayoutPanel layoutPanel;

    /** A wrapper for the layout panel. */
    private JPanel layoutPanelWrapper;

    /** Panel for specific group types. Null for bare neuron group. */
    private CommittablePanel specificNeuronGroupPanel;

    /**
     * The neuron group panel containing both a basic neuron info panel and
     * a neuron update settings panel.
     */
    private CommittablePanel combinedNeuronInfoPanel;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * A list of components for storage while tabs are being switched. The
     * parent to this panel is able to resize according to the displayed tab
     * because all invisible tabs are replaced by dummy panels with the same
     * preferred size as the currently displayed panel. When panels aren't being
     * displayed, they are stored here and when a new tab is selected it
     * the appropriate panel from this list is displayed.
     */
    private ArrayList<Component> storedComponents = new ArrayList<Component>();

    /** The parent window for easy resizing. */
    private final Window parent;

    /**
     * Constructor for the case where a neuron group is being created.
     *
     * @param np Parent network panel
     * @param parent parent dialog containing this panel.
     */
    public NeuronGroupPanel(final NetworkPanel np,
            final Window parent) {
        networkPanel = np;
        this.parent = parent;
        isCreationPanel = true;
        layoutPanel = new MainLayoutPanel(false, parent);
        initializeLayout();
        addListeners();
    }

    /**
     * Constructor for case where an existing neuron group is being edited.
     *
     * @param np Parent network panel
     * @param ng neuron group being edited
     * @param parent parent dialog containing this panel.
     */
    public NeuronGroupPanel(final NetworkPanel np, final NeuronGroup ng,
            final Window parent) {
        networkPanel = np;
        this.parent = parent;
        neuronGroup = ng;
        isCreationPanel = false;
        layoutPanel = new MainLayoutPanel(false, parent);
        initializeLayout();
        addListeners();
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

        summaryPanel = new ApplyPanel(new SummaryPanel(neuronGroup, false));

        specificNeuronGroupPanel = getSpecificGroup();
        if (specificNeuronGroupPanel != null) {
            specificNeuronGroupPanel = new ApplyPanel(specificNeuronGroupPanel);
        }

        combinedNeuronInfoPanel = new ApplyPanel(new CombinedNeuronInfoPanel(
                neuronGroup.getNeuronList(), parent)) {
            @Override
            public boolean commitChanges() {
                boolean success = super.commitChanges();
                networkPanel.repaint();
                return success;
            }
        };

        final ApplyPanel cnip = (ApplyPanel) combinedNeuronInfoPanel;
        final ApplyPanel sp = (ApplyPanel) summaryPanel;

        cnip.getApplyButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                ((SummaryPanel) sp.getPanel()).getTypeField().setText(
                        (String) ((CombinedNeuronInfoPanel)
                                cnip.getPanel()).getUpdateInfoPanel()
                                .getCbNeuronType().getSelectedItem());
            }
        });

        layoutPanelWrapper = new ApplyPanel(new CommittablePanel() {
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
        });
    }

    /**
     *
     */
    private void initializeLayout() {

        setLayout(new BorderLayout());
        this.setMinimumSize(new Dimension(200, 300));

        specificNeuronGroupPanel = getSpecificGroup();

        if (isCreationPanel) {

            if (parent instanceof StandardDialog) {
                ((StandardDialog) parent).setTitle("Create neuron group");
            }
            neuronGroup = new NeuronGroup(networkPanel.getNetwork(),
                    networkPanel.getWhereToAdd(),
                    NeuronGroup.DEFAULT_GROUP_SIZE);

            summaryPanel = new SummaryPanel(neuronGroup, true);

            combinedNeuronInfoPanel = new CombinedNeuronInfoPanel(
                    neuronGroup.getNeuronList(), parent);

            layoutPanelWrapper = new JPanel();
            layoutPanelWrapper.add(layoutPanel);

        } else {
            if (parent instanceof StandardDialog) {
                ((StandardDialog) parent).setTitle("Edit "
                        + neuronGroup.getTypeDescription());
            }
            editWrapComponents();
        }



        fillFieldValues();

        storedComponents.add((JPanel) summaryPanel);
        if (specificNeuronGroupPanel != null) {
            storedComponents.add(specificNeuronGroupPanel.getPanel());
        }
        storedComponents.add((JPanel) combinedNeuronInfoPanel);
        storedComponents.add(layoutPanelWrapper);
        tabbedPane.addTab(SummaryPanel.DEFAULT_PANEL_NAME,
                (JPanel) summaryPanel);
        if (specificNeuronGroupPanel != null) {
            tabbedPane.addTab("Properties",
                    specificNeuronGroupPanel.getPanel());
        }
        tabbedPane.addTab("Neurons", new JPanel()); // Holder
        tabbedPane.addTab("Layout", new JPanel()); // Holder
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
     *
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
                        tmpPanel.setPreferredSize(current.getPreferredSize());
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
     * @return the committable neuron group panel corresponding to the specific
     * type of the neuron group being edited if it has one.
     */
    private CommittablePanel getSpecificGroup() {
        if (neuronGroup instanceof CompetitiveGroup) {
            return new CompetitivePropertiesPanel(
                    networkPanel, (CompetitiveGroup) neuronGroup);
        } else if (neuronGroup instanceof WinnerTakeAll) {
            return new WTAPropertiesPanel(networkPanel,
                    (WinnerTakeAll) neuronGroup);
        } else if (neuronGroup instanceof SOMGroup) {
            return new SOMPropertiesPanel(networkPanel,
                    (SOMGroup) neuronGroup);
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

        // For backwards compatibility
        if (neuronGroup.getLayout() == null) {
            neuronGroup.setLayout(NeuronGroup.DEFAULT_LAYOUT);
        }

        layoutPanel.setCurrentLayout(neuronGroup.getLayout());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean commitChanges() {
        boolean success = true;
        success &= summaryPanel.commitChanges();
        success &= combinedNeuronInfoPanel.commitChanges();
        if (isCreationPanel) {
            Neuron template = neuronGroup.getNeuronList().get(0);
            try {
                int numNeurons = Integer.parseInt(((SummaryPanel) summaryPanel)
                        .getEditablePopField().getText());
                if (numNeurons < 1) {
                    throw new NumberFormatException();
                }
                if (numNeurons >= NeuronGroup.DEFAULT_GROUP_SIZE) {
                    for (int i = NeuronGroup.DEFAULT_GROUP_SIZE;
                            i < numNeurons; i++)
                    {
                        neuronGroup.addNeuron(template.deepCopy());
                    }
                } else {
                    while (neuronGroup.getNeuronList().size() > numNeurons) {
                        neuronGroup.getNeuronList().remove(neuronGroup
                                .getNeuronList().size() - 1);
                    }
                }
            } catch (NumberFormatException nfe) {
                System.err.println("Class: " + this.getClass().getSimpleName()
                        + " Method: commitChanges "
                        + "NumberFormatException: "
                        + nfe.getMessage());
                ((SummaryPanel) summaryPanel.getPanel()).getPopLabel()
                    .setForeground(Color.RED);
                repaint();
                return false;
            }
        }

        if (specificNeuronGroupPanel != null) {
            success &= ((GroupPropertiesPanel) specificNeuronGroupPanel
                    .getPanel()).commitChanges();
        }

        applyLayout();
        networkPanel.repaint();
        return success;

    }

    /**
     * A helper method to perform all the steps necessary to apply a layout
     * and have those layouts reflected.
     */
    private void applyLayout() {
        layoutPanel.commitChanges();
        neuronGroup.setLayout(layoutPanel.getCurrentLayout());
        neuronGroup.applyLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHelpPath() {
        return "Pages/Network/groups.html";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Group getGroup() {
        return neuronGroup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getPanel() {
        return this;
    }

}
