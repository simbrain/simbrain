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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;

/**
 * A class for storing static methods for custom deserialization of custom
 * serializations of group objects in Simbrain.
 *
 * @author Zoë Tosi
 *
 */
public class GroupDeserializer {

    /**
     * @param rowCompByteArr the row compressed sparse matrix used to fill the
     *            synapse group in compressed byte array format.
     * @param sg the synapse group to populate
     * @return a boolean indicating success or failure of reconstruction.
     */
    public static boolean reconstructCompressedSynapseStrengths(
            byte[] rowCompByteArr, SynapseGroup sg) {
        try {
            // Read in all the data from the file stored in discrete bytes
            ByteBuffer inStream = ByteBuffer.wrap(rowCompByteArr);
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
                        newLine = newLine | index; // keep track of this end
                                                   // code
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
                    } else if (newLine == 0xFFFFFF) { // Byte and short end
                                                      // codes
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
                for (Synapse s : synapses) {
                    s.setStrength(inStream.getFloat());
                }
            } else if (inStream.remaining() == numSyns * 8) { // Float_64
                                                              // encoding
                for (Synapse s : synapses) {
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
        } catch (IndexOutOfBoundsException ob) {
            ob.printStackTrace();
            System.out
                    .println("Possible Causes: Source or target group doesn't"
                            + " have proper number of neurons. Incorrect number of"
                            + " synapses given as first number in file.");
            return false;
        } catch (InputMismatchException ime) {
            ime.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Reads a file containing the relative position and strengths of synapses,
     * in compressed row format and stored in compressed byte code and recreates
     * those synapse positions and their strengths in the supplied synapse
     * groups. Method will fail (not adding any synapses) to the given synapse
     * group if the source or target neuron groups of the supplied synapse group
     * are not of the same size as the source and target groups of the original.
     *
     * @param filename the name of the file storing the synapse values
     * @param sg the synapse group to populate
     * @return a boolean indicating success or failure of reconstruction.
     */
    public static boolean reconstructCompressedSynapseStrengths(
            String filename, SynapseGroup sg) {
        Path p = Paths.get(filename);
        // TODO: Add buffered version to decrease load on RAM for large files
        // with size of Simbrain weight compression files, this shouldn't be
        // an issue until the far future.
        try {
			return reconstructCompressedSynapseStrengths(Files.readAllBytes(p),
					sg);
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
    }

}
