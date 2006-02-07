
package org.simbrain.network.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.SwingUtilities;

import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Clamps weights action.
 */
public final class ClampWeightsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new clamp weights action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public ClampWeightsAction(final NetworkPanel networkPanel) {

        super("Clamp Weights");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.gif"));
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        // Perform action
        JCheckBoxMenuItem cb = (JCheckBoxMenuItem) event.getSource();

        // Determine status
        networkPanel.getNetwork().setClampWeights(cb.isSelected());

    }
}