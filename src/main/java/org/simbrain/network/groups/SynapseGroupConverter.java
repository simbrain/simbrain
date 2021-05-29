package org.simbrain.network.groups;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.trainers.InvalidDataException;
import org.simbrain.network.util.SynapseSet;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Converts the synapses sets in a synapse group to a compressed format if possible.
 */
public class SynapseGroupConverter implements Converter {

    /**
     * A flag for how decimal values should be serialized and methods for
     * converting doubles or doubles as long bits appropriately.
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

    @Override
    public boolean canConvert(Class type) {
        return type == SynapseSet.class;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        SynapseSet ss = (SynapseSet) source;

        writer.startNode("parent");
        context.convertAnother(ss.getParent());
        writer.endNode();

        writer.startNode("useCompressedRep");
        context.convertAnother(ss.getParent().isUseGroupLevelSettings());
        writer.endNode();

        writer.startNode("compressedSynapseRep");
        if (ss.getParent().isUseGroupLevelSettings()) {
            context.convertAnother(toByteArray(ss, Precision.FLOAT_64));
        } else {
            context.convertAnother(getFullByteRep(ss));
        }
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

        reader.moveDown();
        SynapseGroup parent = (SynapseGroup) context.convertAnother(null, SynapseGroup.class);
        reader.moveUp();

        reader.moveDown();
        boolean useCompressedRep = (Boolean) context.convertAnother(null, Boolean.class);
        reader.moveUp();

        reader.moveDown();
        HashSet<Synapse> synapses = new HashSet<>();
        if (useCompressedRep) {
            byte[] compressedMatrixRep = (byte[]) context.convertAnother(null, byte[].class);
            synapses = synapseFromCompressed(compressedMatrixRep, parent);
            // TODO: Not needed since xstream handles it?
            // parent.setAndConformToTemplate(parent.getExcitatoryPrototype(), SimbrainConstants.Polarity.EXCITATORY);
            // parent.setAndConformToTemplate(parent.getInhibitoryPrototype(), SimbrainConstants.Polarity.INHIBITORY);
        } else {
            byte[] fullSynapseRep = (byte[]) context.convertAnother(null, byte[].class);

            Map<Integer, Neuron> srcMap = new HashMap<>((int) (parent.getSourceNeuronGroup().size() / 0.75));
            Map<Integer, Neuron> tarMap = new HashMap<>((int) (parent.getTargetNeuronGroup().size() / 0.75));
            int i = 0;
            for (Neuron n : parent.getSourceNeuronGroup().getNeuronList()) {
                srcMap.put(i++, n);
            }
            i = 0;
            for (Neuron n : parent.getTargetNeuronGroup().getNeuronList()) {
                tarMap.put(i++, n);
            }
            ByteBuffer bigBuff = ByteBuffer.wrap(fullSynapseRep);
            while (bigBuff.hasRemaining()) {
                int delay = bigBuff.getInt();
                int codeBuffSize = 20 + (delay * 8) + 4 + 1;
                ByteBuffer codeBuff = ByteBuffer.allocate(codeBuffSize);
                codeBuff.putInt(delay);
                byte[] data = new byte[codeBuffSize - 4];
                bigBuff.get(data);
                codeBuff.put(data);
                Neuron src = srcMap.get(bigBuff.getInt());
                Neuron tar = tarMap.get(bigBuff.getInt());
                Synapse s = new Synapse(src, tar);
                s.decodeNumericByteArray(ByteBuffer.wrap(codeBuff.array()));
                synapses.add(s);
            }
            // TODO: Not needed since xstream handles it?
            // parent.setIncrement(parent.getExcitatoryPrototype().getIncrement(), SimbrainConstants.Polarity.EXCITATORY);
            // parent.setLearningRule(parent.getExcitatoryPrototype().getLearningRule(),
            //         SimbrainConstants.Polarity.EXCITATORY);
            // parent.setSpikeResponder(parent.getExcitatoryPrototype().getSpikeResponder(),
            //         SimbrainConstants.Polarity.EXCITATORY);
            // parent.setLowerBound(parent.getExcitatoryPrototype().getLowerBound(),
            //         SimbrainConstants.Polarity.EXCITATORY);
            // parent.setUpperBound(parent.getExcitatoryPrototype().getUpperBound(),
            //         SimbrainConstants.Polarity.EXCITATORY);
            // parent.setIncrement(parent.getInhibitoryPrototype().getIncrement(), SimbrainConstants.Polarity.INHIBITORY);
            // parent.setLearningRule(parent.getInhibitoryPrototype().getLearningRule(),
            //         SimbrainConstants.Polarity.INHIBITORY);
            // parent.setSpikeResponder(parent.getInhibitoryPrototype().getSpikeResponder(),
            //         SimbrainConstants.Polarity.INHIBITORY);
            // parent.setLowerBound(parent.getInhibitoryPrototype().getLowerBound(),
            //         SimbrainConstants.Polarity.INHIBITORY);
            // parent.setUpperBound(parent.getInhibitoryPrototype().getUpperBound(),
            //         SimbrainConstants.Polarity.INHIBITORY);
        }
        // TODO: Needed?
        // if (connectionManager instanceof Sparse) {
        //     ((Sparse) connectionManager).setPermitDensityEditing(false);
        // }

        reader.moveUp();

        return new SynapseSet(parent, synapses);
    }

    /**
     * Returns a full synapse representation.
     */
    public static byte[] getFullByteRep(SynapseSet ss) {
        Map<Neuron, Integer> srcMap = new HashMap<>((int) (ss.getParent().getSourceNeuronGroup().size() / 0.75));
        Map<Neuron, Integer> tarMap = new HashMap<>((int) (ss.getParent().getTargetNeuronGroup().size() / 0.75));
        int i = 0;
        for (Neuron n : ss.getParent().getSourceNeuronGroup().getNeuronList()) {
            srcMap.put(n, i++);
        }
        i = 0;
        for (Neuron n : ss.getParent().getTargetNeuronGroup().getNeuronList()) {
            tarMap.put(n, i++);
        }
        byte[][] synBytes = new byte[ss.size()][];
        i = 0;
        int totalBytes = 0;
        for (Iterator<Synapse> it = ss.iterator(); it.hasNext(); ) {
            Synapse s = it.next();
            byte[] synCode = s.getNumericValuesAsByteArray();
            ByteBuffer indices = ByteBuffer.allocate(8);
            indices.putInt(srcMap.get(s.getSource()));
            indices.putInt(tarMap.get(s.getTarget()));
            int index = synCode.length - 8;
            for (int j = index, n = synCode.length; j < n; j++) {
                synCode[j] = indices.array()[j - index];
            }
            synBytes[i++] = synCode;
            totalBytes += synCode.length;
        }
        ByteBuffer buff = ByteBuffer.allocate(totalBytes);
        for (byte[] synCodes : synBytes) {
            buff.put(synCodes);
        }
        return buff.array();
    }

