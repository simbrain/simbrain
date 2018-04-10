package org.simbrain.custom_sims.simulations.neat;

/**
 * Procedure usually contains a list of ProcedureAction, and can be run using executor.
 * The purpose of this is later, there may be a GUI interface for use to design their own NEAT simulation
 * in Simbrain. A procedure will become something similar to method in Java, and allows user to define
 * fitness function or other functions (e.g. setting entity location in a odor world under some condition).
 * @author LeoYulinLi
 *
 */
public interface Procedure extends Runnable {
    @Override
    void run();
}
