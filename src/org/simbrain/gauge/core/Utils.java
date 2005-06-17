/*
 * Part of HiSee, a tool for visualizing high dimensional datasets
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.gauge.core;

import java.math.BigDecimal;



/**
 * <b>Utils</b> contains static utility classes.
 */
public class Utils {
	
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
}
