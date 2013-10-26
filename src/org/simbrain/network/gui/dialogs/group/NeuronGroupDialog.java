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

import java.awt.geom.Point2D;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.gui.dialogs.network.CompetitivePropertiesPanel;
import org.simbrain.network.subnetworks.Competitive;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;

/**
 * Dialog for editing neuron groups.
 *
 * @author Jeff Yoshimi
 */
public class NeuronGroupDialog extends StandardDialog {

    // TODO
    // - Move general info to a separate panel?
    // - Change in a way that prepares for possible future where multiple can be
    // edited at once?

    /** Parent network panel. */
    private NetworkPanel networkPanel;

    /** Neuron Group. */
    private NeuronGroup neuronGroup;

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** General properties. */
    private JPanel tabMain = new JPanel();

    /** Miscellaneous tab. */
    private JPanel tabActivation = new JPanel();

    /** Miscellaneous tab. */
    private JPanel tabLayout = new JPanel();

    /** Layout panel. */
    private MainLayoutPanel layoutPanel;

    /** Panel for specific group types. Null for bare neuron group. */
    private JPanel specificNeuronGroupPanel;

    /** Label Field. */
    private final JTextField tfNeuronLabel = new JTextField();

    /** Main properties panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /**
     * Construct the neuron group dialog.
     *
     * @param np Parent network panel
     * @param ng neuron group being edited
     */
    public NeuronGroupDialog(final NetworkPanel np, final NeuronGroup ng) {
        networkPanel = np;
        neuronGroup = ng;
        setTitle("Edit Neuron Group");

        setSpecificGroup();
        fillFieldValues();
        setContentPane(tabbedPane);

        // Generic group properties
        tabbedPane.addTab("Properties", tabMain);
        tabMain.add(mainPanel);
        mainPanel.addItem("Id:", new JLabel(neuronGroup.getId()));
        mainPanel.addItem("Label:", tfNeuronLabel);

        // If this is a subclass of neuron group, add a tab for editing those
        // properties
        initializeSpecificGroupTab();

        // Layout properties
        tabbedPane.addTab("Layout", tabLayout);
        layoutPanel = new MainLayoutPanel(false, this);
        tabLayout.add(layoutPanel);

        // Set up help button
        if (specificNeuronGroupPanel != null) {
            Action helpAction = new ShowHelpAction(
                    ((GroupPropertiesPanel) specificNeuronGroupPanel)
                            .getHelpPath());
            this.addButton(new JButton(helpAction));
        }
    }

    /**
     * Sets the specificNeuronGroupPanel based on the underlying group.
     */
    private void setSpecificGroup() {
        if (neuronGroup instanceof Competitive) {
            specificNeuronGroupPanel = new CompetitivePropertiesPanel(
                    networkPanel, (Competitive) neuronGroup);
        }
    }

    /**
     * Add a tab for specific neuron group rules.
     */
    private void initializeSpecificGroupTab() {
        if (specificNeuronGroupPanel == null) {
            return;
        } else {
            tabbedPane.addTab("" + neuronGroup.getClass().getSimpleName()
                    + " properties", specificNeuronGroupPanel);
        }
    }

    /**
     * Set the initial values of dialog components.
     */
    public void fillFieldValues() {

        tfNeuronLabel.setText(neuronGroup.getLabel());
        // layoutPanel.setLayout(neuronGroup.getLabel()) //TODO How?

        if (specificNeuronGroupPanel != null) {
            ((GroupPropertiesPanel) specificNeuronGroupPanel).fillFieldValues();
        }
    }

    /**
     * Commit changes.
     */
    public void commitChanges() {
        neuronGroup.setLabel(tfNeuronLabel.getText());
        if (specificNeuronGroupPanel != null) {
            ((GroupPropertiesPanel) specificNeuronGroupPanel).commitChanges();
        }
        layoutPanel.commitChanges();
        // TODO: Bug. In feed-forward nets, neuronGroup.getMinY() returns 0
        // and the next line crashes the piccolo canvas
        layoutPanel.getCurrentLayout().setInitialLocation(
                new Point2D.Double(neuronGroup.getMinX(), neuronGroup.getMinY()));
        layoutPanel.getCurrentLayout().layoutNeurons(
                neuronGroup.getNeuronList());
        networkPanel.repaint();
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

}
