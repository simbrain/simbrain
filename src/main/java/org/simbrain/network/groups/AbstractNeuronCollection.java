package org.simbrain.network.groups;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.dl4j.ArrayConnectable;
import org.simbrain.network.dl4j.WeightMatrix;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Superclass for all neuron collection and neuron group.
 */
public abstract class AbstractNeuronCollection extends Group implements AttributeContainer, ArrayConnectable, LocatableModel {

    /**
     * References to neurons in this collection
     */
    private List<Neuron> neuronList = new CopyOnWriteArrayList<>();

    /**
     * Array to hold activation values for any caller that needs the activation values for this group in array form.
     * Lazy... activations are only written (and this array is only initialized) when {@link #getActivations()} is
     * called.
     */
    private double[] activations;

    private WeightMatrix incomingWeightMatrix;

    private List<WeightMatrix> outgoingWeightMatrices = new ArrayList<>();

    public AbstractNeuronCollection(Network net) {
        super(net);
    }

    /**
     * Returns all the neurons in this group within a certain radius of the
     * given neuron. This method will never return the given neuron as part
     * of the list of neurons within the given radius, nor will it return
     * neurons with the exact same position as the given neuron as a part
     * of the returned list.
     *
     * @param n      the neurons
     * @param radius the radius to search within.
     * @return neurons in the group within a certain radius
     */
    public List<Neuron> getNeuronsInRadius(Neuron n, int radius) {
        ArrayList<Neuron> ret = new ArrayList<Neuron>((int) (size() / 0.75f));
        for (Neuron potN : neuronList) {
            double dist = Network.getEuclideanDist(n, potN);
            if (dist <= radius && dist != 0) {
                ret.add(potN);
            }
        }
        return ret;
    }


