package org.simbrain.network.dl4j;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.pmw.tinylog.Logger;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.trainers.ErrorListener;
import org.simbrain.network.trainers.IterableTrainerTemp;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Simbrain wrapper for a DL4J {@link MultiLayerNetwork}.
 *
 * @see {https://deeplearning4j.org/docs/latest/deeplearning4j-nn-multilayernetwork}
 */
public class MultiLayerNet implements ArrayConnectable, IterableTrainerTemp, LocatableModel {

    /**
     * The main dl4j object being wrapped
     */
    private MultiLayerNetwork network;

    /**
     * Reference to parent network
     */
    private Network parent;

    /**
     * Buffer for inputs to this network
     */
    private INDArray input;

    /**
     * Reference to incoming weight matrix.
     */
    private WeightMatrix incomingWeightMatrix;

    /**
     * Reference to outgoing weight matrix.
     */
    private List<WeightMatrix> outgoingWeightMatrices = new ArrayList<>();

    /**
     * Cached size of the output layer.
     */
    private int outputSize;

    /**
     * Cached sizes of the layers of the network.
     */
    private List<Integer> topology;

    /**
     * Location of the upper-left of the network.
     */
    private Point2D location = new Point2D.Double();

    /**
     * Support for property change events.
     */
    private transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    // todo
    private DataSet dataset;
    private int iteration = 0;
    private List<ErrorListener> errorListeners = new ArrayList<ErrorListener>();
    private boolean updateCompleted = true;
    /**
     * Construct a multi layer network from a specification of its topology. E.g. 4,3,5 is a network with 4 units in the
     * input layer, 3 in the hidden layer, and 5 in the output layer.
     * <p>
     * Note that in the dl4j terminology this would be one 4,5 dense layer, and a "5" output layer
     *
     * @param parent parent network
     */
    public MultiLayerNet(Network parent, List<Integer> netTopology, MultiLayerConfiguration conf) {

        this.parent = parent;
        this.topology = netTopology;

        if (topology.size() < 2) {
            throw new IllegalArgumentException("Sizes must have least 2 elements");
        }

        input = Nd4j.zeros(1, topology.get(0));

        outputSize = topology.get(topology.size() - 1);

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        network = net;

        dataset = new DataSet(input, getOutputArray());
    }

    public MultiLayerNetwork getMultiLayernet() {
        return network;
    }

    @Override
    public INDArray getOutputArray() {
        return network.output(input);
    }

    @Override
    public void setInputArray(INDArray activations) {
        input = activations;
    }

    public List<Integer> getTopology() {
        return topology;
    }

    @Override
    public long inputSize() {
        return input.length();
    }

    @Override
    public long outputSize() {
        return outputSize;
    }

    @Override
    public WeightMatrix getIncomingWeightMatrix() {
        return incomingWeightMatrix;
    }

    @Override
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
    public String getId() {
        return null;
    }

    @Override
    public void setLocation(Point2D location) {
        this.location = location;
        fireLocationChange();
    }

    @Override
    public Point2D getAttachmentPoint() {
        return new Point2D.Double(location.getX(), location.getY());
    }

    @Override
    public void onLocationChange(Runnable task) {
        addPropertyChangeListener(evt -> {
            if ("moved".equals(evt.getPropertyName())) {
                task.run();
            }
        });
    }

    @Override
    public Network getNetwork() {
        return parent;
    }

    public void fireLocationChange() {
        changeSupport.firePropertyChange("moved", null, null);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public String toString() {
        return Arrays.stream(network.getLayers()).map(l -> l.getClass().getSimpleName()).collect(Collectors.joining(", "));
    }

    @Override
    public void iterate() throws Trainer.DataNotInitializedException {
        network.fit(dataset);
        iteration++;
        fireErrorUpdated();
    }

    @Override
    public void addErrorListener(final ErrorListener errorListener) {
        if (errorListeners == null) {
            errorListeners = new ArrayList<ErrorListener>();
        }
        errorListeners.add(errorListener);
    }


    /**
     * Notify listeners that the error value has been updated. Only makes sense
     * for iterable methods.
     */
    public void fireErrorUpdated() {
        for (ErrorListener listener : errorListeners) {
            listener.errorUpdated();
        }
    }

    @Override
    public void removeErrorListener(ErrorListener errorListener) {
        errorListeners.remove(errorListener);
    }

    @Override
    public int getIteration() {
        return iteration;
    }

    @Override
    public double getError() {
        return network.score();
    }

    @Override
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    @Override
    public void setUpdateCompleted(boolean uc) {
        this.updateCompleted = uc;
    }

    // TODO
    public void initData(INDArray input, INDArray targets) {
        dataset.setFeatures(input);
        dataset.setLabels(targets);
    }

    @Override
    public void commitChanges() {
        System.out.println("MultiLayerNet.commitChanges");
    }

    @Override
    public void randomize() {
        // todo
    }

    @Override
    public void revalidateSynapseGroups() {

    }

    // TODO
    @Override
    public double getCenterX() {
        return location.getX();
    }

    @Override
    public double getCenterY() {
        return location.getY();
    }

    @Override
    public void setCenterX(double newx) {
        location.setLocation(newx, location.getY());
        fireLocationChange();
    }

    @Override
    public void setCenterY(double newy) {
        location.setLocation(location.getX(), newy);
        fireLocationChange();
    }

    /**
     * For creation using an {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public static class CreationTemplate implements EditableObject {

        @UserParameter( label = "Network topology", order = 1)
        private String topologyString = "5,10,5";

        @UserParameter( label = "Optimizer", order = 10)
        private OptimizationAlgorithm optimizationAlgorithm = OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT;

        @UserParameter( label = "Use minibatch", order = 20)
        private boolean minibatch;

        public MultiLayerNet create(Network parent, LayerCreationTemplate lct,
                                    OutputLayerCreationTemplate oct) {

            List<Integer> netTopology;

            try {
                netTopology = Arrays.stream(topologyString.split(","))
                        .map(Integer::valueOf)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                e.printStackTrace();
                Logger.warn("Incorrect layer size list.");
                netTopology = Arrays.asList(4, 10, 2);
            }

            NeuralNetConfiguration.ListBuilder lb = new NeuralNetConfiguration.Builder()
                    .seed(1234)
                    .optimizationAlgo(optimizationAlgorithm)
                    .biasInit(0) // init the bias with 0 - empirical value, too
                    // from "http://deeplearning4j.org/architecture": The networks can
                    // process the input more quickly and more accurately by ingesting
                    // minibatches 5-10 elements at a time in parallel.
                    // this example runs better without, because the dataset is smaller than
                    // the mini batch size
                    .miniBatch(minibatch)
                    .list();

            // Set up main layers using layer creation template
            for (int i = 0; i < netTopology.size() - 2; i++) {
                lb = lb.layer(lct.create(netTopology.get(i), netTopology.get(i+1)));
            }

            // Set up output layers using output layer creation template
            lb.layer(oct.create(netTopology.get(netTopology.size() - 1)));

            MultiLayerConfiguration conf = lb.build();
            MultiLayerNet net =
                    new MultiLayerNet(parent, netTopology, conf);
            return net;
        }

        @Override
        public String getName() {
            return "Multi-Layer Network";
        }

    }

    public static class LayerCreationTemplate implements EditableObject {

        @UserParameter(label = "Activation Function", order = 1)
        private Activation actFunc = Activation.SIGMOID;

        @UserParameter(label = "Use bias", order = 10)
        private boolean hasBias = true;

        // TODO: Weight Init, and a bajillion other thigns

        public Layer create(int numSrc, int numTar) {
            return new DenseLayer.Builder()
                    .nIn(numSrc)
                    .nOut(numTar)
                    .hasBias(hasBias)
                    .activation(actFunc)
                    // random initialize weights with values between 0 and 1
                    .weightInit(new UniformDistribution(0, 1))
                    .build();
        }
    }

    public static class OutputLayerCreationTemplate implements EditableObject {

        @UserParameter(label = "Loss Function", order = 1)
        private LossFunctions.LossFunction lossFunc = LossFunctions.LossFunction.MSE;

        @UserParameter(label = "Activation Function", order = 2)
        private Activation actFunc = Activation.SIGMOID;

        @UserParameter(label = "Use bias", order = 10)
        private boolean hasBias = true;

        // TODO: Weight Init, and a bajillion other things
        // TODO: Not all loss functions are compatible with all layer types.
        // Somehow deal with DL4JInvalidConfigException here

        public Layer create(int numNodes) {
            return new OutputLayer.Builder(lossFunc)
                    .nOut(numNodes)
                    .activation(actFunc)
                    .weightInit(new UniformDistribution(0, 1))
                    .build();
        }
    }

}
