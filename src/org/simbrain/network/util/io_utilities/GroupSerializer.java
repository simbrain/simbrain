/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.util.io_utilities;

import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.math.ProbDistributions.LogNormalDistribution;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for storing static methods for custom serialization of group objects
 * in Simbrain.
 *
 * @author Zoë Tosi
 */
public class GroupSerializer {

    /**
     * A flag for how decimal values should be serialized and methods for
     * converting doubles or doubles as long bits appropriately.
     *
     * @author Zoë Tosi
     */
    public enum Precision {
        FLOAT_32 {
            @Override
            public byte[] asByteArray(double d) {
                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(Float.floatToIntBits((float) d));
                return b.array();
            }

            @Override
            public byte[] asByteArray(long l) {
                double d = Double.longBitsToDouble(l);
                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(Float.floatToIntBits((float) d));
                return b.array();
            }

        }, FLOAT_64 {
            @Override
            public byte[] asByteArray(double d) {
                ByteBuffer b = ByteBuffer.allocate(8);
                b.putLong(Double.doubleToLongBits(d));
                return b.array();
            }

            @Override
            public byte[] asByteArray(long l) {
                ByteBuffer b = ByteBuffer.allocate(8);
                b.putLong(l);
                return b.array();
            }
        };

        public abstract byte[] asByteArray(double d);

        public abstract byte[] asByteArray(long l);
    }

    /**
     * Saves a synapse group with a given precision representing its weights to
     * a file with the given filename.
     *
     * @param synGrp      the synapse group to serialize
     * @param wtPrecision the precision (32 or 64 bit) used to store the weight values
     * @param filename    the name of the file containing the synapse group information.
     */
    public static void serializeCompressedSynGroup(SynapseGroup synGrp, Precision wtPrecision, String filename) {
            byte[] byteCompressedRowInds = synGrp.getSparseCode(wtPrecision).array();
//        long[] rowCompression = synGrp.getRowCompressedMatrixRepresentation();
//        byte[] byteCompressedRowInds = rowCompMat2CompByteArray(rowCompression, wtPrecision);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(filename))) {
            out.write(byteCompressedRowInds);
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }



    /**
     * Converts a complete row compressed represenation of a matrix into a byte
     * array. Further compresses the column index values values by using the
     * smallest number of discrete bytes to store each index value. Ex: if an
     * index has a value less than 255 it is stored in a single byte, otherwise
     * it is stored as a short, and so on if the index is greater than 65535.
     *
     * @param riCompressedMat row compressed matrix where column values are separated by a
     *                        new row code (-1 or 0xffffffff) representing positions in a
     *                        sparse matrix
     * @param precision
     * @return the compressed byte array representation of the row compressed
     * matrix riCompressedMat.
     */
    public static byte[] rowCompMat2CompByteArray(long[] riCompressedMat, Precision precision) {
        final byte maxByte = -1;
        List<Byte> preByteArray = new ArrayList<Byte>();
        boolean switchedToShorts = false;
        boolean switchedToInts = false;
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.putInt((int) riCompressedMat[0]);
        for (byte b : buff.array()) {
            preByteArray.add(b);
        }

        int numNonZero = 0;
        for (int i = 1, n = riCompressedMat.length; i < n; i++) {
            if (numNonZero < riCompressedMat[0] || riCompressedMat[i] == -1) {
                if (riCompressedMat[i] == -1) { // Blank row
                    // New Row code
                    for (int k = 0; k < 4; k++) {
                        preByteArray.add(maxByte);
                    }
                    switchedToShorts = false;
                    switchedToInts = false;
                    continue;
                }
                if (i > 1) {
                    if (riCompressedMat[i - 1] >= riCompressedMat[i]) {
                        // New row code
                        for (int k = 0; k < 4; k++) {
                            preByteArray.add(maxByte);
                        }
                        switchedToShorts = false;
                        switchedToInts = false;
                    }
                }

                if (!switchedToShorts && !switchedToInts) {
                    if (riCompressedMat[i] > Byte.MAX_VALUE * 2) {
                        switchedToShorts = true;
                        preByteArray.add(maxByte);
                        if (riCompressedMat[i] > Short.MAX_VALUE * 2) {
                            switchedToInts = true;
                            preByteArray.add(maxByte);
                            preByteArray.add(maxByte);
                        }
                    } else {
                        preByteArray.add((byte) (((int) riCompressedMat[i]) << 24 >>> 24));
                        numNonZero++;
                    }
                }

                if (switchedToShorts && !switchedToInts) {
                    if (riCompressedMat[i] > Short.MAX_VALUE * 2) {
                        switchedToInts = true;
                        // No longer using shorts end code
                        preByteArray.add(maxByte);
                        preByteArray.add(maxByte);
                    } else {
                        ByteBuffer b = ByteBuffer.allocate(2);
                        b.putShort((short) (((int) riCompressedMat[i]) << 16 >>> 16));
                        numNonZero++;
                        for (byte sec : b.array()) {
                            preByteArray.add(sec);
                        }
                    }
                }

                if (switchedToInts) {
                    ByteBuffer b = ByteBuffer.allocate(4);
                    b.putInt((int) riCompressedMat[i]);
                    numNonZero++;
                    for (byte sec : b.array()) {
                        preByteArray.add(sec);
                    }
                }
            } else {
                byte[] bytes = precision.asByteArray(riCompressedMat[i]);
                for (byte b : bytes) {
                    preByteArray.add(b);
                }
            }
        }
        byte[] byteArr = new byte[preByteArray.size()];
        int i = 0;
        for (Byte b : preByteArray) {
            byteArr[i++] = b.byteValue();
        }
        return byteArr;
    }

