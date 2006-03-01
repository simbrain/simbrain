
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Clamps neurons action.
 */
public final class ClampNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new clamp neurons action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public ClampNeuronsAction(final NetworkPanel networkPanel) {

        super("Clamp Neurons");

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
        networkPanel.getNetwork().setClampNeurons(cb.isSelected());

    }
}