    /**
     * @return a row compressed representation of the weight matrix derived from
     * this synapse group. All values are stored as longs, and row changes are
     * denoted by -1.
     */
    public static long[] getRowCompressedMatrix(SynapseSet ss) {
        double[][] pairs = getNumericIndices(ss);
        int numSyns = ss.size();
        int numSrc = ss.getParent().getSourceNeuronGroup().size();
        long[] compRowRep = new long[numSrc + (2 * numSyns)];
        int currRow = 0;
        int m = 0;
        compRowRep[0] = numSyns;
        int l = 1;
        for (int n = numSyns + numSrc; l < n; l++) {
            if (m == numSyns) {
                break;
            }
            if (pairs[m][0] != currRow) {
                compRowRep[l] = -1L;
                currRow++;
            } else {
                compRowRep[l] = (long) pairs[m][1];
                compRowRep[numSyns + numSrc + m] = Double.doubleToLongBits(pairs[m][2]);
                m++;
            }
        }
        // Adds trailing "new row" markers when the last synapse comes from a
        // source neuron other than the last one. This represents empty rows on the end.
        while (currRow < numSrc - 1) {
            compRowRep[l++] = -1L;
            currRow++;
        }
        return compRowRep;
    }

    /**
     * Produces a sparse representation of the synapse group and returns that data as a byte buffer. Ordering is such
     * that meta-data comes first indicating a marker which is currently used for backwards compatibility indicating that
     * the bytes here represent the new serialization scheme. This is followed by the number of synapses. From there
     * each source neuron index is followed by the number of outgoing connections it has and then the indices of the
     * outgoing connections in the target neuron group. All indices and metadata are encoded as integers. The remaining
     * bytes are bit representations of the FP weight values either in single or double precision.
     *
     * @param precision what precision to story the weights.
     */
    public static ByteBuffer getSparseCode(Precision precision, SynapseSet ss) {
        double[][] pairs = getNumericIndices(ss);
        // Can't use src group size because some neurons in the group may not have any synaptic connections to the target
        int numSrc = 0;
        int curSrc = -1;
        // keeps track of indices of the beginnings and ends of target neuron to each source
        List<Integer> localOutInds = new ArrayList<>();
        for (int ii = 0; ii < pairs.length; ++ii) {
            if (curSrc != pairs[ii][0]) {
                localOutInds.add(ii);
                curSrc = (int) pairs[ii][0];
                numSrc++;
            }
        }
        localOutInds.add(pairs.length);
        ByteBuffer buffer;
        if (precision == Precision.FLOAT_64) {
            buffer = ByteBuffer.allocate(4 * (2 * numSrc + 3 * pairs.length) + 4 + 1 + 4 + 4);
        } else {
            buffer = ByteBuffer.allocate(4 * (2 * numSrc + 2 * pairs.length) + 4 + 1 + 4 + 4);
        }

        buffer.putInt(-1); // Marker so what we know that the new serialization method is being used.
        // Meta encoding whether or not double precision is being used
        buffer.put((byte) (precision == Precision.FLOAT_64 ? 0x1 : 0x0));
        // Meta number of synapses
        buffer.putInt(pairs.length);
        // Meta number of EFFECTIVE source neurons--ones with at least one outgoing connection in this group
        buffer.putInt(numSrc);
        for (int ii = 0; ii < numSrc; ++ii) {
            int start = localOutInds.get(ii);
            int end = localOutInds.get(ii + 1);
            buffer.putInt((int) pairs[start][0]); // SourceIndex
            buffer.putInt(end - start); // number of targets
            for (int jj = start; jj < end; ++jj) {
                buffer.putInt((int) pairs[jj][1]);
            }
        }

        if (precision == Precision.FLOAT_64) {
            for (double[] pair : pairs) {
                buffer.putLong(Double.doubleToLongBits(pair[2]));
            }
        } else {
            for (double[] pair : pairs) {
                buffer.putInt(Float.floatToIntBits((float) pair[2]));
            }
        }
        return buffer;
    }

