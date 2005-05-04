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
			JOptionPane.showMessageDialog(null, "Could not find script file \n" + theFile, "Warning", JOptionPane.ERROR_MESSAGE);
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
	  
}