    /**
     * Test Main
     *
     * @param args
     */
    public static void main(String[] args) {

        Network net = new Network();
        NeuronGroup ng1 = new NeuronGroup(net, 2048);

        ProbabilityDistribution exRand =
                LogNormalDistribution.builder()
                        .polarity(Polarity.EXCITATORY)
                        .location(2.5)
                        .build();


        ProbabilityDistribution inRand =
                LogNormalDistribution.builder()
                        .polarity(Polarity.INHIBITORY)
                        .location(3.5)
                        .build();

        Sparse rCon = new Sparse(0.11, false, false);
        SynapseGroup sg = SynapseGroup.createSynapseGroup(ng1, ng1, rCon, 0.5, exRand, inRand);
        System.out.println("Initial Construction complete...\n");
        long start = System.nanoTime();
        System.out.println("Begin Serialization... ");
        try {
            serializeCompressedSynGroup(sg, Precision.FLOAT_32, "Test1.bin");
        } catch (Exception e) {
            System.out.println("FAILURE.\n");
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("SUCCESS!");
        long end = System.nanoTime();
        System.out.println("\tSerialization Time: " + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 4) + " secs.\n");
        System.out.println("Begin Deserialization... ");
        NeuronGroup ng2 = new NeuronGroup(net, 2048);
        SynapseGroup sg2 = new SynapseGroup(ng2, ng2);
        long start2 = System.nanoTime();
        boolean status = GroupDeserializer.reconstructCompressedSynapseStrengths("./Test1.bin", sg2);
        end = System.nanoTime();
        if (status) {
            System.out.println("SUCCESS!");
        } else {
            System.out.println("FAILURE");
            System.exit(1);
        }
        System.out.println("\tReconstruction Time: " + SimbrainMath.roundDouble((end - start2) / Math.pow(10, 9), 4) + " secs.\n");
        System.out.println("TOTAL Time: " + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 4) + " secs.");
        System.out.println("\nValidating Reconstruction...");
        System.out.println("\tOriginal No. Synapses: " + sg.size());
        System.out.println("\tReconstructed No. Synapses: " + sg2.size());
        // int orZeros = 0;
        // int recZeros = 0;
        // int weightChecks = 0;
        List<Neuron> ng1List = ng1.getNeuronList();
        List<Neuron> ng2List = ng2.getNeuronList();
        boolean error = false;
        double errorTolerance = 0.00001;
        for (int i = 0, n = ng1.size(); i < n; i++) {
            Neuron n1src = ng1List.get(i);
            Neuron n2src = ng2List.get(i);
            // if (n1src.getFanOut().size() == 0) {
            // orZeros++;
            // }
            // if (n2src.getFanOut().size() == 0) {
            // recZeros++;
            // }
            for (int j = 0, m = ng2.size(); j < m; j++) {
                Neuron n1tar = ng1List.get(j);
                Neuron n2tar = ng2List.get(j);
                Synapse s1 = n1src.getFanOut().get(n1tar);
                Synapse s2 = n2src.getFanOut().get(n2tar);
                if (s1 == null && s2 != null) {
                    System.out.println("\t\tPositionError");
                    error = true;
                    i = n;
                    break;
                }
                if (s2 == null && s1 != null) {
                    System.out.println("\t\tPositionError");
                    error = true;
                    i = n;
                    break;
                }
                if (s1 != null && s2 != null) {
                    // weightChecks++;
                    if (Math.abs(s1.getStrength() - s2.getStrength()) > errorTolerance) {
                        System.out.println("\t\tWeight mismatch");
                        System.out.println(s1.getStrength() + " " + s2.getStrength());
                        error = true;
                        i = n;
                        break;
                    }
                }
            }
        }
        if (!error) {
            System.out.println("\tNo strucutral/positional errors.");
            System.out.println("\tNo weight mismatches greater than " + errorTolerance);
            System.out.println("SUCCESS!");
        } else {
            System.out.println("FAILURE");
        }

        // System.out.println(orZeros);
        // System.out.println(recZeros);
        // System.out.println(weightChecks);
    }

}
