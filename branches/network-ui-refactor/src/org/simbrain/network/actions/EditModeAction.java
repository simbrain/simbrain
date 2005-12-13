
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.EditMode;
import org.simbrain.network.NetworkPanel;

/**
 * Build mode action.
 */
class EditModeAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Build mode. */
    private final EditMode editMode;


    /**
     * Create a new edit mode action with the specified name,
     * network panel, and edit mode.
     *
     * @param name name
     * @param networkPanel network panel, must not be null
     * @param editMode edit mode, must not be null
     */
    EditModeAction(final String name,
                    final NetworkPanel networkPanel,
                    final EditMode editMode) {

        super(name);

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        if (editMode == null) {
            throw new IllegalArgumentException("editMode must not be null");
        }

        this.networkPanel = networkPanel;
        this.editMode = editMode;
    }


    /** @see AbstractAction */
    public final void actionPerformed(final ActionEvent event) {
        networkPanel.setEditMode(editMode);
    }
}