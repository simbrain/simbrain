package org.simbrain.workspace.serialization;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.simbrain.workspace.updater.UpdateAction;

/**
 * A persistable form of update action that can be used to recreate the
 * action.
 *
 * @author Jeff Yoshimi
 */
@XStreamAlias("ArchivedUpdateAction")
final class ArchivedUpdateAction {

    /**
     * Reference to the action itself.
     */
    private final UpdateAction updateAction;

    /**
     * Reference to the component id for this action, or null if not needed.
     */
    private final String componentId;

    /**
     * Reference to the coupling id for this action, or null if not needed.
     */
    private final String couplingId;

    /**
     * Construct the archived update action.
     *
     * @param action      reference to the update action itself.
     * @param componentId component id or null if none needed
     * @param couplingId  coupling id or null if none needed
     */
    ArchivedUpdateAction(UpdateAction action, String componentId, String couplingId) {
        this.updateAction = action;
        this.componentId = componentId;
        this.couplingId = couplingId;
    }

    /**
     * @return the componentId
     */
    public String getComponentId() {
        return componentId;
    }

    /**
     * @return the couplingId
     */
    public String getCouplingId() {
        return couplingId;
    }

    /**
     * @return the updateAction
     */
    public UpdateAction getUpdateAction() {
        return updateAction;
    }

}