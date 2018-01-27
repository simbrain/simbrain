package org.simbrain.workspace.updater;

public class UpdateActionTimer {

    private UpdateAction timedAction;
    private double durationMs;

    /**
     * Create a new UpdateActionTimer.
     * @param timedAction The update action to be timed.
     */
    public UpdateActionTimer(UpdateAction timedAction) {
        this.timedAction = timedAction;
    }

    /** Invoke the timed update action and update the duration. */
    public void invokeTimedAction() {
        long startTime = System.nanoTime();
        timedAction.invoke();
        durationMs = ((System.nanoTime() - startTime) / 1.0e6);
    }

    public double getDurationMs() {
        return durationMs;
    }

    public UpdateAction getTimedAction() {
        return timedAction;
    }

}
