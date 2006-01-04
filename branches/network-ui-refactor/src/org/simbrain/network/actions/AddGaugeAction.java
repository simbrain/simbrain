
package org.simbrain.network.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkSelectionEvent;
import org.simbrain.network.NetworkSelectionListener;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.Utils;

/**
 * Add Gauge to workspace which by default gauges all neuronnodes on this networkpanel.
 */
public final class AddGaugeAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new show network preferences action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public AddGaugeAction(final NetworkPanel networkPanel) {

        super("Add Gauge");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Gauge.gif"));

        this.putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        networkPanel.getWorkspace().addGauge();
        GaugeFrame gauge = networkPanel.getWorkspace().getLastGauge();
        // By default gauge all neurons of the current network
        gauge.setVariables(networkPanel.getNetwork().getNeuronList(), networkPanel.getNetworkFrame().getTitle());
        networkPanel.getNetwork().addNetworkListener(gauge);
    }
}