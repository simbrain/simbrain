package org.simbrain.network.DL4JSandbox;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.learning.config.Sgd;

import java.util.List;

public class DL4JMultiLayerNetwork {
    private MultiLayerNetwork network;

    public DL4JMultiLayerNetwork(List<Layer> layers) {
        NeuralNetConfiguration.ListBuilder listBuilder = new NeuralNetConfiguration.Builder()
                .updater(new Sgd(0.1))
                .seed(1234)
                .biasInit(0) // init the bias with 0 - empirical value, too
                // from "http://deeplearning4j.org/architecture": The networks can
                // process the input more quickly and more accurately by ingesting
                // minibatches 5-10 elements at a time in parallel.
                // this example runs better without, because the dataset is smaller than
                // the mini batch size
                .miniBatch(false)
                .list();

        for (Layer layer : layers) {
            listBuilder = listBuilder.layer(layer);
        }

        network = new MultiLayerNetwork(listBuilder.build());
        network.init();
    }

    public MultiLayerNetwork getNetwork() {
        return network;
    }
}
