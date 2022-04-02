/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.trainers;

import org.simbrain.network.events.TrainerEvents;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor.EditableObject;

/**
 * Superclass for all types of trainer which can be iterated and which return an
 * error when they are iterated.
 *
 * @author jyoshimi
 */
public abstract class IterableTrainer implements EditableObject {

    //TODO: Fine-tune the useparameter annotations to optimize the editor dialog

    /**
     * Flag used for iterative training methods.
     */
    private boolean updateCompleted = true;

    /**
     * Iteration number. An epoch.
     */
    private int iteration;

    /**
     * Handle trainer events.
     */
    private transient TrainerEvents events = new TrainerEvents(this);

    /**
     * Current stopping condition.
     */
    @UserParameter(
        label = "Stopping condition",
        description = "Stopping condition", order = 100)
    private StoppingCondition stoppingCondition = StoppingCondition.NONE;

    /**
     * If used, stop iterating if error is below this value.
     */
    @UserParameter(label = "Error Threshold", order = 110)
    private double errorThreshold = .2;

    /**
     * When stopping condition is based on iterations, stop when iterations
     * exceeds this value.
     */
    @UserParameter(label = "Iterations for Before Stopping", order = 120)
    private int iterationsBeforeStopping = 1000;

    public IterableTrainer() {
        super();
    }

    /**
     * Stopping condition.
     */
    public enum StoppingCondition {
        THRESHOLD_ERROR {
            public String toString() {
                return "Threshold error";
            }
        }, NUM_ITERATIONS {
            public String toString() {
                return "Number of epochs";
            }
        }, NONE {
            public String toString() {
                return "None (keep going until manual stop)";
            }
        }
    }

    @UserParameter(label = "Randomizer", isObjectType = true, order = 200)
    private ProbabilityDistribution randomizer = new UniformDistribution();

    /**
     * Get the current MSE error.
     *
     * @return the current MSE error
     */
    public abstract double getError();

    /**
     * Randomize the network being trained. Convenience method so guis wrapping
     * trainers have access to network randomization when the user wishes to
     * "restart" training.
     */
    public abstract void randomize();

    //TODO
    public void iterate2() throws DataNotInitializedException  {
        apply();
        incrementIteration();
        events.fireErrorUpdated();
    }

    /**
     * Iterate the training algorithm and stop iteration based on the selected
     * stopping condition.
     *
     * @throws DataNotInitializedException if input or target data not set
     */
    public void iterate() throws DataNotInitializedException {

        if (getTrainingSet().getInputData() == null) {
            throw new DataNotInitializedException("Input data not initalized");
        }
        if (getTrainingSet().getTargetData() == null) {
            throw new DataNotInitializedException("Target data not initalized");
        }

        events.fireBeginTraining();
        switch (stoppingCondition) {
            case NONE:
                apply();
                break;
            case NUM_ITERATIONS:
                for (int i = 0; i < iterationsBeforeStopping; i++) {
                    if (updateCompleted) {
                        break;
                    }
                    apply();
                }
                setUpdateCompleted(true);
                break;
            case THRESHOLD_ERROR:
                do {
                    apply();
                } while ((getError() > errorThreshold) && (!updateCompleted));
                setUpdateCompleted(true);
                break;
            default:
                break;
        }
        events.fireEndTraining();

    }

    protected abstract TrainingSet getTrainingSet();

    public abstract void apply() throws DataNotInitializedException;

    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    public void setUpdateCompleted(final boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
    }

    public void incrementIteration() {
        iteration++;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public int getIteration() {
        return iteration;
    }

    /**
     * Returns the number of rows in whichever dataset has fewer rows. Used when
     * mismatched datasets are in use.
     *
     * @return least number of rows
     */
    protected int getMinimumNumRows() {
        if ((getTrainingSet().getInputData() == null) || (getTrainingSet() == null)) {
            return 0;
        }
        int inputRows = getTrainingSet().getInputData().length;
        int targetRows = getTrainingSet().getTargetData().length;
        if (inputRows < targetRows) {
            return inputRows;
        } else {
            return targetRows;
        }
    }

    /**
     * Called when datatables are changed. Override if the trainer needs to
     * initialize any internal variables when this happens.
     */
    public void initData() {
    }

    /**
     * Called when the trainer is closed.  Override if needed.
     */
    public void commitChanges() {
    }

    public ProbabilityDistribution getRandomizer() {
        return randomizer;
    }

    /**
     * Exception thrown when a training algorithm is applied but no data have
     * been initialized.
     */
    public class DataNotInitializedException extends Exception {

        public DataNotInitializedException(final String message) {
            super(message);
        }

    }

    public TrainerEvents getEvents() {
        return events;
    }
}
