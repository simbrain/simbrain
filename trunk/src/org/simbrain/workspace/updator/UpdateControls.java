package org.simbrain.workspace.updator;

import java.util.List;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Interface provided to update methods.  Tools and operations that can be
 * performed when creating an update method.  For example, updateComponent
 * updates a component and counts down a latch.
 *
 * Basically a way to selectively provide access to methods that the Controller
 * needs to use to do do workspace updates without having to make them public
 * methods on the WorkspaceUpdate class. One of the benefits of this is that it
 * makes clear what the controller can do to manage updates. This also has some
 * other benefits. The ability to limit access to these controls only to the
 * current controller is really the biggest. Another benefit is that because the
 * controllers are not bound to a specific implementation, different
 * implementations can be given to them without affecting the controllers. For
 * example, one thing that someone might need to do is invalidate the controls
 * once a new controller is set. With this approach, that can be done pretty
 * easily.
 *
 * @author Matt Watson
 */
public interface UpdateControls {

    /**
     * Updates all the couplings.
     */
    void updateCouplings();

    /**
     * Returns the components in the workspace.
     *
     * @return the components in the workspace.
     */
    List<? extends WorkspaceComponent> getComponents();

    /**
     * Submits a component for update, calling countDown on the given latch when
     * it is updated.
     *
     * @param component The component to update.
     * @param signal The signal to call done on when complete.
     */
    void updateComponent(WorkspaceComponent component, CompletionSignal signal);

    /**
     * Update all incoming couplings (i.e. consumers) associated with this
     * component. Used in priority based workspace update.
     *
     * @see {org.simbrain.workspace.updator.PriorityUpdator}
     * @param component component whose consumers should be updated.
     */
    void updateIncomingCouplings(WorkspaceComponent component);

    /**
     * Update all outgoing couplings (i.e. producers) associated with this
     * component. Used in priority based workspace update.
     *
     * @see {org.simbrain.workspace.updator.PriorityUpdator}
     * @param component component whose producers should be updated.
     */
    void updateOutgoingCouplings(WorkspaceComponent component);
}