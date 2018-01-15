package org.simbrain.world.threedworld.actions;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.threedworld.ThreeDWorld;

/**
 * ToggleUpdateSyncAction toggles whether ThreeDWorld updates are synchronous with
 * Simbrain workspace updates or are processed as fast as possible.
 */
public class ToggleUpdateSyncAction extends AbstractAction {
    private static final long serialVersionUID = 2065579357970661L;

    private ThreeDWorld world;

    /**
     * Construct a new ToggleUpdateSyncAction.
     * @param world The world to toggle the update synchronization state for.
     */
    public ToggleUpdateSyncAction(ThreeDWorld world) {
        super("Sync Update");
        this.world = world;
        putValue(SMALL_ICON, ResourceManager.getSmallIcon("Clock.png"));
        putValue(SHORT_DESCRIPTION, "Synchronize Update to Workspace");
    }

    @Override public void actionPerformed(ActionEvent event) {
        final boolean sync = ((JToggleButton) event.getSource()).isSelected();
        world.getEngine().enqueue(() -> {
            world.getEngine().setUpdateSync(sync);
        });
    }
}
