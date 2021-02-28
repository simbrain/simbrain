// package org.simbrain.network.subnetworks;
//
// import org.junit.Test;
// import org.simbrain.network.core.Network;
// import org.simbrain.network.neuron_update_rules.UpdateRuleEnum;
//
// import java.awt.geom.Point2D;
//
// import static org.junit.Assert.*;
//
// public class FeedForwardTest {
//
//     @Test
//     public void testUpdate() {
//         Network net = new Network();
//         FeedForward ff = new FeedForward(net, new int[]{2,2,2}, new Point2D.Double(0,0));
//         ff.getNeuronGroupList().get(0).forceSetActivations(new double[]{1,-1});
//         ff.getNeuronGroupList().get(1).setGroupUpdateRule(UpdateRuleEnum.LINEAR);
//         ff.getNeuronGroupList().get(2).setGroupUpdateRule(UpdateRuleEnum.LINEAR);
//         ff.getWeightMatrixList().get(0).diagonalize();
//         ff.getWeightMatrixList().get(1).diagonalize();
//         ff.update();
//         assertArrayEquals(new double[]{1,-1}, ff.getOutputLayer().getActivations(), .01);
//         //System.out.println(ff);
//     }
// }