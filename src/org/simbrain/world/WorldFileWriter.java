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

package org.simbrain.world;

import java.io.FileOutputStream;
import java.io.PrintStream;

import org.simbrain.util.SimbrainMath;

/**
 * <b>WorldFileWriter</b> saves a world as an xml file.
 */
public class WorldFileWriter {

	public final static String xmlDeclaration = "<?xml version=\"1.0\"?>"; 
	public final static String rootElement = "world";
	public final static String entityElement = "entity";
	public final static String nameElement = "name";
	public final static String imageNameElement = "imageName";
	public final static String xElement = "x";
	public final static String yElement = "y";
	public final static String stimElement = "stim";
	public final static String addNoiseElement = "noise";
	public final static String decayFunctionElement = "decayFunction";
	public final static String dispersionElement = "dispersion";
	public final static String noiseLevelAttribute = "level";
	
	public static void write(FileOutputStream stream, World world) {
		PrintStream ps = new PrintStream(stream);
		
		ps.println(xmlDeclaration);  
		ps.println("<" + rootElement + ">");  
		
		//Print world entity information to file output stream
		for (int i = 0; i < world.getObjectList().size(); i++) {
			WorldEntity entity = (WorldEntity) world.getObjectList().get(i);
			
			ps.println("  <" + entityElement +">");
			
			if (entity.getName() != null && !entity.getName().trim().equals("")) {
				ps.print("    <" + nameElement +">");
				ps.print(entity.getName());
				ps.println("</" + nameElement +">");			
			}

			ps.print("    <" + imageNameElement +">");
			ps.print(entity.getImageName());
			ps.println("</" + imageNameElement +">");			
			
			ps.print("    <" + xElement +">");
			ps.print(entity.getLocation().x);
			ps.print("</" + xElement +">");			
			ps.print("<" + yElement +">");
			ps.print(entity.getLocation().y);
			ps.println("</" + yElement +">");			

			ps.print("    <" + stimElement +">");
			ps.print(SimbrainMath.getVectorString(entity.getStimulus(), ","));
			ps.println("</" + stimElement +">");			
			
			ps.print("    <" + decayFunctionElement +">");
			ps.print(entity.getDecayFunction());
			ps.println("</" + decayFunctionElement +">");			

			ps.print("    <" + dispersionElement +">");
			ps.print(entity.getDispersion());
			ps.println("</" + dispersionElement +">");							
			
			if (entity.isAddNoise() == true) {	
				ps.print("    <" + addNoiseElement +" " + noiseLevelAttribute + "=\"" + entity.getNoiseLevel() + "\">");
				ps.print(entity.isAddNoise());
				ps.println("</" + addNoiseElement +">");
			}
			
			ps.println("  </" + entityElement +">");
		}
		ps.println("</" + rootElement + ">");  
		}

}