    /**
     * For large, sparse synapse groups this will cause a heap overflow. Use
     * getRowCompressedMatrixRepresentation instead.
     *
     * @return a representation of the synapse strengths in this synapse group
     * as a weight matrix between two activation vectors (neuron groups).
     */
    public static double[][] getWeightMatrix(SynapseGroup sg) {
        double[][] weightMatrix = new double[sg.getSourceNeurons().size()][sg.getTargetNeurons().size()];
        int i = 0;
        int j = 0;
        // Create numbers for neurons... less expensive than constant
        // indexOf calls to array lists.
        Map<Neuron, Integer> sourceMap = new HashMap<Neuron, Integer>();
        Map<Neuron, Integer> targetMap = new HashMap<Neuron, Integer>();
        for (Neuron n : sg.getSourceNeurons()) {
            sourceMap.put(n, i++);
        }
        for (Neuron n : sg.getTargetNeurons()) {
            targetMap.put(n, j++);
        }
        // Construct uncompressed matrix from weights
        for (Synapse s : sg.getExcitatorySynapses()) {
            i = sourceMap.get(s.getSource());
            j = targetMap.get(s.getTarget());
            weightMatrix[i][j] = s.getStrength();
        }
        for (Synapse s : sg.getInhibitorySynapses()) {
            i = sourceMap.get(s.getSource());
            j = targetMap.get(s.getTarget());
            weightMatrix[i][j] = s.getStrength();
        }
        return weightMatrix;
    }

