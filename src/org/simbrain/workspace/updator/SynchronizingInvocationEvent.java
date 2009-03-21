/**
 * 
 */
package org.simbrain.workspace.updator;

import java.awt.event.InvocationEvent;
import java.util.concurrent.Callable;

class SynchronizingInvocationEvent extends InvocationEvent {
        private final InvocationEvent event;
        
        public SynchronizingInvocationEvent(final InvocationEvent event, final WorkspaceUpdator updator, final Signal signal) {
            super(event.getSource(), new Runnable() {
                public void run() {
                    try {
                        updator.syncOnAllComponents(new Callable<Object>() {
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
        
        public Exception getException() {
            return event.getException();
        }
        
        public Throwable getThrowable() {
            return event.getThrowable();
        }
        
        public long getWhen() {
            return event.getWhen();
        }
        
        public String paramString() {
            return event.paramString();
        }
        
        public interface Signal {
            void done();
        }
    }