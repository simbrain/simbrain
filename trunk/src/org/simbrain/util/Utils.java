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
package org.simbrain.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import com.Ostermiller.util.CSVParser;

public class Utils {

	  public static double[][] getDoubleMatrix(File theFile) {
		FileInputStream f = null;
		String line = null;
		CSVParser theParser = null;

		String[][] string_matrix;
		
		try {
			theParser =
				new CSVParser(f = new FileInputStream(theFile), "", "", "#"); // # is a comment delimeter in net files
			string_matrix = theParser.getAllValues();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Could not find file \n" + theFile, "Warning", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		//convert strings to doubles
		double ret[][] = new double[string_matrix.length][string_matrix[0].length];
		for(int i = 0; i < string_matrix.length; i++) {
			for(int j = 0; j < string_matrix[i].length; j++) {
				ret[i][j] = Double.parseDouble(string_matrix[i][j]);
			}
		}
		return ret;
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
		
	  
}
