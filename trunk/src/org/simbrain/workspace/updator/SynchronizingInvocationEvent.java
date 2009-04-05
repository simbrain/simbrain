/**
 * 
 */
package org.simbrain.workspace.updator;

import java.awt.event.InvocationEvent;
import java.util.concurrent.Callable;

import org.simbrain.workspace.Workspace;

/**
 * Class used to wrap InvocationEvents such that they are synchronized when
 * executed.
 * 
 * @author Matt Watson
 */
class SynchronizingInvocationEvent extends InvocationEvent {
    /** default serial version uid. */
    private static final long serialVersionUID = 1L;
    /** the event to synchronize. */
    private final InvocationEvent event;
    
    /**
     * Creates an invocation event for the provided event using the
     * workspace for synchronization and calling signal.done() when
     * finished.
     * 
     * @param event The 'real' invocation event.
     * @param workspace The workspace used for synchronization.
     * @param signal The signal to call when done.
     */
    public SynchronizingInvocationEvent(final InvocationEvent event,
            final Workspace workspace, final CompletionSignal signal) {
        super(event.getSource(), new Runnable() {
            public void run() {
                try {
                    workspace.syncOnAllComponents(new Callable<Object>() {
                        public Object call() throws Exception {
                            event.dispatch();
                            signal.done();
                            return null;
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
        this.event = event;
    }
    
    /**
     * {@inheritDoc}
     */
    public Exception getException() {
        return event.getException();
    }
    
    /**
     * {@inheritDoc}
     */
    public Throwable getThrowable() {
        return event.getThrowable();
    }
    
    /**
     * {@inheritDoc}
     */
    public long getWhen() {
        return event.getWhen();
    }
    
    /**
     * {@inheritDoc}
     */
    public String paramString() {
        return event.paramString();
    }
}