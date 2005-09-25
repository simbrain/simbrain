/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.util;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.world.odorworld.OdorWorldFrame;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;
/**
 * 
 * <b>Utils</b>
 */
public class Utils {
	
	private static final String FS = System.getProperty("file.separator");

	/**
	 * Read a csv (comma-separated-values) files.
	 * 
	 * @param theFile the file to read in
	 * @return an two-dimensional array of comma-separated values
	 */	
	public static double[][] getDoubleMatrix(File theFile) {
	
		String[][] string_matrix = getStringMatrix(theFile);

		//convert strings to doubles
		double ret[][] = new double[string_matrix.length][string_matrix[0].length];
		for (int i = 0; i < string_matrix.length; i++) {
			for (int j = 0; j < string_matrix[i].length; j++) {
				ret[i][j] = Double.parseDouble(string_matrix[i][j]);
			}
		}
		return ret;
	}

	/**
	 * Read a csv (comma-separated-values) files.
	 * 
	 * @param theFile the file to read in
	 * @return an two-dimensional array of comma-separated values
	 */	
	public static String[][] getStringMatrix(File theFile) {
		CSVParser theParser = null;

		String[][] string_matrix;

		try {
			theParser = new CSVParser(new FileInputStream(theFile), "", "",
					"#"); // # is a comment delimeter in net files
			string_matrix = theParser.getAllValues();
		}  catch (java.io.FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "Could not find the file \n" + theFile,
			        "Warning", JOptionPane.ERROR_MESSAGE);
			return null;
		} catch (Exception e){
		    JOptionPane.showMessageDialog(null, "There was a problem opening the file \n" + theFile,
			        "Warning", JOptionPane.ERROR_MESSAGE);
		    e.printStackTrace();
			return null;
		}
		
