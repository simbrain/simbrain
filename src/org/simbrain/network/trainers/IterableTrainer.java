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

import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor.EditableObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Superclass for all types of trainer which can be iterated and which return an
 * error when they are iterated.
 *
 * @author jyoshimi
 */
public abstract class IterableTrainer extends Trainer implements EditableObject {

    //TODO: Fine-tune the useparameter annotations to optimize the editor dialog

    /**
     * Flag used for iterative training methods.
     */
    private boolean updateCompleted = true;

    /**
     * Listener list.
     */
    private List<ErrorListener> errorListeners = new ArrayList<ErrorListener>();

    /**
     * Iteration number. An epoch.
     */
    private int iteration;

    /**
     * Current stopping condition.
     */
    @UserParameter(
        label = "Stopping condition",
        description = "Stopping condition", order = 2)
    private StoppingCondition stoppingCondition = StoppingCondition.NONE;

    /**
     * Iterate through the dataset polling random rows.
     */
    // TODO: use this
    @UserParameter(label = "Stochastic Iteration")
    private boolean stochasticIteration = false;

    @UserParameter(label = "Randomizer", isObjectType = true, order = 1000)
    private ProbabilityDistribution randomizer = UniformDistribution.create();

    /** If used, stopped iterating if validation error is below this. */
    //private double validationErrorThreshold = .2;

    /**
     * If used, stop iterating if error is below this value.
     */
    @UserParameter(label = "Error Threshold")
    private double errorThreshold = .2;

    /**
     * When stopping condition is based on iterations, stop when iterations
     * exceeds this value.
     */
    @UserParameter(label = "Iterations for Before Stopping")
    private int iterationsBeforeStopping = 1000;

    /**
     * Stopping condition.
     */
    public enum StoppingCondition {
        THRESHOLD_ERROR {
            public String toString() {
                return "Threshold error";
            }
        }, THRESHOLD_VALIDATION_ERROR {
            public String toString() {
                return "Threshold error in validation set";
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

    /**
     * Construct the iterable trainer.
     *
     * @param network the trainable network
     */
    public IterableTrainer(Trainable network) {
        super(network);
    }

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

    /**
     * Get the current MSE error.
     *
     * @return the current MSE error
     */
    public double getValidationError() {
        return 0;
    }

    /**
     * Iterate the training algorithm and stop iteration based on the selected
     * stopping condition.
     *
     * @throws DataNotInitializedException if input or target data not set
     */
    public void iterate() throws DataNotInitializedException {

        if (getTrainableNetwork().getTrainingSet().getInputData() == null) {
            throw new DataNotInitializedException("Input data not initalized");
        }
        if (getTrainableNetwork().getTrainingSet().getTargetData() == null) {
            throw new DataNotInitializedException("Target data not initalized");
        }

        fireTrainingBegin();
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
            case THRESHOLD_VALIDATION_ERROR:
                break;
            default:
                break;
        }
        fireTrainingEnd();

    }

    /**
     * Notify listeners that the error value has been updated. Only makes sense
     * for iterable methods.
     */
    public void fireErrorUpdated() {
        for (ErrorListener listener : getErrorListeners()) {
            listener.errorUpdated();
        }
    }

    /**
     * @return boolean updated completed.
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Sets updated completed value.
     *
     * @param updateCompleted Updated completed value to be set
     */
    public void setUpdateCompleted(final boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
    }

    /**
     * Increment the iteration number by 1.
     */
    public void incrementIteration() {
        iteration++;
    }

    /**
     * @param iteration the iteration to set
     */
    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    /**
     * Return the current iteration.
     *
     * @return current iteration.
     */
    public int getIteration() {
        return iteration;
    }

    /**
     * @return the errorListener
     */
    public List<ErrorListener> getErrorListeners() {
        return Collections.unmodifiableList(errorListeners);
    }

    /**
     * Add an error listener.
     *
     * @param errorListener the listener to add
     */
    public void addErrorListener(final ErrorListener errorListener) {
        if (errorListeners == null) {
            errorListeners = new ArrayList<ErrorListener>();
        }
        errorListeners.add(errorListener);
    }

    /**
     * Remove an error listener.
     *
     * @param errorListener the listener to remove
     */
    public void removeErrorListener(final ErrorListener errorListener) {
        if (errorListeners != null) {
            errorListeners.remove(errorListener);
        }
    }

    /**
     * Returns the number of rows in whichever dataset has fewer rows. Used when
     * mismatched datasets are in use.
     *
     * @return least number of rows
     */
    protected int getMinimumNumRows(Trainable network) {
        if ((network.getTrainingSet().getInputData() == null) || (network.getTrainingSet() == null)) {
            return 0;
        }
        int inputRows = network.getTrainingSet().getInputData().length;
        int targetRows = network.getTrainingSet().getTargetData().length;
        if (inputRows < targetRows) {
            return inputRows;
        } else {
            return targetRows;
        }
    }

    /**
     * @return the stoppingCondition
     */
    public StoppingCondition getStoppingCondition() {
        return stoppingCondition;
    }

    /**
     * @param stoppingCondition the stoppingCondition to set
     */
    public void setStoppingCondition(StoppingCondition stoppingCondition) {
        this.stoppingCondition = stoppingCondition;
    }

    /**
     * @return the iterationsBeforeStopping
     */
    public int getIterationsBeforeStopping() {
        return iterationsBeforeStopping;
    }

    /**
     * @param iterationsBeforeStopping the number of iterations before stopping.
     */
    public void setIterationsBeforeStopping(int iterationsBeforeStopping) {
        this.iterationsBeforeStopping = iterationsBeforeStopping;
    }

    /**
     * @return the errorThreshold
     */
    public double getErrorThreshold() {
        return errorThreshold;
    }

    /**
     * @param errorThreshold the errorThreshold to set
     */
    public void setErrorThreshold(double errorThreshold) {
        this.errorThreshold = errorThreshold;
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

}
