/*
 * Created on Oct 3, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.util;

import java.io.File;
import java.io.FileInputStream;

import javax.swing.JOptionPane;

import com.Ostermiller.util.CSVParser;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
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
