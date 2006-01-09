
package org.simbrain.network.actions;

import java.awt.Event;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.simbrain.network.EditMode;
import org.simbrain.network.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Zoom out edit mode action.
 */
public final class ZoomOutEditModeAction
    extends EditModeAction {

    /**
     * Create a new zoom out edit mode action with the specified network
     * panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public ZoomOutEditModeAction(final NetworkPanel networkPanel) {
        super("Zoom out", networkPanel, EditMode.ZOOM_OUT);
        putValue(SMALL_ICON, ResourceManager.getImageIcon("ZoomOut.gif"));
        putValue(SHORT_DESCRIPTION, "Zoom out (shift+z)");

        networkPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.SHIFT_MASK), this);
        networkPanel.getActionMap().put(this, this);
    }
}