package org.simbrain.util.simbrain_io;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.randomizer.PolarizedRandomizer;

public class SynapseGroupIO {

	public enum Precision {
		FLOAT_32, FLOAT_64;
	}
	
	
	
	/**
	 * Produces a row compressed representation of the weight matrix represented
	 * by the given synapse group.
	 * @param sg the synapse group to store in row-compressed form
	 * @return a long 2d array where the first array is the column values
	 * and the 2nd array is the weight values stored as long bits.
	 */
	public static long [][] rowCompression (SynapseGroup sg) {

    	long [] wts = new long [sg.size()];
    	long [] colInds;
    	List<Integer> colIndArrL;
    	double [][] indices = new double[sg.getSourceNeurons().size()]
    			[sg.getTargetNeurons().size()];
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
    		indices[i][j] = s.getStrength();
    	}
    	for (Synapse s : sg.getInhibitorySynapses()) {
    		i = sourceMap.get(s.getSource());
    		j = targetMap.get(s.getTarget());
    		indices[i][j] = s.getStrength();
    	}
    	i = 0;
    	j = 0;
    	colIndArrL = new ArrayList<Integer>();
    	for (int k = 0, n = sg.getSourceNeurons().size(); k < n; k++) {
    		for (int l = 0, m = sg.getTargetNeurons().size(); l < m; l++) {
    			if (indices[k][l] != 0) {
    				colIndArrL.add(l);
    				wts[j++] = Double.doubleToLongBits(indices[k][l]);
    			}
    		}
    		// No need for a row-end code for the last row.
    		if (k < n-1)
    			colIndArrL.add(-1);
    	}
    	long [][] rowCompression = new long[2][];
    	colInds = new long[colIndArrL.size()];
    	for (int w = 0, n = colIndArrL.size(); w < n; w++) {
    		colInds[w] = colIndArrL.get(w).intValue();
    	}
    	rowCompression[0] = colInds;
    	rowCompression[1] = wts;
    	return rowCompression;

	}
	
	/**
	 * Indexes cannot be long values, only integers, however it is sometimes
	 * useful to store them in a long array. Converts the long array to an
	 * int array and passes that to compressRowIndices2ByteArray as per normal.
	 * @param colInds
	 * @return
	 */
	public static byte [] compressRowIndices2ByteArray(long [] colInds) {
		int [] riInt = new int[colInds.length];
		for (int i = 0, n = colInds.length; i < n; i++) {
			riInt[i] = (int) colInds[i];
		}
		return compressRowIndices2ByteArray(riInt);
	}
	
	/**
	 * Converts the column indexes of a compressed row representation into
	 * a byte array. Further compresses these values by using the smallest
	 * number of discrete bytes to store each index value. Ex: if an index has
	 * a value less than 255 it is stored in a single byte, otherwise it is 
	 * stored as a short, and so on if the index is greater than 65535.
	 * @param colInds column values separated by a new row code (-1 or
	 *  0xffffffff) representing positions in a sparse matrix
	 * @return the compressed byte array represenation of the column indices
	 */
	public static byte [] compressRowIndices2ByteArray(int [] colInds) {
		final byte maxByte = -1;
    	List<Byte> preByteArray = new ArrayList<Byte>();
    	boolean switchedToShorts = false;
    	boolean switchedToInts = false;
    	for (int i = 0, n = colInds.length; i < n; i++) {
    		if (colInds[i] == -1) { //Blank row
    			// New Row code
    			for (int k = 0; k < 4; k++) {
    				preByteArray.add(maxByte);
    			}
    			switchedToShorts = false;
    			switchedToInts = false;
    			continue;
    		}
    		if (i > 0) {
    			if (colInds[i - 1] >= colInds[i]) {
    				// New row code
        			for (int k = 0; k < 4; k++) {
        				preByteArray.add(maxByte);
        			}
        			switchedToShorts = false;
        			switchedToInts = false;
    			}
    		}
    		
    		if (!switchedToShorts && !switchedToInts) {
    			if (colInds[i] > Byte.MAX_VALUE * 2) {
    				switchedToShorts = true;
    				preByteArray.add(maxByte);
        			if (colInds[i] > Short.MAX_VALUE * 2) {
        				switchedToInts = true;
        				preByteArray.add(maxByte);
        				preByteArray.add(maxByte);
        			}
    			} else {
    				preByteArray.add((byte) (colInds[i] << 24 >>> 24));
    			}
    		}
    		
    		if (switchedToShorts && !switchedToInts) {
    			if (colInds[i] > Short.MAX_VALUE * 2) {
    				switchedToInts = true;
    				// No longer using shorts end code
    				preByteArray.add(maxByte);
    				preByteArray.add(maxByte);
    			} else {
    				ByteBuffer b = ByteBuffer.allocate(2);
    				b.putShort((short)(colInds[i] << 16 >>> 16));
    				for (byte sec : b.array()) {
    					preByteArray.add(sec);
    				}
    			}
    		}
    		
    		if (switchedToInts) {
    			ByteBuffer b = ByteBuffer.allocate(4);
				b.putInt(colInds[i]);
				for (byte sec : b.array()) {
					preByteArray.add(sec);
				}
    		}
    		
    	}
    	byte [] byteArr = new byte[preByteArray.size()];
    	int i = 0;
    	for (Byte b : preByteArray) {
    		byteArr[i++] = b.byteValue();
    	}
    	
    	return byteArr;
    	
	}
	
	/**
	 * Saves a synapse group with a given precision representing its weights
	 * to a file with the given filename. The format it is stored in represents
	 * compressed row storage where the column indices are stored using the
	 * least possible number of discrete bytes and separating rows with an
	 * end-code of -1 as an integer or 0xffffffff. 
	 * @param synGrp
	 * @param wtPrecision
	 * @param filename
	 */
	public static void serializeCompressedSynGroup(SynapseGroup synGrp,
			Precision wtPrecision, String filename) {
		long [][] rowCompression = rowCompression(synGrp);
		byte [] byteCompressedRowInds =
				compressRowIndices2ByteArray(rowCompression[0]);
    	try (OutputStream out = new BufferedOutputStream(
    			new FileOutputStream(filename))) {
    		ByteBuffer meta = ByteBuffer.allocate(4);
    		meta.putInt(synGrp.size());
    		out.write(meta.array());
    		out.write(byteCompressedRowInds);
    		long [] wts = rowCompression[1];
    		ByteBuffer weights;
    		if (Precision.FLOAT_64 == wtPrecision) {
    			weights = ByteBuffer.allocate(wts.length * 8);
    			for (long l : wts) {
    				weights.putLong(l);
    			}
    			out.write(weights.array());
    		} else if (Precision.FLOAT_32 == wtPrecision){
    			weights = ByteBuffer.allocate(wts.length * 4);
    			int [] intWts = new int[wts.length];
    			for (int i = 0, n  = wts.length; i < n; i++) {
    				intWts[i] = Float.floatToIntBits((float) Double
    						.longBitsToDouble(wts[i]));
    			}
    			for (int i : intWts) {
    				weights.putInt(i);
    			}
    			out.write(weights.array());
    		}
    	} catch (IOException ie) {
    		ie.printStackTrace();
    	}
	}
	
	/**
	 * Reads a file containing the relative position and strengths of synapses,
	 * in compressed row format and stored in compressed byte code and recreates
	 * those synapse positions and their strengths in the supplied synapse 
	 * groups. Method will fail (not adding any synapses) to the given
	 * synapse group if the source or target neuron groups of the supplied
	 * synapse group are not of the same size as the source and target groups
	 * of the original.
	 * @param filename the name of the file storing the synapse values
	 * @param sg the synapse group to populate
	 */
	public static void reconstructCompressedSynapseStrengths (String filename,
			SynapseGroup sg) {
		Path p = Paths.get(filename);
		// TODO: Add buffered version to decrease load on RAM for large files
		// with size of Simbrain weight compression files, this shouldn't be
		// an issue until the far future.
		try {
			// Read in all the data from the file stored in discrete bytes
			ByteBuffer inStream = ByteBuffer.wrap(Files
					.readAllBytes(p));
			// First number is always the number of synapses stored as an int
			int numSyns = inStream.getInt();
			// Assume that the file is using bytes to encode the index values
			// this assumption can be countered if the byte -> short or 
			// byte -> int end code is read.
			boolean usingBytes = true;
			boolean usingShorts = false;
			boolean usingInts = false;
			int row = 0;
			// The column number
			int index;
			// Keeps track of end codes to differentiate between
			// cases of multiple end codes and the new-line code.
			int newLine; 
			List<Neuron> src = sg.getSourceNeurons();
			List<Neuron> tar = sg.getTargetNeurons();
			// Store synapses before putting them in the group.
			List<Synapse> synapses = new LinkedList<Synapse>();
			for (int i = 0; i < numSyns; i++) {
				index = 0;
				newLine = 0;
				if (usingBytes) {
					index = inStream.get() << 24 >>> 24;
					if (index == 0xFF) { // Byte -> Short end code
						usingShorts = true;
						usingBytes = false;
						newLine = newLine | index; // keep track of this end code
					}
				}
				
				if (usingShorts) {
					index = inStream.getShort() << 16 >>> 16;
					if (index == 0xFFFF) { // Short -> int end code
						usingInts = true;
						usingShorts = false;
						if (newLine == 0xFF) {
							// Keep track of end code
							newLine = (newLine << 16) | index;
						} else {
							// Keep track of end code
							newLine = index;
						}
					}
				}
				
				if (usingInts) {
					boolean newRow = false;
					ByteBuffer b = ByteBuffer.allocate(4);
					if (newLine == 0) { // No end codes have been read
						index = inStream.getInt();
						newRow = index == -1;
					} else if (newLine == 0xFFFF) { // Short end code was read
						// Check that next two bytes aren't another end code
						short sh = inStream.getShort();
						b.putShort(sh);
						newLine = (newLine << 16) | sh;
						newRow = newLine == -1; // Next two bytes WERE another
						// end code, which means it was actually a new line code
						// not an end code.
						// If not, then it was just a transition from short
						// coding to int coding
						if (!newRow) {
							index = (b.getShort() << 16) | inStream.getShort();
						}
					} else if (newLine == 0xFFFFFF) { // Byte and short end codes
						// were read
						// Check that the next byte isn't 0xff
						byte by = inStream.get();
						b.put(by);
						newLine = (newLine << 8) | by;
						// If so it was a new line code, if not then it was
						// just a transition from byte coding directly to
						// int coding.
						newRow = newLine == -1;
						if (!newRow) {
							b.put(inStream.get());
							b.putShort(inStream.getShort());
							index = b.getInt();
						}
					}
					if (newRow) {
						row++; // increment the row
						i--; // Each new row code takes up a position in the
						// index array and does not correspond to an actual
						// synapse, thus decrement i because we want to keep
						// going until we're sure we've accounted for every
						// synapse.
						
						// Reset our assumptions about how index is stored until
						// we have evidence to the contrary.
						usingBytes = true;
						usingShorts = false;
						usingInts = false;
						continue; // don't create a synapse.
					} 
				}
				// Where index is the column or index in the target neuron group
				Synapse s = new Synapse(src.get(row), tar.get(index));
				synapses.add(s);
			}
			
			if (inStream.remaining() == numSyns * 4) { // Float_32 encoding
				for (Synapse s: synapses) {
					s.setStrength(inStream.getFloat());
				}
			} else if (inStream.remaining() == numSyns * 8) { // Float_64 encoding
				for (Synapse s: synapses) {
					s.setStrength(inStream.getDouble());
				}
			} else {
				// Only 2 precisions available. If there is a mismatch then
				// there are too little or to many bytes representing weights
				// given everything we've determined so far
				throw new InputMismatchException("Byte inconsistency."
						+ " Remaining bytes in file are inconsistent with"
						+ " weight values encoded as either 32-bit or 64-bit"
						+ " floating point values.");
			}
			// Assuming there are no errors, populate the synapse group
			// with the reconstructed synapses.
			for (Synapse s : synapses) {
				sg.addSynapseUnsafe(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IndexOutOfBoundsException ob) {
			ob.printStackTrace();
			System.out.println("Source or target group doesn't have"
					+ " proper number of neurons.");
		} catch (InputMismatchException ime) {
			ime.printStackTrace();
		}
		
	}
	
//	// Beginning of more general solution to custom synapse serialization
//	// for all relevant numerical values associated with synapses.
//	private static class SynapseGroupData {
//		
//		private long [] weights;
//		private int [] indices;
//		private boolean useGroupLevelStats;
//		private Map<Class, Byte> surMap;
//		private Map<SpikeResponder, Byte> spikeResMap;
//		private double [] upBounds;
//		private double [] lowBounds;
//		private int [] delays;
//		private double [] increments;
//		private boolean [] freezes;
//		private boolean [] enableds;
//		
//		public SynapseGroupData(SynapseGroup synGrp) {
//			
//		}
//		
//		/**
//		 * 
//		 * @param sg
//		 * @return
//		 */
//		public void rowCompression (SynapseGroup sg) {
//	    	weights = new long [sg.size()];
//	    	indices = new int [sg.size()];
//			if (!sg.isUseGroupLevelSettings()) {
//				surMap = new HashMap<Class<?>, Byte>();
//				spikeResMap = new HashMap<SpikeResponder, Byte>();
//				upBounds = new double [sg.size()];
//				lowBounds = new double [sg.size()];
//				delays = new int [sg.size()];
//				increments = new double [sg.size()];
//				freezes = new boolean [sg.size()];
//				enableds = new boolean [sg.size()];
//			}
//	    	Synapse [][] synMat = new Synapse[sg.getSourceNeurons().size()]
//	    			[sg.getTargetNeurons().size()];
//	    	int i = 0;
//	    	int j = 0;
//	    	Map<Neuron, Integer> sourceMap = new HashMap<Neuron, Integer>();
//	    	Map<Neuron, Integer> targetMap = new HashMap<Neuron, Integer>();
//	    	for (Neuron n : sg.getSourceNeurons()) {
//	    		sourceMap.put(n, i++);
//	    	}
//	    	for (Neuron n : sg.getTargetNeurons()) {
//	    		targetMap.put(n, j++);
//	    	}
//	    	for (Synapse s : sg.getExcitatorySynapses()) {
//	    		i = sourceMap.get(s.getSource());
//	    		j = targetMap.get(s.getTarget());
//	    		synMat[i][j] = s;
//	    	}
//	    	for (Synapse s : sg.getInhibitorySynapses()) {
//	    		i = sourceMap.get(s.getSource());
//	    		j = targetMap.get(s.getTarget());
//	    		synMat[i][j] = s;
//	    	}
//	    	i = 0;
//	    	j = 0;
//	    	int i_before;
//	    	byte spkResCode = 0;
//	    	byte surCode = 0;
//	    	for (int k = 0, n = sg.getSourceNeurons().size(); k < n; k++) {
//	    		i_before = i;
//	    		for (int l = 0, m = sg.getTargetNeurons().size(); l < m; l++) {
//	    			if (synMat[k][l] != null) {
//	    				indices[i] = l;
//	    				weights[j++] = Double.doubleToLongBits(synMat[k][l]
//	    						.getStrength());
//	    				if (!sg.isUseGroupLevelSettings()) {
//	    					upBounds[i] = synMat[k][l].getUpperBound();
//	    					lowBounds[i] = synMat[k][l].getLowerBound();
//	    					delays[i] = synMat[k][l].getDelay();
//	    					increments[i] = synMat[k][l].getIncrement();
//	    					freezes[i] = synMat[k][l].isFrozen();
//	    					enableds[i] = synMat[k][l].isEnabled();
//	    					
//	    					
//	    					
//	    				}
//	    				i++;
//	    			}
//	    		}
//	    		if (i_before == i) { //empty row
//	    			indices = Arrays.copyOf(indices, indices.length + 1);
//	    			indices[i++] = -1;
//	    		}
//	    	}
//		}
//		
//	}
//	
	
	/**
	 * Test Main
	 * @param args
	 */
    public static void main(String[] args) {
    	
    	Network net = new Network();
        NeuronGroup ng1 = new NeuronGroup(net, 2048);
    	PolarizedRandomizer exRand = new PolarizedRandomizer(Polarity.EXCITATORY,
    			ProbDistribution.LOGNORMAL);
    	PolarizedRandomizer inRand = new PolarizedRandomizer(Polarity.INHIBITORY,
    			ProbDistribution.LOGNORMAL);
    	exRand.setParam1(2.5);
    	inRand.setParam1(3.5);	

    	Sparse rCon = new Sparse(0.01, false, false);
    	SynapseGroup sg = SynapseGroup.createSynapseGroup(ng1, ng1, rCon, 0.5,
    			exRand, inRand);
    	System.out.println(sg.size());
    	long start = System.nanoTime();
    	serializeCompressedSynGroup(sg, Precision.FLOAT_64, "Test1.bin");
    	NeuronGroup ng2 = new NeuronGroup(net, 2048);
    	SynapseGroup sg2 = new SynapseGroup(ng2, ng2);
    	reconstructCompressedSynapseStrengths("./Test1.bin", sg2);
    	long end = System.nanoTime();
    	System.out.println("Time :" + SimbrainMath.roundDouble((end-start)
    			/ Math.pow(10, 9), 4));
    	System.out.println(sg2.size());
    	System.out.println("Original " + sg.size());
    	System.out.println("Reconstructed " + sg2.size());
    	int orZeros = 0;
    	int recZeros = 0;
    	int weightChecks = 0;
    	List<Neuron> ng1List = ng1.getNeuronList();
    	List<Neuron> ng2List = ng2.getNeuronList();
    	for (int i = 0, n = ng1.size(); i < n; i++) {
    		Neuron n1src = ng1List.get(i);
    		Neuron n2src = ng2List.get(i);
    		if (n1src.getFanOut().size() == 0) {
    			orZeros++;
    		}
    		if (n2src.getFanOut().size() == 0) {
    			recZeros++;
    		}
    		for (int j = 0, m = ng2.size(); j < m; j++) {
    			Neuron n1tar = ng1List.get(j);
    			Neuron n2tar = ng2List.get(j);
    			Synapse s1 = n1src.getFanOut().get(n1tar);
    			Synapse s2 = n2src.getFanOut().get(n2tar);
    			if (s1 == null && s2 != null) {
    				System.out.println("PositionError");
    				break;
    			} 
    			if (s2 == null && s1 != null) {
    				System.out.println("PositionError");
    				break;
    			} 
    			if (s1 != null && s2 != null) {
    				weightChecks++;
    				if (s1.getStrength() != s2.getStrength()) {
    					System.out.println("Weight mismatch");
    					System.out.println(s1.getStrength() + " "
    							+ s2.getStrength());
    					break;
    				}
    			}
    		}
    	}
    	System.out.println(orZeros);
    	System.out.println(recZeros);
    	System.out.println(weightChecks);
    }
}
