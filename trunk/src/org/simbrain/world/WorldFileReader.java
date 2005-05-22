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

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <b>WorldFileHandler</b> parses xml files describing worlds.
 */
public class WorldFileReader extends DefaultHandler {

	protected StringBuffer contentBuffer = new StringBuffer();
	protected ArrayList entityList = new ArrayList();

	private World theWorld;
	
	// Default values
	protected String decayFunction = "Step";
	protected String imageName = "Flower.gif";
	protected int x_coord = 50;
	protected int y_coord = 50;
	protected double[] distal_stimulus;
	protected double dispersion = 100;
	protected double noiseLevel = .5;
	protected double orientation = 45; //Creature orientation
	protected boolean addNoise = false;

	public WorldFileReader(World w) {
		theWorld = w;
	}
	/**
	 * Build the list of entities which will populate the world
	 */
	public void addEntity() {
		
		//TODO: Add agent identifier in xml
		if (imageName.equals("Mouse.gif")) {			
			Agent we = new Agent(theWorld, imageName, x_coord, y_coord, orientation);
			we.setStimulusObject(new Stimulus(distal_stimulus, decayFunction, dispersion,
											addNoise, noiseLevel));	 
			entityList.add(we);
			
		} else {
			WorldEntity we = new WorldEntity(theWorld, imageName, x_coord, y_coord);
			we.setStimulusObject(new Stimulus(distal_stimulus, decayFunction, dispersion,
											addNoise, noiseLevel));	 
			entityList.add(we);

		}
		
	}
	
	
	/**
	 * Turn a comman-separated string of doubles of this form: "3,12,3,2,..." into an array of doubles
	 * 
	 * @param theArray String version of the array
	 * @return array of doubles
	 */
	private double[] parseDoubleArray(String theArray) {
		StringTokenizer st = new StringTokenizer(theArray, ",");
		double[] ret = new double[st.countTokens()];
		int i = 0;
		while (st.hasMoreElements()) {
			ret[i] = Double.parseDouble(st.nextToken());
			i++;
		}
		return ret;
	}
	
	public void startElement(String uri,String lname,String qname, Attributes attributes) {
		contentBuffer.setLength(0);
		if(lname.equals(WorldFileWriter.addNoiseElement)) {
			noiseLevel = Double.parseDouble(attributes.getValue("level"));
		}
	}

	public void characters(char[] chars, int start, int length) {
		contentBuffer.append(chars, start, length);
	}
	public void endElement(String uri, String lname, String qname) {
		if (lname.equals(WorldFileWriter.entityElement)) {
			addEntity();
		} else {
			
			String content = contentBuffer.toString().trim();
			if (lname.equals(WorldFileWriter.xElement)) {
				x_coord = Integer.parseInt(content);
			} else if (lname.equals(WorldFileWriter.yElement)) {
				y_coord = Integer.parseInt(content);
			} else if (lname.equals(WorldFileWriter.stimElement)) {
				distal_stimulus = parseDoubleArray(content);
			} else if (lname.equals(WorldFileWriter.decayFunctionElement)) {
				decayFunction = content;
			} else if (lname.equals(WorldFileWriter.dispersionElement)) {
				dispersion = Double.parseDouble(content);
			} else if (lname.equals(WorldFileWriter.addNoiseElement)) {
				addNoise = Boolean.valueOf(content).booleanValue();
			} else if (lname.equals(WorldFileWriter.imageNameElement)) {
				imageName = content;
			} else if (lname.equals(WorldFileWriter.orientationElement)) {
				orientation = Double.parseDouble(content);
			}
		}
	}


	public ArrayList getEntityList() {
		return entityList;
	}

}