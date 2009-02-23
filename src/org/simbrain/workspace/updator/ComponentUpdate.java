package org.simbrain.workspace.updator;

import java.util.concurrent.CountDownLatch;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Runnable for updating a specific component.
 * 
 * @author Matt Watson
 */
class ComponentUpdate implements Runnable {
    /** The component that this instance updates. */
    final WorkspaceComponent<?> component;
    /** count down latch used to update couplings after all components are updated. */
    private final CountDownLatch latch;
    /** The component updator callback. */
    private final ComponentUpdator componentUpdator;
    
    /**
     * Creates a new instance with the given component and latch.
     * 
     * @param component The component instance.
     * @param componentUpdator The updator callback.
     * @param latch The latch.
     */
    ComponentUpdate(final WorkspaceComponent<?> component,
            final ComponentUpdator componentUpdator, final CountDownLatch latch) {
        this.component = component;
        this.latch = latch;
        this.componentUpdator = componentUpdator;
    }

    /**
     * Updates the component.
     */
    public void run() {
        UpdateThread thread = (UpdateThread) Thread.currentThread();
        
        thread.setCurrentTask(this);
        
        WorkspaceUpdator.LOGGER.trace("updating component: " + component);
        
        componentUpdator.update(component);
        
        thread.clearCurrentTask(this);
        
        latch.countDown();
    }
}