    /**
     * A more compressed version of a weight matrix for cases where a weight
     * matrix is needed, but may cause memory issues if fully instantiated. Eg,
     * for very sparse synapse groups between very large neuron groups.
     *
     * @return a 2D array with a number of rows equal to the total number of
     * synapses and a number of columns equal to 3. Each row contains the the
     * source index number, the target index number, and the strength in that
     * order. This array is then sorted by source index then target index. Ex: 1
     * 2 .9 0 3 5.3 0 1 -.1 Becomes: 0 1 -.1 0 3 5.3 1 2 .9
     */
    public static double[][] getNumericIndices(SynapseSet ss) {
        double[][] pairs = new double[ss.size()][3];
        int i = 0;
        int j = 0;
        // Create numbers for neurons... less expensive than constant
        // indexOf calls to array lists.
        Map<Neuron, Integer> sourceMap = new HashMap<>((int) (ss.getParent().getSourceNeuronGroup().size() / 0.75));
        Map<Neuron, Integer> targetMap = new HashMap<>((int) (ss.getParent().getTargetNeuronGroup().size() / 0.75));
        // Assign indices to each source and target neuron in a lookup table
        // so that synapses can be identified by a source and target index
        for (Neuron n : ss.getParent().getSourceNeurons()) {
            sourceMap.put(n, i++);
        }
        for (Neuron n : ss.getParent().getTargetNeurons()) {
            targetMap.put(n, j++);
        }
        // Put each synapse strength into a table [i, j, w], where i is the
        // source neuron index in a weight matrix, j is the target index and
        // w is the synapse strength.
        int k = 0;
        for (Iterator<Synapse> it = ss.iterator(); it.hasNext(); ) {
            Synapse s = it.next();
            pairs[k++] = new double[]{sourceMap.get(s.getSource()), targetMap.get(s.getTarget()), s.getStrength()};
        }
        // Create a comparator to sort synapse table entries by source, then
        // by column.
        Comparator<double[]> rowColOrderer = new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                if (o1[0] < o2[0]) {
                    return -1;
                } else if (o1[0] > o2[0]) {
                    return 1;
                } else {
                    if (o1[1] < o2[1]) {
                        return -1;
                    } else if (o1[1] > o2[1]) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
        };
        // And sort the table
        Arrays.sort(pairs, rowColOrderer);
        return pairs;
    }

    /**
     * Converts a complete row compressed representation of a matrix into a byte
     * array. Further compresses the column index values values by using the
     * smallest number of discrete bytes to store each index value. Ex: if an
     * index has a value less than 255 it is stored in a single byte, otherwise
     * it is stored as a short, and so on if the index is greater than 65535.
     *
     * @return the compressed byte array representation of the row compressed
     * matrix riCompressedMat.
     */
    public static byte[] toByteArray(SynapseSet ss, Precision precision) {
        // row compressed matrix where column values are separated by a new row code (-1 or 0xffffffff) representing
        //positions in a sparse matrixow compressed matrix where column values are separated by a new row code
        // (-1 or 0xffffffff) representing positions in a sparse matrix
        long[] rowCompressed = getRowCompressedMatrix(ss);
        final byte maxByte = -1;
        List<Byte> preByteArray = new ArrayList<>();
        boolean switchedToShorts = false;
        boolean switchedToInts = false;
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.putInt((int) rowCompressed[0]);
        for (byte b : buff.array()) {
            preByteArray.add(b);
        }

        int numNonZero = 0;
        for (int i = 1, n = rowCompressed.length; i < n; i++) {
            if (numNonZero < rowCompressed[0] || rowCompressed[i] == -1) {
                if (rowCompressed[i] == -1) { // Blank row
                    // New Row code
                    for (int k = 0; k < 4; k++) {
                        preByteArray.add(maxByte);
                    }
                    switchedToShorts = false;
                    switchedToInts = false;
                    continue;
                }
                if (i > 1) {
                    if (rowCompressed[i - 1] >= rowCompressed[i]) {
                        // New row code
                        for (int k = 0; k < 4; k++) {
                            preByteArray.add(maxByte);
                        }
                        switchedToShorts = false;
                        switchedToInts = false;
                    }
                }

                if (!switchedToShorts && !switchedToInts) {
                    if (rowCompressed[i] > Byte.MAX_VALUE * 2) {
                        switchedToShorts = true;
                        preByteArray.add(maxByte);
                        if (rowCompressed[i] > Short.MAX_VALUE * 2) {
                            switchedToInts = true;
                            preByteArray.add(maxByte);
                            preByteArray.add(maxByte);
                        }
                    } else {
                        preByteArray.add((byte) (((int) rowCompressed[i]) << 24 >>> 24));
                        numNonZero++;
                    }
                }

                if (switchedToShorts && !switchedToInts) {
                    if (rowCompressed[i] > Short.MAX_VALUE * 2) {
                        switchedToInts = true;
                        // No longer using shorts end code
                        preByteArray.add(maxByte);
                        preByteArray.add(maxByte);
                    } else {
                        ByteBuffer b = ByteBuffer.allocate(2);
                        b.putShort((short) (((int) rowCompressed[i]) << 16 >>> 16));
                        numNonZero++;
                        for (byte sec : b.array()) {
                            preByteArray.add(sec);
                        }
                    }
                }

                if (switchedToInts) {
                    ByteBuffer b = ByteBuffer.allocate(4);
                    b.putInt((int) rowCompressed[i]);
                    numNonZero++;
                    for (byte sec : b.array()) {
                        preByteArray.add(sec);
                    }
                }
            } else {
                byte[] bytes = precision.asByteArray(rowCompressed[i]);
                for (byte b : bytes) {
                    preByteArray.add(b);
                }
            }
        }
        byte[] byteArr = new byte[preByteArray.size()];
        int i = 0;
        for (Byte b : preByteArray) {
            byteArr[i++] = b;
        }
        return byteArr;
    }

