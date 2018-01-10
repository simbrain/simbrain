package org.simbrain.network.trainers;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.subnetworks.BackpropNetwork;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.jblas.DoubleMatrix;
import org.junit.Test;

public class BackpropTrainer2Test {

  @Test
  public void testCreation() {
        int noOut = 1;
        int noHid = 5;
        int noInp = 1;
        BackpropNetwork network = new BackpropNetwork(new Network(),
                new int[] { noInp, noHid, noOut });

        double str = 0.1;
        double[][] hidOutStrs = new double[noHid][noOut];
        int row = 0, col = 0;
        for (Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
            row = 0;
            for (Synapse s : n.getFanIn()) {
                s.forceSetStrength(str);
                hidOutStrs[row][col] = str;
                row++;
                str += 0.1;
            }
            col++;
        }

        str = 0.05;
        double[][] inpHidStrs = new double[noInp][noHid];
        row = 0;
        col = 0;
        for (Neuron n : network.getHiddenLayer().getNeuronListUnsafe()) {
            row = 0;
            for (Synapse s : n.getFanIn()) {
                s.forceSetStrength(str);
                inpHidStrs[row][col] = str;
                row++;
                str += 0.1;
            }
            col++;
        }
        
        double biases = 0;
        for (Neuron n : network.getOutputLayer().getNeuronListUnsafe()) {
            ((SigmoidalRule) n.getUpdateRule()).setBias(0.314);
            biases += 0.314;
        }

        biases = 0.11;
        for (Neuron n : network.getHiddenLayer().getNeuronList()) {
            ((SigmoidalRule) n.getUpdateRule()).setBias(biases);
            biases += 0.1;
        }

        BackpropTrainer2 trainer = new BackpropTrainer2(network);
        
//        trainer.printDebugInfo();
//         System.out.println(Arrays.deepToString(inpHidStrs));
//         System.out.println();
//         System.out.println(Arrays.deepToString(hidOutStrs));

        double[][] inHidJBlas = trainer.getWeightMatrices().get(0)
                .toArray2();
        System.out.println(Arrays.deepToString(inpHidStrs));
        System.out.println();
        System.out.println(Arrays.deepToString(inHidJBlas));
        System.out.println("-----");
        double[][] hidOutJBlas = trainer.getWeightMatrices().get(1)
                .toArray2();
        System.out.println(Arrays.deepToString(hidOutStrs));
        System.out.println();
        System.out.println(Arrays.deepToString(hidOutJBlas));

        //        
          for (int ii = 0; ii < noHid; ++ii) {
              assertArrayEquals(hidOutJBlas[ii], hidOutStrs[ii], 0);
          }
      }
        
}
