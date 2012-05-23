package org.simbrain.workspace.updater;

/**
 * Simple interface for calling back when completed.
 * 
 * @author Matt Watson
 */
public interface CompletionSignal {

    /**
     * Signals that the event is done.
     */
    void done();
    
    /**
     * Simple signal with no implementation.
     */
    CompletionSignal IGNORE = new CompletionSignal() {
        public void done() {
            /* no implementation */
        }
    };
}