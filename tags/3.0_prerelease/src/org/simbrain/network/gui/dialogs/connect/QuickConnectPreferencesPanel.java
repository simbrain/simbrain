/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.connect;

import java.awt.CardLayout;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.connections.QuickConnectionManager;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.AllToAllPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.SparseConnectionPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.OneToOnePanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>QuickConnectPreferencesPanel</b> is a dialog box for setting the
 * properties of the quick connection manager, which is used when creating
 * connections on loose neurons using key commands.
 */
public class QuickConnectPreferencesPanel extends JPanel {

    /** Select connection type. */
    private JComboBox<ConnectNeurons> cbConnectionType;

    /** Main dialog box. */
    private Box mainPanel = Box.createVerticalBox();

    /** Panel for setting connection type. */
    private LabelledItemPanel typePanel = new LabelledItemPanel();

    /** Reference to network panel. */
    private NetworkPanel panel;

    /** Parent dialog. */
    private final Window parentWindow;

    /** Panel for setting the inhibitory / excitatory ratio. */
    private SynapsePolarityAndRandomizerPanel ratioPanel;

    /** Panel which holds the connection panels (for use in resizing). */
    private JPanel connectionPanelHolder = new JPanel(new CardLayout());

    /** List of connection panels (for use in resizing). */
    private AbstractConnectionPanel[] connectorPanels = new AbstractConnectionPanel[3];

    /**
     * Connection dialog default constructor.
     */
    public QuickConnectPreferencesPanel(NetworkPanel panel,
            final Window parentWindow) {
        this.panel = panel;
        this.parentWindow = parentWindow;

        // Set up combo box
        cbConnectionType = new JComboBox<ConnectNeurons>(panel
                .getQuickConnector().getConnectors());
        cbConnectionType.setSelectedItem(panel.getQuickConnector()
                .getCurrentConnector());
        cbConnectionType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                initCardPanel();
            }
        });

        typePanel.addItem("Quick Connection Type", cbConnectionType);

        // Set up connection holder
        QuickConnectionManager manager = panel.getQuickConnector();
        connectorPanels[0] = new AllToAllPanel(manager.getAllToAll(), panel);
        connectorPanels[1] = new OneToOnePanel(manager.getOneToOne());
        connectorPanels[2] = SparseConnectionPanel
                .createSparsityAdjustmentPanel(manager.getSparse(), panel);
        connectionPanelHolder
                .add(connectorPanels[0], AllToAll.getName());
        connectionPanelHolder
                .add(connectorPanels[1], OneToOne.getName());
        connectionPanelHolder.add(connectorPanels[2], Sparse.getName());

        // Set up main panel
        JLabel infoLabel = new JLabel(
                "Set preferences for making \"quick connections\"");
        infoLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(infoLabel);
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(typePanel);
        mainPanel.add(connectionPanelHolder);
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        QuickConnectionManager connector = panel.getQuickConnector();
        ratioPanel = SynapsePolarityAndRandomizerPanel
                .createPolarityRatioPanel(parentWindow,
                        connector.getExRandomizer(),
                        connector.getInRandomizer(),
                        connector.isUseExcitatoryRandomization(),
                        connector.isUseInhibitoryRandomization());
        ratioPanel.setExcitatoryRatio(panel.getQuickConnector()
                .getExcitatoryRatio());
        mainPanel.add(ratioPanel);
        this.add(mainPanel);

        initCardPanel();

    }

    /**
     * Set the current "card".
     */
    private void initCardPanel() {
        CardLayout cl = (CardLayout) connectionPanelHolder.getLayout();
        cl.show(connectionPanelHolder, cbConnectionType.getSelectedItem()
                .toString());
        connectionPanelHolder.setPreferredSize(getSelectedPanel()
                .getPreferredSize());
        mainPanel.revalidate();
        mainPanel.repaint();
        parentWindow.pack();
    }

    /**
     * Helper method to get the current card panel.
     *
     * @return the currently visible panel
     */
    private AbstractConnectionPanel getSelectedPanel() {
        for (AbstractConnectionPanel p : connectorPanels) {
            if (p.isVisible()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        QuickConnectionManager connector = panel.getQuickConnector();
        connector.setCurrentConnector((ConnectNeurons) cbConnectionType
                .getSelectedItem());
        connectorPanels[0].commitChanges();
        connectorPanels[1].commitChanges();
        connectorPanels[2].commitChanges();
        ratioPanel.commitChanges();
        connector.setExcitatoryRatio(ratioPanel.getPercentExcitatory());
        connector.setExRandomizer(ratioPanel.getExRandomizer());
        connector.setInRandomizer(ratioPanel.getInRandomizer());
        connector.setUseExcitatoryRandomization(ratioPanel
                .exRandomizerEnabled());
        connector.setUseInhibitoryRandomization(ratioPanel
                .inRandomizerEnabled());
    }

    /**
     * Helper class for embedding this panel in a dialog.
     */
    public class QuickConnectDialog extends StandardDialog {

        /** Reference to the preferences panel. */
        private QuickConnectPreferencesPanel panel;

        /**
         * Construct the dialog.
         *
         * @param panel the embedded panel
         */
        public QuickConnectDialog(QuickConnectPreferencesPanel panel) {
            this.panel = panel;
            setTitle("Quick Connect Propeties");

        }

        @Override
        protected void closeDialogOk() {
            super.closeDialogOk();
            panel.commitChanges();
        }

    }

}
