// package org.simbrain.network.subnetworks;
//
// import junit.framework.TestCase;
// import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
// import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
// import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
// import org.deeplearning4j.nn.conf.layers.DenseLayer;
// import org.deeplearning4j.nn.conf.layers.OutputLayer;
// import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
// import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
// import org.junit.jupiter.api.Test;
// import org.nd4j.evaluation.classification.Evaluation;
// import org.nd4j.evaluation.regression.RegressionEvaluation;
// import org.nd4j.linalg.activations.Activation;
// import org.nd4j.linalg.api.ndarray.INDArray;
// import org.nd4j.linalg.dataset.DataSet;
// import org.nd4j.linalg.factory.Nd4j;
// import org.nd4j.linalg.learning.config.Sgd;
// import org.nd4j.linalg.lossfunctions.LossFunctions;
// import org.simbrain.network.core.Network;
//
// import java.util.Arrays;
//
// public class LMSNetworkTest extends TestCase {
//
//     // @Test
//     // public void testLMSNet() {
//     //
//     //     Network net = new Network();
//     //     LMSNetwork lms = new LMSNetwork(net, 5,5,null);
//     //
//     //     lms.getTrainingSet().setInputData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});
//     //     lms.getTrainingSet().setTargetData(new double[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}});
//     //
//     //     //lms.getTrainer().apply();
//     //     assertEquals(0,0);
//     //
//     // }
//     //
//     // public void testName() {
//     //     INDArray inputs = Nd4j.eye(5);
//     //     INDArray targets = inputs.dup();
//     //
//     //     DataSet dataset = new DataSet(inputs, targets);
//     //
//     //     MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
//     //             // Using stochastic gradient decent
//     //             .updater(new Sgd(0.2))
//     //             .seed(1)
//     //             .biasInit(0)
//     //             .miniBatch(false)
//     //             .list()
//     //             .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR)
//     //                     .nIn(5)
//     //                     .nOut(5)
//     //                     .activation(Activation.RELU)
//     //                     .weightInit(new UniformDistribution(0, 1))
//     //                     .build())
//     //             .build();
//     //
//     //     MultiLayerNetwork net = new MultiLayerNetwork(config);
//     //     net.init();
//     //
//     //     // System.out.println(net.summary());
//     //
//     //     // net.setListeners(new ScoreIterationListener(5));
//     //
//     //     for (int i = 0; i < 25; i++) {
//     //         // fitting the dataset nEpochs time.
//     //         net.fit(dataset);
//     //         // System.out.println("score:" + net.score());
//     //     }
//     //
//     //     var results = net.feedForward(inputs);
//     //     // results.forEach((row) -> {
//     //     //     System.out.println("------");
//     //     //     System.out.println(row);
//     //     //     System.out.println("-------");
//     //     // });
//     //
//     //     // var evaluation = new RegressionEvaluation();
//     //     // evaluation.eval(dataset.getLabels(), net.output(dataset.getFeatures()));
//     //     // System.out.println(evaluation.stats());
//     //
//     //     // Check that error is sufficiently low
//     //     assertTrue(net.score() < .1);
//     // }
//     //
//     // @Test
//     // public void testMln() {
//     //
//     //     MultiLayerNetwork mln;
//     //
//     //     MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
//     //             // Using stochastic gradient decent
//     //             .updater(new Sgd(.01))
//     //             .seed(1)
//     //             .biasInit(0)
//     //             .miniBatch(true)
//     //             .list()
//     //             .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
//     //                     .nIn(3)
//     //                     .nOut(2)
//     //                     .activation(Activation.SIGMOID)
//     //                     .weightInit(new UniformDistribution(0, 1)) //TODO
//     //                     .build())
//     //             .build();
//     //
//     //     mln = new MultiLayerNetwork(config);
//     //     mln.init();
//     //
//     //     // How to get parameters
//     //     System.out.println(mln.getLayer(0).paramTable());
//     //     System.out.println(mln.getLayer(0).getParam("W"));
//     //     System.out.println(mln.getLayer(0).getParam("b"));
//     //     System.out.println(mln.getLayer(0).params());
//     //     assertTrue(1==1);
//     // }
// }