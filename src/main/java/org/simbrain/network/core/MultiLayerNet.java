package org.simbrain.network.core;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.pmw.tinylog.Logger;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Simbrain wrapper for a DL4J {@link MultiLayerNetwork}.
 *
 * @see {https://deeplearning4j.org/docs/latest/deeplearning4j-nn-multilayernetwork}
 */
public class MultiLayerNet implements ArrayConnectable {

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
    private WeightMatrix outgoingWeightMatrix;

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

    // public MultiLayerNet(List<Layer> layers) {
    //     NeuralNetConfiguration.ListBuilder listBuilder = new NeuralNetConfiguration.Builder()
    //             .updater(new Sgd(0.1))
    //             .seed(1234)
    //             .biasInit(0) // init the bias with 0 - empirical value, too
    //             // from "http://deeplearning4j.org/architecture": The networks can
    //             // process the input more quickly and more accurately by ingesting
    //             // minibatches 5-10 elements at a time in parallel.
    //             // this example runs better without, because the dataset is smaller than
    //             // the mini batch size
    //             .miniBatch(false)
    //             .list();
    //
    //     for (Layer layer : layers) {
    //         listBuilder = listBuilder.layer(layer);
    //         if (layer instanceof OutputLayer) {
    //             outputSize = ((OutputLayer) layer).getNOut();
    //         }
    //     }
    //
    //     input = Nd4j.zeros(1, ((DenseLayer) layers.get(0)).getNIn());
    //
    //
    //
    //     network = new MultiLayerNet(listBuilder.build());
    //     network.init();
    // }

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
    }


    //public static List<Layer> getLayerFromWeightMatrices(List<WeightMatrix> matrices) {
    //    List<Layer> ret = matrices.stream().map(WeightMatrix::asLayer).collect(Collectors.toList());
    //    ret.add(((NeuronArray) matrices.get(matrices.size() - 1).getTarget()).asLayer());
    //    return ret;
    //}


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
    public WeightMatrix getOutgoingWeightMatrix() {
        return outgoingWeightMatrix;
    }

    @Override
    public void setOutgoingWeightMatrix(WeightMatrix outgoingWeightMatrix) {
        this.outgoingWeightMatrix = outgoingWeightMatrix;
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
        return new Point2D.Double(location.getX() + 150 / 2.0, location.getY() + 50 / 2.0);
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

    // TODO: Separate class?
    /**
     * For creation using an {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public static class CreationTemplate implements EditableObject {

        @UserParameter( label = "Network topology", order = 1)
        private String topologyString = "4,10,2";

        @UserParameter( label = "Use minibatch", order = 10)
        private boolean minibatch;

        public MultiLayerNet create(Network parent) {

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
                    .updater(new Sgd(0.1))
                    .seed(1234)
                    .biasInit(0) // init the bias with 0 - empirical value, too
                    // from "http://deeplearning4j.org/architecture": The networks can
                    // process the input more quickly and more accurately by ingesting
                    // minibatches 5-10 elements at a time in parallel.
                    // this example runs better without, because the dataset is smaller than
                    // the mini batch size
                    .miniBatch(minibatch)
                    .list();

            for (int i = 0; i < netTopology.size() - 2; i++) {
                lb = lb.layer(
                        new DenseLayer.Builder().nIn(netTopology.get(i)).nOut(netTopology.get(i + 1))
                                .activation(Activation.SIGMOID)
                                // random initialize weights with values between 0 and 1
                                .weightInit(new UniformDistribution(0, 1))
                                .build()
                );
            }

            lb.layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                    .nOut(netTopology.get(netTopology.size() - 1))
                    .activation(Activation.SOFTMAX)
                    .weightInit(new UniformDistribution(0, 1))
                    .build());

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
}
