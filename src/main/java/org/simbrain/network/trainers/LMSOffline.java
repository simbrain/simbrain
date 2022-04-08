// /*
//  * Part of Simbrain--a java-based neural network kit
//  * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
//  *
//  * This program is free software; you can redistribute it and/or modify
//  * it under the terms of the GNU General Public License as published by
//  * the Free Software Foundation; either version 2 of the License, or
//  * (at your option) any later version.
//  *
//  * This program is distributed in the hope that it will be useful,
//  * but WITHOUT ANY WARRANTY; without even the implied warranty of
//  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  * GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License
//  * along with this program; if not, write to the Free Software
//  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//  */
// package org.simbrain.network.trainers;
//
// //import java.io.FileWriter;
// //import java.io.IOException;
// //import java.io.PrintWriter;
// //import java.util.ArrayList;
// //import java.util.Collections;
//
// import Jama.Matrix;
// import kotlin.Pair;
// import org.ojalgo.access.Access2D.Builder;
// import org.ojalgo.access.*;
//
// import org.ojalgo.matrix.BasicMatrix;
// import org.ojalgo.matrix.BasicMatrix.Factory;
// import org.ojalgo.matrix.PrimitiveMatrix;
// import org.simbrain.network.core.Neuron;
// import org.simbrain.network.groups.Subnetwork;
// import org.simbrain.network.groups.SynapseGroup;
// import org.simbrain.network.neuron_update_rules.SigmoidalRule;
// import org.simbrain.network.util.SimnetUtils;
// import org.simbrain.util.math.Matrices;
// import org.simbrain.util.stats.distributions.UniformDistribution;
// import org.simbrain.util.stats.ProbabilityDistribution;
//
// import javax.swing.*;
//
// /**
//  * Offline/Batch Learning with least mean squares.
//  *
//  * @author ztosi
//  * @author jyoshimi
//  */
// public class LMSOffline extends Trainer {
//
//     /**
//      * Current solution type.
//      */
//     private SolutionType solutionType = SolutionType.WIENER_HOPF;
//
//     /**
//      * Whether or not ridge regression is to be performed.
//      */
//     private boolean ridgeRegression;
//
//     /**
//      * The magnitude of the ridge regression.
//      */
//     private double alpha;
//
//     /**
//      * Whether or not to add noise to the input state matrix.
//      */
//     private boolean noiseAdded;
//
//     /**
//      * The noise generator from which random values are taken if randomizing
//      * the input state matrix.
//      */
//     private ProbabilityDistribution noiseGen = new UniformDistribution();
//
//     /**
//      * Construct the LMSOOffline object, with a trainable network the Synapse
//      * group where the new synapses will be placed.
//      *
//      * @param network the network to train
//      */
//     public LMSOffline(Trainable network) {
//         super(network);
//     }
//
//     /**
//      * Solution methods for offline LMS.
//      */
//     public enum SolutionType {
//         /**
//          * Wiener-Hopf solution.
//          */
//         WIENER_HOPF {
//             @Override
//             public String toString() {
//                 return "Wiener-Hopf";
//             }
//         },
//
//         /**
//          * Moore-Penrose Solution.
//          */
//         MOORE_PENROSE {
//             @Override
//             public String toString() {
//                 return "Moore-Penrose";
//             }
//         }
//
//     }
//
//     ;
//
//     @Override
//     public void apply() throws DataNotInitializedException {
//
//         if (getTrainableNetwork().getTrainingSet().getInputData() == null) {
//             throw new DataNotInitializedException("Input data not initalized");
//         }
//         if (getTrainableNetwork().getTrainingSet().getTargetData() == null) {
//             throw new DataNotInitializedException("Target data not initalized");
//         }
//
//         getEvents().fireBeginTraining();
//
//         int index = 0;
//         for (Neuron n : network.getOutputNeurons()) {
//
//             // If output nodes are sigmoidal, alter the effective target data
//             // such that the desired value will result when it is put through
//             // the sigmoidal. Warning: problems can occur here if the bounds of
//             // the sigmoidal are not set properly.
//             if (n.getUpdateRule() instanceof SigmoidalRule) {
//                 for (int i = 0; i < network.getTrainingSet().getTargetData().length; i++) {
//                     network.getTrainingSet().getTargetData()[i][index] = ((SigmoidalRule) n.getUpdateRule()).getInverse(network.getTrainingSet().getTargetData()[i][index]);
//                 }
//             }
//             index++;
//         }
//
//         // Add noise to the input state matrix.
//         if (noiseAdded) {
//             double[][] stateMat = network.getTrainingSet().getInputData();
//             for (int i = 0, n = stateMat.length; i < n; i++) {
//                 for (int j = 0, m = stateMat[i].length; j < m; j++) {
//                     network.getTrainingSet().getInputData()[i][j] = stateMat[i][j] + noiseGen.nextRand();
//                 }
//             }
//         }
//
//         if (solutionType == SolutionType.WIENER_HOPF) {
//             weinerHopfSolution(network);
//         } else if (solutionType == SolutionType.MOORE_PENROSE) {
//             moorePenroseSolution(network);
//         } else {
//             throw new IllegalArgumentException("Solution type must be " + "'MoorePenrose' or 'WeinerHopf'.");
//         }
//
//         // Make sure excitatory/inhibitory are in proper lists
//         if (getTrainableNetwork().getNetwork() instanceof Subnetwork) {
//             SynapseGroup group = ((Subnetwork) getTrainableNetwork().getNetwork()).getSynapseGroup();
//             if (group != null) {
//                 group.revalidateSynapseSets();
//             }
//         } else if (getTrainableNetwork().getNetwork() instanceof SynapseGroup) {
//             if (getTrainableNetwork().getNetwork() != null) {
//                 ((SynapseGroup) getTrainableNetwork().getNetwork()).revalidateSynapseSets();
//             }
//         }
//
//         getEvents().fireEndTraining();
//         revalidateSynapseGroups();
//
//     }
//
//     /**
//      * Implements the Wiener-Hopf solution to LMS linear regression.
//      * TODO: Fix progress updates to reflect actual training times &#38; %s
//      *
//      * @param network the trainable network being trained
//      */
//     public void weinerHopfSolution(Trainable network) {
//         long start = System.nanoTime();
//         double[][] inputMatrix = network.getTrainingSet().getInputData();
//         double[][] trainingMatrix = network.getTrainingSet().getTargetData();
//         try {
//
//             Factory<?> mf = PrimitiveMatrix.FACTORY;
//
//             Builder<?> tmpBuilder = mf.getBuilder(inputMatrix.length, inputMatrix[0].length);
//             for (int i = 0; i < tmpBuilder.countRows(); i++) {
//                 for (int j = 0; j < tmpBuilder.countColumns(); j++) {
//                     tmpBuilder.set(i, j, inputMatrix[i][j]);
//                 }
//             }
//
//             BasicMatrix stateMat = (BasicMatrix) tmpBuilder.build();
//
//             tmpBuilder = mf.getBuilder(trainingMatrix.length, trainingMatrix[0].length);
//             for (int i = 0; i < tmpBuilder.countRows(); i++) {
//                 for (int j = 0; j < tmpBuilder.countColumns(); j++) {
//                     tmpBuilder.set(i, j, trainingMatrix[i][j]);
//                     if (Double.isInfinite(trainingMatrix[i][j]) || Double.isNaN(trainingMatrix[i][j])) {
//                         throw new NumberFormatException("Invalid target" + " values.");
//                     }
//                 }
//             }
//
//             BasicMatrix teachMat = (BasicMatrix) tmpBuilder.build();
//
//             getEvents().fireProgressUpdated("Correlating State Matrix (R = S'S)...", 0);
//             teachMat = stateMat.transpose().multiplyRight(teachMat);
//
//
//             getEvents().fireProgressUpdated("Cross-Correlating States with Teacher data (P = S'D)...", 15);
//             stateMat = stateMat.transpose().multiplyRight(stateMat);
//
//             getEvents().fireProgressUpdated("Computing Inverse Correlation Matrix...", 30);
//
//             if (ridgeRegression) {
//                 tmpBuilder = mf.getBuilder((int) stateMat.countRows(), (int) stateMat.countColumns());
//                 for (int i = 0, n = (int) stateMat.countColumns(); i < n; i++) {
//                     tmpBuilder.set(i, i, alpha * alpha);
//                 }
//                 BasicMatrix scaleMat = (BasicMatrix) tmpBuilder.build();
//                 stateMat = stateMat.add(scaleMat);
//             }
//
//             stateMat = stateMat.invert();
//
//             getEvents().fireProgressUpdated("Computing Weights", 80);
//             tmpBuilder = stateMat.multiplyRight(teachMat).copyToBuilder();
//             BasicMatrix finalMat = (BasicMatrix) tmpBuilder.build();
//             double[][] wOut = new double[(int) tmpBuilder.countRows()]
//                     [(int) tmpBuilder.countColumns()];
//             for (int i = 0, n = (int) tmpBuilder.countRows(); i < n; i++) {
//                 for (int j = 0, m = (int) tmpBuilder.countColumns(); j < m; j++) {
//                     wOut[i][j] = finalMat.doubleValue(i, j);
//                 }
//             }
//             getEvents().fireProgressUpdated("Set weights...", 95);
//             SimnetUtils.setWeights(network.getInputNeurons(), network.getOutputNeurons(), wOut);
//             getEvents().fireProgressUpdated("Done!", 100);
//
//             // TODO: What error does JAMA actually throw for singular Matrices?
//         } catch (RuntimeException e) {
//             JOptionPane.showMessageDialog(new JFrame(), "" + "State Correlation Matrix is Singular." + "\nCheck that target values are in range of output units." + "\nOtherwise, input matrix is rank-deficient.", "Training Failed", JOptionPane.ERROR_MESSAGE);
//             getEvents().fireProgressUpdated("Training Failed", 0);
//         }
//
//         trainingMatrix = null;
//         inputMatrix = null;
//
//         long end = System.nanoTime();
//         System.out.println("Time: " + (end - start) / Math.pow(10, 9));
//     }
//
//     /**
//      * Moore penrose.
//      *
//      * @param network the trainable network being trained
//      */
//     public void moorePenroseSolution(Trainable network) {
//         Matrix inputMatrix = new Matrix(network.getTrainingSet().getInputData());
//         Matrix trainingMatrix = new Matrix(network.getTrainingSet().getTargetData());
//
//         getEvents().fireProgressUpdated("Computing Moore-Penrose Pseudoinverse...", 0);
//         // Computes Moore-Penrose Pseudoinverse
//         inputMatrix = Matrices.pinv(inputMatrix);
//
//         getEvents().fireProgressUpdated("Computing Weights...", 50);
//         double[][] wOut = inputMatrix.times(trainingMatrix).getArray();
//
//         getEvents().fireProgressUpdated("Setting Weights...", 75);
//         SimnetUtils.setWeights(network.getInputNeurons(), network.getOutputNeurons(), wOut);
//         getEvents().fireProgressUpdated("Done!", 100);
//
//         inputMatrix = null;
//         trainingMatrix = null;
//     }
//
//     /**
//      * Set solution type.
//      *
//      * @param solutionType the solutionType to set
//      */
//     public void setSolutionType(SolutionType solutionType) {
//         this.solutionType = solutionType;
//     }
//
//     public boolean isRidgeRegression() {
//         return ridgeRegression;
//     }
//
//     public void setRidgeRegression(boolean ridgeRegression) {
//         this.ridgeRegression = ridgeRegression;
//     }
//
//     public double getAlpha() {
//         return alpha;
//     }
//
//     public void setAlpha(double alpha) {
//         this.alpha = alpha;
//     }
//
//     public boolean isNoiseAdded() {
//         return noiseAdded;
//     }
//
//     public void setNoiseAdded(boolean noiseAdded) {
//         this.noiseAdded = noiseAdded;
//     }
//
//     public ProbabilityDistribution getNoiseGen() {
//         return noiseGen;
//     }
//     //
//     //    /**
//     //     *
//     //     * @param args args
//     //     */
//     //    public static void main(String[] args) {
//     //        try {
//     //            FileWriter fw = new FileWriter("SinContIn.csv");
//     //            PrintWriter pw = new PrintWriter(fw);
//     //
//     //            int samples = 20;
//     //            int tPerSample = 5000;
//     //
//     //            ArrayList<Double> frequencies = new ArrayList<Double>();
//     //            double [] sineWave = new double [samples * tPerSample];
//     //            double delta_t = 0.1; //ms
//     //
//     //            for (int i = 0; i < 20; i++) {
//     //                frequencies.add(1.0 + (double) i / 10.0);
//     //            }
//     //            Collections.shuffle(frequencies);
//     //            for (int i = 0; i < samples; i++) {
//     //                for (int j = 0; j < tPerSample; j++) {
//     //
//     //                    sineWave[(i * tPerSample) + j] = Math.sin((j/10.0)
//     //                            * frequencies.get(i));
//     //                    System.out.println((j/100.0) * frequencies.get(i));
//     //
//     //                }
//     //            }
//     //
//     //            for (int i = 0; i < tPerSample * samples; i++) {
//     //                pw.println(frequencies.get((int) Math.floor(i/tPerSample))
//     //                        + "");
//     //            }
//     //
//     //            fw.close();
//     //            pw.close();
//     //            FileWriter fw2 = new FileWriter("SinContTeach.csv");
//     //            PrintWriter pw2 = new PrintWriter(fw2);
//     //
//     //            for (int i = 0; i < tPerSample * samples; i++) {
//     //                pw2.println(sineWave[i] + "");
//     //            }
//     //
//     //            pw2.close();
//     //            fw2.close();
//     //
//     //        } catch (IOException e) {
//     //            e.printStackTrace();
//     //        }
//     //    }
//     //
// }
