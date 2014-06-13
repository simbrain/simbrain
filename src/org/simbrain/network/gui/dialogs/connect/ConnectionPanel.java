package org.simbrain.network.gui.dialogs.connect;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.connections.QuickConnectPreferences.ConnectType;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.DensityBasedConnectionPanel;
import org.simbrain.network.gui.dialogs.connect.connector_panels.OneToOnePanel;

public class ConnectionPanel {

    private final Window parentFrame;

    private final NetworkPanel networkPanel;

    private final JPanel mainPanel;

    private JComboBox<ConnectType> connectorCb = new JComboBox<ConnectType>(
        new ConnectType[] { ConnectType.values()[0], ConnectType.values()[1],
            ConnectType.values()[2] });

    private JPanel connectPanel = new JPanel();

    private AbstractConnectionPanel[] connectorPanels =
        new AbstractConnectionPanel[3];

    public static ConnectionPanel createConnectionPanel(final Window parent,
        final NetworkPanel networkPanel) {
        ConnectionPanel cp = new ConnectionPanel(parent, networkPanel);
        cp.addListeners();
        return cp;
    }

    private ConnectionPanel(final Window parent,
        final NetworkPanel networkPanel) {
        this.parentFrame = parent;
        this.networkPanel = networkPanel;
        mainPanel = new JPanel();
        init();
    }

    private void init() {
        connectPanel.setLayout(new CardLayout());
        connectorPanels[0] = DensityBasedConnectionPanel
            .createSparsityAdjustmentPanel(new AllToAll(), networkPanel);
        connectorPanels[1] = DensityBasedConnectionPanel
            .createSparsityAdjustmentPanel(new Sparse(), networkPanel);
        connectorPanels[2] = new OneToOnePanel(new OneToOne());
        connectPanel.add(connectorPanels[0], ConnectType.ALL_TO_ALL.toString());
        connectPanel.add(connectorPanels[1], ConnectType.SPARSE.toString());
        connectPanel.add(connectorPanels[2], ConnectType.ONE_TO_ONE.toString());
        ((CardLayout) connectPanel.getLayout()).show(connectPanel, ConnectType
            .ALL_TO_ALL.toString());

        JPanel cbPanel = new JPanel(new FlowLayout());
        cbPanel.add(new JLabel("Connection Manager: "));
        cbPanel.add(connectorCb);

        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(cbPanel, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        mainPanel.add(connectPanel, gbc);

    }

    private void addListeners() {
        connectorCb.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                CardLayout cl = (CardLayout) connectPanel.getLayout();
                cl.show(connectPanel, connectorCb.getSelectedItem().toString());
                connectPanel.setPreferredSize(getSelectedPanel()
                    .getPreferredSize());
                mainPanel.revalidate();
                mainPanel.repaint();
                parentFrame.pack();
            }
        });
    }

    private AbstractConnectionPanel getSelectedPanel() {
        for (AbstractConnectionPanel p : connectorPanels) {
            if (p.isVisible()) {
                return p;
            }
        }
        return null;
    }

    public void commitChanges(SynapseGroup synapseGroup) {
        AbstractConnectionPanel acp = getSelectedPanel();
        acp.commitChanges();
        synapseGroup.setConnectionManager(acp.getConnection());
    }

    public void commitChanges(List<Neuron> source, List<Neuron> target) {
        AbstractConnectionPanel acp = getSelectedPanel();
        acp.commitChanges(source, target);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public static void main(String[] args) {
        NetworkPanel np = new NetworkPanel(new Network());
        JFrame frame = new JFrame();
        ConnectionPanel cp = createConnectionPanel(frame, np);
        frame.setContentPane(cp.getPanel());
        cp.getPanel().setVisible(true);
        frame.setVisible(true);
        frame.pack();
    }

}
