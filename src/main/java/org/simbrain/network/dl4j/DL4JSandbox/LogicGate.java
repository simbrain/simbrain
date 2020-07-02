package org.simbrain.network.dl4j.DL4JSandbox;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.evaluation.classification.Evaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *
 * +---------+---------+---------------+--------------+
 * | Input 1 | Input 2 | Label 1(NAND) | Label 2(AND) |
 * +---------+---------+---------------+--------------+
 * |    0    |    0    |       1       |       0      |
 * +---------+---------+---------------+--------------+
 * |    1    |    0    |       1       |       0      |
 * +---------+---------+---------------+--------------+
 * |    0    |    1    |       1       |       0      |
 * +---------+---------+---------------+--------------+
 * |    1    |    1    |       0       |       1      |
 * +---------+---------+---------------+--------------+
 *
 * <b>Reference: https://deeplearning4j.konduit.ai/getting-started/quickstart</b>
 * @author Ken Fukuyama
 * @version 1.0
 *
 */

public class LogicGate {

    // Declaring a Logger object to store messages. See logback.xml to set log level
    private static final Logger log = LoggerFactory.getLogger(LogicGate.class);

    public static void main(String[] args) {
        int seed = 42;        // number used to initialize a pseudorandom number generator.
        int nEpochs = 10;    // number of training epochs

        // Preparing
        //##################################################################################
        log.info("Data preparation...");

        // Input array (2 columns) for 4 training data (4 rows). (Training data)
        // Initialize with ones.
        INDArray inputArr = Nd4j.ones(4, 2);

        // Expected output array (2 columns) for 4 training data (4 rows). (Test data)
        // Initialize with ones.
        INDArray labelsArr = Nd4j.ones(4, 2);

        // Initialize 4x2 input array
        inputArr.putScalar(new int[]{0, 0}, 0);
        inputArr.putScalar(new int[]{0, 1}, 0);
        inputArr.putScalar(new int[]{1, 0}, 1);
        inputArr.putScalar(new int[]{1, 1}, 0);
        inputArr.putScalar(new int[]{2, 0}, 0);
        inputArr.putScalar(new int[]{2, 1}, 1);
        inputArr.putScalar(new int[]{3, 0}, 1);
        inputArr.putScalar(new int[]{3, 1}, 1);

        // Initialize 4x2 label array
        labelsArr.putScalar(new int[]{0, 0}, 1);
        labelsArr.putScalar(new int[]{0, 1}, 0);
        labelsArr.putScalar(new int[]{1, 0}, 1);
        labelsArr.putScalar(new int[]{1, 1}, 0);
        labelsArr.putScalar(new int[]{2, 0}, 1);
        labelsArr.putScalar(new int[]{2, 1}, 0);
        labelsArr.putScalar(new int[]{3, 0}, 0);
        labelsArr.putScalar(new int[]{3, 1}, 1);

        // Create dataset object with input and output neurons.
        DataSet dataset = new DataSet(inputArr, labelsArr);

        log.info("Configurating the network and training...");
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                // Using stochastic gradient decent
                .updater(new Sgd(0.2))
                .seed(seed)
                .biasInit(0)
                .miniBatch(false)
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(2)
                        .nOut(4)
                        // Choosing activation function.
                        .activation(Activation.RELU)
                        // create a uniform distributed weights with values between 0 and 1
                        .weightInit(new UniformDistribution(0, 1))
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(2)
                        .activation(Activation.SOFTMAX)
                        .weightInit(new UniformDistribution(0, 1))
                        .build())
                .build();

        MultiLayerNetwork multiLayerNetwork = new MultiLayerNetwork(config);
        multiLayerNetwork.init();

        // Set the interval of iterations to be displayed on the console.
        multiLayerNetwork.setListeners(new ScoreIterationListener(200));
        // Shows the summary of network configuration.
        System.out.println(multiLayerNetwork.summary());


        // Training
        //##################################################################################
        log.info("Training neural network");
        for (int i = 0; i < nEpochs; i++) {
            // fitting the dataset nEpochs time.
            multiLayerNetwork.fit(dataset);
        }


        //##################################################################################
        // Using training data (input array), which is dataset.getFeatures to perform forward pass
        // Return the output of the final layer
        // Ctrl+ Q (Windows) for documentation
        INDArray outputArr = multiLayerNetwork.output(dataset.getFeatures());

        // Evaluation using the correct label (labelsArr) and compare the result with outputArr.
        Evaluation evaluation = new Evaluation();
        evaluation.eval(dataset.getLabels(), outputArr);
        // Print out the final evaluation result.
        System.out.println(evaluation.stats());


    }

}
