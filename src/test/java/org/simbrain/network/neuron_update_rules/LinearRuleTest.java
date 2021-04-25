// package org.simbrain.network.neuron_update_rules;
//
// import org.junit.jupiter.api.Test;
// import org.simbrain.network.core.Network;
// import org.simbrain.network.core.Neuron;
// import org.simbrain.network.core.Synapse;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
//
// public class LinearRuleTest {
//
//     @Test
//     public void testUpdate() {
//
//         // Default update rule is linear
//         Network net = new Network();
//         Neuron output = new Neuron(net);
//         output.setUpperBound(10);
//         net.addNetworkModel(output);
//
//         Neuron input1 = new Neuron(net);
//         input1.setActivation(1);
//         input1.setClamped(true);
//         net.addNetworkModel(input1);
//         Neuron input2 = new Neuron(net);
//         input2.setActivation(-1);
//         input2.setClamped(true);
//         net.addNetworkModel(input2);
//
//         Synapse w13 = new Synapse(input1, output);
//         w13.setStrength(.5);
//         net.addNetworkModel(w13);
//         Synapse w23 = new Synapse(input2, output);
//         w23.setStrength(-1);
//         net.addNetworkModel(w23);
//
//         // 1*.5 + -1*-1 = .5 + 1 = 1.5
//         net.update();
//         assertEquals(1.5, output.getActivation(), 0);
//
//         // Testing two negative slopes.
//         input1 = new Neuron(net);
//         input1.setActivation(1);
//         input1.setClamped(true);
//         net.addNetworkModel(input1);
//         input2 = new Neuron(net);
//         input2.setActivation(-1);
//         input2.setClamped(true);
//         net.addNetworkModel(input2);
//
//         w13 = new Synapse(input1, output);
//         w13.setStrength(-0.8);
//         net.addNetworkModel(w13);
//         w23 = new Synapse(input2, output);
//         w23.setStrength(-0.2);
//         net.addNetworkModel(w23);
//
//         net.update();
//         // 0.6 with the epsilon(threshold) 0.0001
//         assertEquals(-0.6, output.getActivation(), 0.0001);
//
//     }
//
//     @Test
//     public void testClipping() {
//         LinearRule lr = new LinearRule();
//         lr.setUpperBound(10);
//         lr.setLowerBound(-10);
//
//         // Test clipping upper bound
//         assertEquals(10, lr.clip(100), 0);
//         // Test no clipping
//         assertEquals(5, lr.clip(5), 0);
//         // Test clipping lower bound
//         assertEquals(-10, lr.clip(-20), 0);
//     }
//
//     @Test
//     public void testDerivative() {
//
//         LinearRule lr = new LinearRule();
//         lr.setUpperBound(10);
//         lr.setLowerBound(-10);
//         lr.setSlope(5.0);
//
//         // Above upper bound should return 0
//         assertEquals(0, lr.getDerivative(11), 0);
//
//         // Below lower bound should return 0
//         assertEquals(0, lr.getDerivative(-11), 0);
//
//         // Between lower and upper bound returns the slope
//         assertEquals(5.0, lr.getDerivative(0), 0);
//     }
//
//
// }