		return string_matrix;
	}
	
	/**
	 * Save data as CSV (comma-separated-value) file
	 * @param data
	 * @param theFile
	 */
	public static void writeMatrix(String[][] data, File theFile) {
		FileOutputStream f = null;
		try {
			f = new FileOutputStream(theFile);
		} catch (Exception e) {
			System.out.println("Could not open file stream: " + e.toString());
		}

		if (f == null) {
			return;
		}
		
		CSVPrinter thePrinter = new CSVPrinter(f);

		thePrinter.printlnComment("");
		thePrinter.printlnComment("File: " + theFile.getName());
		thePrinter.printlnComment("");
		thePrinter.println();
		thePrinter.println(data);

		thePrinter.println();
	}
	  
		/**
		 * Helper method to create a relative path for use in saving simulation files
		 * which refer to files within directories.   Substracts the absolutePath of 
		 * the local user directory from the absolute path of the file to be saved,
		 * and converts  file-separators into forward slashes, which are used for saving
		 * simualtion files. 
		 * 
		 * @param baseDir absolute path of the local simbrain directory.
		 * @param absolutePath the absolute path of the file to be saved
		 * @return the relative path from the local directory to the file to be saved
		 */
		public static String getRelativePath(String baseDir, String absolutePath) {
			
			int localLength =  baseDir.length();
			int totalLength = absolutePath.length();
			int diff = totalLength - localLength;
			String relativePath = absolutePath.substring(totalLength - diff);
			relativePath = relativePath.replaceAll("/./", "/");
			relativePath.replace('/', System.getProperty("file.separator").charAt(0));	// For windows machines..	
			relativePath = new String("." + relativePath);
			return relativePath;
		}

		/**
		 * Convert an array of doubles into a String
		 * 
		 * @param theVec the array of doubles to convert
		 * @return the String representation of the array
		 */
		public static String getVectorString(double[] theVec, String delimiter) {
			String retString = "";
			for (int i = 0; i < theVec.length - 1; i++) {
				retString = retString.concat("" + theVec[i] + delimiter);
			}
			retString = retString.concat("" + theVec[theVec.length - 1]);
			return retString;
		}

		/**
		 * Convert a delimeted string of doubles into an array of doubles.  Undoes String getVectorString.
		 * 
		 * @param theVec string version of vector
		 * @param delimiter delimeter used in that string
		 * @return the corresponding array of doubles
		 */
		public static double[] getVectorString(String theVec, String delimiter) {
			StringTokenizer st = new StringTokenizer(theVec, delimiter);
			double[] ret = new double[st.countTokens()];
			int i = 0;
			while(st.hasMoreTokens()) {
				ret[i] = Double.parseDouble(st.nextToken());
				i++;
			}
			
			return ret;
		}
		
		
		/**
		 * Converts an array of strings containing doubles into an array of values
		 * 
		 * @param line the array of strings
		 * @return the array of doubles
		 */
		public static double[] stringArrayToDoubleArray(String[] line) {
			double[] ret = new double[line.length];
			for (int i = 0; i < line.length; i++) {
					ret[i] = Double.parseDouble(line[i]);
			}
			
			return ret;
		}
		
		/**
		 * Utility to class to convert arrays of doubles to strings.  
		 * @param data array of doubles
		 * @return string representation of that array
		 */
		public static String doubleArrayToString(double[] data) {
			String ret = new String(" ");
			for (int i = 0; i < data.length; i++) {
				String num = round(data[i],2);
				if (i == 0) {
					ret = ret + num;
				} else {
					ret = ret + ", " + num;

				}
			}
			return ret;
		}
		
		
		/**
		 * 
		 * @param num double to convert
		 * @param precision number of decimal places
		 * @return string representation of rounded decimal
		 */
		public static String round(double num, int precision) {
		
			BigDecimal bd = new BigDecimal(num);
			return bd.setScale(precision, BigDecimal.ROUND_DOWN).toString();
		
		}
		
		/**
		 * Checks whether an array list cantains a name, and warns you if it does
		 * @param al the array list to check; must be an array of strings
		 * @param theString the name to check for
		 * @return true if the name is contained in the array, false otherwise
		 */
		public static boolean containsName(ArrayList al, String theString) {
			boolean ret = false;
			
			for (int i = 0; i < al.size(); i++) {
				if(((String)al.get(i)).equalsIgnoreCase(theString)) {
					ret = true;
				} 
			}
			
			return ret;
			
		}
		
		/**
		 * Shows the quick reference guide in the help menu.  The quick reference
		 * is an html page in the Simbrain/doc directory
		 */
		public static void showQuickRef() {

			String url = null;

			if(System.getProperty("os.name").startsWith("Windows")){
				url = new String(/*"file:" +*/ System.getProperty("user.dir") 
						+ FS + "docs" + FS + "SimbrainDocs.html");
			} else {
				url = new String("file:" + System.getProperty("user.dir") 
						+ FS + "docs" + FS + "SimbrainDocs.html");
			}
			
			
			
			try {
				BrowserLauncher.openURL(url);
			} catch (IOException e) {
				e.printStackTrace();
			}   
		}

		
		/**
		 * Converts a floating point value into a color in HSB, with Saturation and Brightness 1
		 */
		public static Color floatToHue(float fclr) {
			return Color.getHSBColor(fclr, 1, (float) 1); 
		}
		
		/**
		 * returns the Hue associated with a Color
		 */
		public static float colorToFloat(Color clr) {
			return Color.RGBtoHSB(clr.getRed(), clr.getGreen(),clr.getBlue(), null)[0];
		}
		
		
		/**
		 * Shows the quick reference guide in the help menu.  The quick reference
		 * is an html page in the Simbrain/doc directory
		 */
		public static void showQuickRef(JInternalFrame frame) {

			String url = null;

			if(frame instanceof GaugeFrame){
				if(System.getProperty("os.name").startsWith("Windows")){
					url = new String(/*"file:" +*/ System.getProperty("user.dir") 
							+ FS + "docs" + FS + "Pages" + FS + "Gauge.html");
				} else {
					url = new String("file:" + System.getProperty("user.dir") 
							+ FS + "docs" + FS + "Pages" + FS + "Gauge.html");
				}
			}
			if(frame instanceof NetworkFrame){
				if(System.getProperty("os.name").startsWith("Windows")){
					url = new String(/*"file:" +*/ System.getProperty("user.dir") 
							+ FS + "docs" + FS + "Pages" + FS + "Network.html");
				} else {
					url = new String("file:" + System.getProperty("user.dir") 
							+ FS + "docs" + FS + "Pages" + FS + "Network.html");
				}
			}
			if(frame instanceof OdorWorldFrame){
				if(System.getProperty("os.name").startsWith("Windows")){
					url = new String(/*"file:" +*/ System.getProperty("user.dir") 
							+ FS + "docs" + FS + "Pages" + FS + "World.html");
				} else {
					url = new String("file:" + System.getProperty("user.dir") 
							+ FS + "docs" + FS + "Pages" + FS + "World.html");
				}
			}

			
			
			try {
				BrowserLauncher.openURL(url);
			} catch (IOException e) {
				e.printStackTrace();
			}   
		}

		
}