    private static HashSet<Synapse> synapseFromCompressed(byte[] rowCompByteArr, SynapseGroup newSg) {
        HashSet<Synapse> ret = new HashSet<>();
        try {
            // Read in all the data from the file stored in discrete bytes
            ByteBuffer inStream = ByteBuffer.wrap(rowCompByteArr);

            inStream.getInt(); // skip flag
            Precision precision = inStream.get() == (byte) 0x1 ? Precision.FLOAT_64
                    : Precision.FLOAT_32;

            int size = inStream.getInt();
            int numSrc = inStream.getInt(); // TODO: This number is invalid

            List<Neuron> src = newSg.getSourceNeurons();
            List<Neuron> tar = newSg.getTargetNeurons();
            // Store synapses before putting them in the group.
            List<Synapse> synapses = new LinkedList<>();

            for (int ii = 0; ii < numSrc; ++ii) {
                int srcInd = inStream.getInt();
                int outD = inStream.getInt();
                for (int jj = 0; jj < outD; ++jj) {
                    int tarInd = inStream.getInt();
                    Synapse s = new Synapse(src.get(srcInd), tar.get(tarInd));
                    synapses.add(s);
                }
            }

            ListIterator<Synapse> synIter = synapses.listIterator();
            if (Precision.FLOAT_64 == precision) {
                if (inStream.remaining() / 8 != size) {
                    throw new InvalidDataException("Meta-Data indicates a number of synapses inconsistent with the" +
                            " remaining bytes. Check serialization configuration or file integrity.");
                }
                for (int ii = 0; ii < size; ++ii) {
                    Synapse s = synIter.next();
                    s.forceSetStrength(Double.longBitsToDouble(inStream.getLong()));
                    ret.add(s);
                }
            } else {
                if (inStream.remaining() / 4 != size) {
                    throw new InvalidDataException("Meta-Data indicates a number of synapses inconsistent with the" +
                            " remaining bytes. Check serialization configuration or file integrity.");
                }
                for (int ii = 0; ii < size; ++ii) {
                    Synapse s = synIter.next();
                    s.forceSetStrength(Float.intBitsToFloat(inStream.getInt()));
                    ret.add(s);
                }
            }
        } catch (IndexOutOfBoundsException ob) {
            ob.printStackTrace();
            System.out.println("Possible Causes: Source or target group doesn't"
                    + " have proper number of neurons. Incorrect number of"
                    + " synapses given as first number in file. Corrupted data file.");
            return null;
        } catch (InvalidDataException ide) {
            ide.printStackTrace();
            return null;
        }

        return ret;
    }

}