    /**
     * Get the central x coordinate of this group, based on the positions of the neurons that comprise it.
     *
     * @return the center x coordinate.
     */
    public double getCenterX() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
            if (neuron.getX() > max) {
                max = neuron.getX();
            }
        }
        return min + (max - min) / 2;
    }

    /**
     * Get the central y coordinate of this group, based on the positions of the neurons that comprise it.
     *
     * @return the center y coordinate.
     */
    public double getCenterY() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
            if (neuron.getY() > max) {
                max = neuron.getY();
            }
        }
        return min + (max - min) / 2;
    }

    @Override
    public void setCenterX(double newx) {
        // todo
    }

    @Override
    public void setCenterY(double newy) {
        //todo
    }

    /**
     * Return the width of this group, based on the positions of the neurons that comprise it.
     *
     * @return the width of the group
     */
    public double getWidth() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
            if (neuron.getX() > max) {
                max = neuron.getX();
            }
        }
        return max - min;
    }

    /**
     * Return the height of this group, based on the positions of the neurons that comprise it.
     *
     * @return the height of the group
     */
    public double getHeight() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
            if (neuron.getY() > max) {
                max = neuron.getY();
            }
        }
        return max - min;
    }

    /**
     * @return the longest dimensions upon which neurons are laid out.
     */
    public double getMaxDim() {
        if (getWidth() > getHeight()) {
            return getWidth();
        } else {
            return getHeight();
        }
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    public void offset(final double offsetX, final double offsetY) {
        for (Neuron neuron : neuronList) {
            neuron.setX(neuron.getX() + offsetX);
            neuron.setY(neuron.getY() + offsetY);
            // TODO Below improves performance but there is a problem when creating neuron groups
            //neuron.setX(neuron.getX() + offsetX, false);
            //neuron.setY(neuron.getY() + offsetY, false);
        }
        firePositionChanged();
    }

    /**
     * Node positions within group changed and GUI should be notified of this
     * change.
     */
    public void firePositionChanged() {
        changeSupport.firePropertyChange("moved", null, null);
    }

    public Neuron getNeuron(int neuNo) {
        return neuronList.get(neuNo);
    }

    public void addNeuron(Neuron neuron) {
        neuronList.add(neuron);
        addListener(neuron);
    }

    public void addNeurons(Collection<Neuron> neurons) {
        neuronList.addAll(neurons);
        neurons.forEach(this::addListener);
    }

    /**
     * Add listener to indicated neuron.
     */
    private void addListener(Neuron n) {
        n.addPropertyChangeListener(evt -> {
            if ("moved".equals(evt.getPropertyName())) {
                firePositionChanged();
            }
        });

    }

    public void removeNeuron(Neuron neuron) {
        neuronList.remove(neuron);
    }

    public void removeAllNeurons() {
        neuronList.clear();
    }

    /**
     * Force set activations of neurons using an array of doubles. Assumes the order of the items in the array should
     * match the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    @Consumable()
    public void forceSetActivations(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).forceSetActivation(inputs[i]);
        }
    }

    /**
     * True if the group contains the specified neuron.
     *
     * @param n neuron to check for.
     * @return true if the group contains this neuron, false otherwise
     */
    public boolean containsNeuron(final Neuron n) {
        return neuronList.contains(n);
    }

    /**
     * Set clamping on all neurons in this group.
     *
     * @param clamp true to clamp them, false otherwise
     */
    public void setClamped(final boolean clamp) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setClamped(clamp);
        }
    }

    /**
     * Set all activations to a specified value.
     *
     * @param value the value to set the neurons to
     */
    public void setActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.setActivation(value);
        }
    }

    /**
     * Force set all activations to a specified value.
     *
     * @param value the value to set the neurons to
     */
    public void forceSetActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.forceSetActivation(value);
        }
    }

    /**
     * Copy activations from one neuron group to this one.
     *
     * @param toCopy the group to copy activations from.
     */
    public void copyActivations(AbstractNeuronCollection toCopy) {
        int i = 0;
        for (Neuron neuron : toCopy.getNeuronList()) {
            if (i < neuronList.size()) {
                neuronList.get(i).setActivation(neuron.getInputValue() + neuron.getActivation());
                neuronList.get(i++).setSpike(neuron.isSpike());

            }
        }
    }

    /**
     * Print activations as a vector.
     */
    public void printActivations() {
        System.out.println(Utils.doubleArrayToString(Network.getActivationVector(neuronList)));
    }

    /**
     * Randomize all neurons in group.
     */
    public void randomize() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomize();
        }
    }

    /**
     * Randomize bias for all neurons in group.
     *
     * @param lower lower bound for randomization.
     * @param upper upper bound for randomization.
     */
    public void randomizeBiases(double lower, double upper) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeBias(lower, upper);
        }
    }

    /**
     * Return flat list of fanins for all neurons in group.
     *
     * @return incoming weights
     */
    public List<Synapse> getIncomingWeights() {
        List<Synapse> retList = new ArrayList<Synapse>();
        for (Neuron neuron : this.getNeuronList()) {
            retList.addAll(neuron.getFanIn());
        }
        return retList;
    }

    /**
     * Return flat list of fanouts for all neurons in group.
     *
     * @return outgoing weights
     */
    public List<Synapse> getOutgoingWeights() {
        List<Synapse> retList = new ArrayList<Synapse>();
        for (Neuron neuron : this.getNeuronList()) {
            retList.addAll(neuron.getFanOut().values());
        }
        return retList;
    }

    /**
     * Randomize fan-in for all neurons in group.
     */
    public void randomizeIncomingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanIn();
        }
        getParentNetwork().fireSynapsesUpdated(getIncomingWeights());
    }

    /**
     * Randomize fan-out for all neurons in group.
     */
    public void randomizeOutgoingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanOut();
        }
        getParentNetwork().fireSynapsesUpdated(getOutgoingWeights());
    }

    public abstract void setNeuronType(String rule);

    /**
     * Set all activations to 0.
     */
    public void clearActivations() {
        for (Neuron n : this.getNeuronList()) {
            n.clear();
        }
    }

    @Override
    public INDArray getOutputArray() {
        float[] floatActivation = new float[getActivations().length];
        // Potential performance cost, but no clear way around this
        for (int i = 0; i < getActivations().length; i++) {
            floatActivation[i] = (float) getActivations()[i];
        }
        return Nd4j.create(new int[]{floatActivation.length}, floatActivation);

    }

    @Override
    public long inputSize() {
        return neuronList.size();
    }

    @Override
    public long outputSize() {
        return neuronList.size();
    }

    @Override
    public void setInputArray(INDArray activations) {
        setActivations(activations.toDoubleVector());
    }

    @Override
    public WeightMatrix getIncomingWeightMatrix() {
        return incomingWeightMatrix;
    }

    public void setIncomingWeightMatrix(WeightMatrix incomingWeightMatrix) {
        this.incomingWeightMatrix = incomingWeightMatrix;
    }

    @Override
    public List<WeightMatrix> getOutgoingWeightMatrices() {
        return outgoingWeightMatrices;
    }

    @Override
    public void addOutgoingWeightMatrix(WeightMatrix outgoingWeightMatrix) {
        this.outgoingWeightMatrices.add(outgoingWeightMatrix);
    }

    @Override
    public void removeOutgoingWeightMatrix(WeightMatrix weightMatrix) {
        this.outgoingWeightMatrices.remove(weightMatrix);
    }

    @Override
    public abstract void setLocation(Point2D location);

    @Override
    public Point2D getAttachmentPoint() {
        return new Point2D.Double(SimnetUtils.getMinX(neuronList), SimnetUtils.getMinY(neuronList));
    }

    @Override
    public void onLocationChange(Runnable task) {
        changeSupport.addPropertyChangeListener(evt -> {
            if ("moved".equals(evt.getPropertyName())) {
                task.run();
            }
        });
    }

    /**
     * Return current position (upper left corner of neuron in the farthest north-west position.
     *
     * @return position upper left position of group
     */
    public Point2D.Double getPosition() {
        return new Point2D.Double(SimnetUtils.getMinX(neuronList), SimnetUtils.getMaxX(neuronList));
    }

    /**
     * Returns true if all the neurons in this group are clamped.
     *
     * @return true if all neurons are clamped, false otherwise
     */
    public boolean isAllClamped() {
        boolean ret = true;
        for (Neuron n : getNeuronList()) {
            if (!n.isClamped()) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Returns true if all the neurons in this group are unclamped.
     *
     * @return true if all neurons are unclamped, false otherwise
     */
    public boolean isAllUnclamped() {
        boolean ret = true;
        for (Neuron n : getNeuronList()) {
            if (n.isClamped()) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Set the lower bound on all neurons in this group.
     *
     * @param lb the lower bound to set.
     */
    public void setLowerBound(double lb) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setLowerBound(lb);
        }
    }

    /**
     * Set the upper bound on all neurons in this group.
     *
     * @param ub the upper bound to set.
     */
    public void setUpperBound(double ub) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setUpperBound(ub);
        }
    }

    /**
     * Set the increment on all neurons in this group.
     *
     * @param increment the increment to set.
     */
    public void setIncrement(double increment) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setIncrement(increment);
        }
    }

    /**
     * Utility to method (used in couplings) to get a string showing the labels of all "active" neurons (neurons with
     * activation above a threshold).
     *
     * @param threshold threshold above which to consider a neuron "active"
     * @return the "active labels"
     */
    public String getLabelsOfActiveNeurons(double threshold) {
        StringBuilder strBuilder = new StringBuilder("");
        for (Neuron neuron : neuronList) {
            if ((neuron.getActivation() > threshold) && (!neuron.getLabel().isEmpty())) {
                strBuilder.append(neuron.getLabel() + " ");
            }
        }
        return strBuilder.toString();
    }

    /**
     * Returns the label of the most active neuron.
     *
     * @return the label of the most active neuron
     */
    public String getMostActiveNeuron() {
        double min = Double.MIN_VALUE;
        String result = "";
        for (Neuron neuron : neuronList) {
            if ((neuron.getActivation() > min) && (!neuron.getLabel().isEmpty())) {
                result = neuron.getLabel();
                min = neuron.getActivation();
            }
        }
        return result + " ";
    }

    /**
     * Sets the polarities of every neuron in the group.
     *
     * @param p
     */
    public void setPolarity(SimbrainConstants.Polarity p) {
        for (Neuron n : neuronList) {
            n.setPolarity(p);
        }
    }

    /**
     * Get the neuron with the specified label, or null if none found.
     *
     * @param label label to search for
     * @return the associated neuron
     */
    public Neuron getNeuronByLabel(String label) {
        return neuronList.stream()
                .filter(n -> n.getLabel().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }


    /**
     * Set input values of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    @Consumable()
    public void setInputValues(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).setInputValue(inputs[i]);
        }
    }

    /**
     * Adds input values.  Useful when doing a many to one coupling.
     */
    @Consumable()
    public void addInputValues(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).addInputValue(inputs[i]);
        }
    }

    /**
     * Set activations of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    @Consumable()
    public void setActivations(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).setActivation(inputs[i]);
        }
    }

    @Producible(arrayDescriptionMethod = "getLabelArray")
    public double[] getActivations() {
        if (activations == null) {
            activations = new double[size()];
        }
        for (int ii=0; ii<size(); ++ii) {
            activations[ii] = neuronList.get(ii).getActivation();
        }
        return activations;
    }

    /**
     * Returns an array of labels, one for each neuron this group.
     * Called by reflection for some coupling related events.
     *
     * @return the label array
     */
    public String[] getLabelArray() {
        String[] retArray = new String[getNeuronList().size()];
        int i = 0;
        for(Neuron neuron : getNeuronList()) {
            if (neuron.getLabel().isEmpty()) {
                retArray[i++] = neuron.getId();
            } else {
                retArray[i++] = neuron.getLabel();
            }
        }
        return retArray;
    }

    @Override
    public Network getNetwork() {
        return getParentNetwork();
    }

    public List<Neuron> getNeuronList() {
        return Collections.unmodifiableList(neuronList);
    }

    @Override
    public boolean isEmpty() {
        return neuronList.isEmpty();
    }

    @Override
    public int size() {
        return neuronList.size();
    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * See {@link SimnetUtils#getMinX(List)}
     */
    public double getMinX() {
        return SimnetUtils.getMinX(neuronList);
    }

    /**
     * See {@link SimnetUtils#getMaxX(List)}
     */
    public double getMaxX() {
        return SimnetUtils.getMaxX(neuronList);
    }

    /**
     * See {@link SimnetUtils#getMinY(List)}
     */
    public double getMinY() {
        return SimnetUtils.getMinY(neuronList);
    }

    /**
     * See {@link SimnetUtils#getMaxY(List)}
     */
    public double getMaxY() {
        return SimnetUtils.getMaxY(neuronList);
    }
}
