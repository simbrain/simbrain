/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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

package org.simbrain.network;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import org.simnet.*;
import org.simnet.interfaces.*;

import edu.umd.cs.piccolo.PNode;

/**
 * <b>NetworkFileWriter</b> saves a network as an xml file.
 */
public class NetworkFileWriter {

	public final static String xmlDeclaration = "<?xml version=\"1.0\"?>"; 
	public final static String rootElement = "network";
	public final static String nameElement = "name";
	public final static String neuronElement = "neuron";
	public final static String xElement = "x";
	public final static String yElement = "y";
	public final static String biasElement = "bias";
	public final static String decayElement = "decay";
	public final static String activationElement = "activation";
	public final static String outputElement = "output";
	public final static String inputElement = "input";
	public final static String activationFunctionElement = "f_act";
	public final static String outputFunctionElement= "f_out";
	public final static String upperBoundElement= "upper";
	public final static String lowerBoundElement= "lower";
	public final static String incrementElement = "increment";
	public final static String outputThresholdElement = "output threshold";
	public final static String outputSignalElement = "output signal";
	public final static String wtSrcElement = "src";
	public final static String wtTargetElement = "tar";
	public final static String wtLearningElement = "rule";
	public final static String wtValueElement = "val";
	public final static String wtUpperBoundElement = "upper";
	public final static String wtLowerBoundElement = "lower";
	public final static String wtIncrementElement = "increment";
	public final static String wtMomentumElement = "momentum";
	
	public final static String weightElement= "weight";
	
	
	public static void write(FileOutputStream stream, NetworkPanel netPanel) {
		
		
		PrintStream ps = new PrintStream(stream);
		
		ps.println(xmlDeclaration);  
		ps.println("<" + rootElement + ">");  
		
		//Save neurons
		ArrayList node_list = netPanel.getNodeList();
		for (int i = 0; i < node_list.size(); i++) {
			PNode pn = (PNode) node_list.get(i);
			if (pn instanceof PNodeNeuron) {
				Neuron n = ((PNodeNeuron) pn).getNeuron();
				ps.println("  <" + neuronElement +">");	
					
					ps.print("    <" + nameElement +">");			
					ps.print("n" + i);
					ps.println("</" + nameElement +">");		
					
					ps.print("    <" + xElement +">");			
					ps.print(Integer.toString((int) NetworkPanel.getGlobalX(pn)));				
					ps.print("</" + xElement +">");			
					ps.print("<" + yElement +">");
					ps.print(Integer.toString((int) NetworkPanel.getGlobalY(pn)));				
					ps.println("</" + yElement +">");

					ps.print("    <" + outputElement +">");			
					ps.print(n.getOutputLabel());
					ps.print("</" + outputElement +">");	
					ps.print("<" + inputElement +">");			
					ps.print(n.getInputLabel());
					ps.println("</" + inputElement+">");	
	
					ps.print("    <" + activationElement +">");			
					ps.print(n.getActivation());
					ps.println("</" + activationElement +">");
			
					ps.print("    <" + decayElement +">");			
					ps.print(n.getDecay());
					ps.println("</" + decayElement +">");		
						
					ps.print("    <" + biasElement +">");			
					ps.print(n.getBias());
					ps.println("</" + biasElement +">");	
				
					ps.print("    <" + activationFunctionElement +">");			
					ps.print(n.getActivationFunction().getName());
					ps.println("</" + activationFunctionElement +">");	
					
					ps.print("    <" + outputSignalElement +">");			
					ps.print(n.getOutputSignal());
					ps.println("</" + outputSignalElement +">");										
												
					ps.print("    <" + lowerBoundElement +">");			
					ps.print(n.getLowerBound());
					ps.print("</" + lowerBoundElement +">");	
					ps.print("<" + upperBoundElement +">");			
					ps.print(n.getUpperBound());
					ps.println("</" + upperBoundElement +">");	
														
					ps.print("    <" + incrementElement +">");			
					ps.print(n.getIncrement());
					ps.println("</" + incrementElement +">");		

									
					ps.println("  </" + neuronElement +">");
			}
		}
		
		// Save weights
		for (int i = 0; i < node_list.size(); i++) {
			PNode pn = (PNode) node_list.get(i);
			if (pn instanceof PNodeWeight) {
				Synapse w = ((PNodeWeight) pn).getWeight();
				ps.println("  <" + weightElement +">");	
					
					ps.print("    <" + wtSrcElement +">");			
					ps.print(((Neuron) w.getSource()).getName());
					ps.print("</" + wtSrcElement +">");		
					ps.print("<" + wtTargetElement +">");			
					ps.print(((Neuron) w.getTarget()).getName());
					ps.println("</" + wtTargetElement +">");		
	
					ps.print("    <" + wtLearningElement +">");			
					ps.print(w.getLearningRule().getName());
					ps.println("</" + wtSrcElement +">");		

					ps.print("    <" + wtValueElement +">");			
					ps.print("" + Double.toString(w.getStrength()));
					ps.println("</" + wtValueElement +">");		
	
					ps.print("    <" + wtLowerBoundElement +">");			
					ps.print("" + Double.toString(w.getLowerBound()));
					ps.print("</" + wtLowerBoundElement +">");		
					ps.print("<" + wtUpperBoundElement +">");			
					ps.print("" + Double.toString(w.getUpperBound()));
					ps.println("</" + wtUpperBoundElement +">");		

					ps.print("    <" + wtIncrementElement +">");			
					ps.print("" + Double.toString(w.getIncrement()));
					ps.println("</" + wtIncrementElement +">");		
		
					ps.print("    <" + wtMomentumElement +">");			
					ps.print("" + Double.toString(w.getMomentum()));
					ps.println("</" + wtMomentumElement +">");		
					
				ps.println("  </" + weightElement +">");	
			}
		}
		
		
		ps.println("</" + rootElement + ">");  
		
		
	}

}