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
package org.simbrain.util;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;


/**
 * <b>SimnetUtils</b>. Utility class for simbrain package.
 */
public class Utils {

    /** File system separator. */
    private static final String FS = System.getProperty("file.separator");

     /**
     * Read a csv (comma-separated-values) files.
     *
     * @param theFile the file to read in
     *
     * @return an two-dimensional array of comma-separated values
     */
    public static double[][] getDoubleMatrix(final File theFile) {
        String[][] stringMatrix = getStringMatrix(theFile);

        //convert strings to doubles
        double[][] ret = new double[stringMatrix.length][stringMatrix[0].length];

        for (int i = 0; i < stringMatrix.length; i++) {
            for (int j = 0; j < stringMatrix[i].length; j++) {
                ret[i][j] = Double.parseDouble(stringMatrix[i][j]);
            }
        }

        return ret;
    }

    /**
     * Read a csv (comma-separated-values) files.
     *
     * @param theFile the file to read in
     *
     * @return an two-dimensional array of comma-separated values
     */
    public static String[][] getStringMatrix(final File theFile) {
        CSVParser theParser = null;

        String[][] stringMatrix;

        try {
            //# is a comment delimeter in net files
            theParser = new CSVParser(new FileInputStream(theFile), "", "", "#");
            stringMatrix = theParser.getAllValues();
        } catch (java.io.FileNotFoundException e) {
            JOptionPane.showMessageDialog(
                                          null, "Could not find the file \n" + theFile, "Warning",
                                          JOptionPane.ERROR_MESSAGE);

            return null;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                                          null, "There was a problem opening the file \n" + theFile, "Warning",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return null;
        }

        return stringMatrix;
    }

    /**
     * Save data as CSV (comma-separated-value) file.
     *
     * @param data Data to be written
     * @param theFile File to be written to
     */
    public static void writeMatrix(final String[][] data, final File theFile) {
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
     * Helper method to create a relative path for use in saving simulation
     * files which refer to files within directories. Substracts the
     * absolutePath of the local user directory from the absolute path of the
     * file to be saved, and converts file-separators into forward slashes,
     * which are used for saving simualtion files.
     * 
     * @param baseDir absolute path of the local simbrain directory.
     * @param absolutePath the absolute path of the file to be saved
     * 
     * @return the relative path from the local directory to the file to be
     *         saved
     */
    public static String getRelativePath(final String baseDir, final String absolutePath) {
        int localLength = baseDir.length();
        int totalLength = absolutePath.length();
        int diff = totalLength - localLength;
        String relativePath = absolutePath.substring(totalLength - diff);
        relativePath = relativePath.replaceAll("/./", "/");
        relativePath.replace('/', System.getProperty("file.separator").charAt(0)); // For windows machines..
        relativePath = new String("." + relativePath);

        return relativePath;
    }

    /**
     * Extract file name from a path description.
     *
     * @param thePath the path
     * @return the extracted file name
     */
    public static String getNameFromPath(final String thePath) {
        String[] files = thePath.split("/");
        String ret = files[files.length - 1];
        return ret;
    }

    /**
     * Get the directory component of a file.
     *
     * @param theFile the file to get the directory of.
     * @return the extracted directory path
     */
    public static String getDir(final File theFile) {
        return theFile.getAbsolutePath().substring(0, theFile.getAbsolutePath().length() - theFile.getName().length());
    }

    /**
     * Convert an array of doubles into a String.
     *
     * @param theVec the array of doubles to convert
     * @param delimiter Delimiter
     *
     * @return the String representation of the array
     */
    public static String getVectorString(final double[] theVec, final String delimiter) {
        String retString = "";

        for (int i = 0; i < (theVec.length - 1); i++) {
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
     *
     * @return the corresponding array of doubles
     */
    public static double[] getVectorString(final String theVec, final String delimiter) {
        StringTokenizer st = new StringTokenizer(theVec, delimiter);
        double[] ret = new double[st.countTokens()];
        int i = 0;

        while (st.hasMoreTokens()) {
            ret[i] = Double.parseDouble(st.nextToken());
            i++;
        }

        return ret;
    }

    /**
     * Converts an array of strings containing doubles into an array of values.
     *
     * @param line the array of strings
     *
     * @return the array of doubles
     */
    public static double[] stringArrayToDoubleArray(final String[] line) {
        double[] ret = new double[line.length];

        for (int i = 0; i < line.length; i++) {
            ret[i] = Double.parseDouble(line[i]);
        }

        return ret;
    }

    /**
     * Utility to class to convert arrays of doubles to strings.
     *
     * @param data array of doubles
     *
     * @return string representation of that array
     */
    public static String doubleArrayToString(final double[] data) {
        String ret = new String(" ");

        for (int i = 0; i < data.length; i++) {
            String num = round(data[i], 2);

            if (i == 0) {
                ret = ret + num;
            } else {
                ret = ret + ", " + num;
            }
        }

        return ret;
    }

    /**
     * @param num double to convert
     * @param precision number of decimal places
     *
     * @return string representation of rounded decimal
     */
    public static String round(final double num, final int precision) {
        BigDecimal bd = new BigDecimal(num);

        return bd.setScale(precision, BigDecimal.ROUND_DOWN).toString();
    }

    /**
     * Checks whether an array list cantains a name, and warns you if it does.
     *
     * @param al the array list to check; must be an array of strings
     * @param theString the name to check for
     *
     * @return true if the name is contained in the array, false otherwise
     */
    public static boolean containsName(final ArrayList al, final String theString) {
        boolean ret = false;

        for (int i = 0; i < al.size(); i++) {
            if (((String) al.get(i)).equalsIgnoreCase(theString)) {
                ret = true;
            }
        }

        return ret;
    }

    /**
     * Shows the quick reference guide in the help menu.  The quick reference is an html page in the Simbrain/doc
     * directory
     */
    public static void showQuickRef() {
        String url = null;

        if (System.getProperty("os.name").startsWith("Windows")) {
            url = new String(/*"file:" +*/
                System.getProperty("user.dir") + FS + "docs" + FS + "SimbrainDocs.html");
        } else {
            url = new String("file:" + System.getProperty("user.dir") + FS + "docs" + FS + "SimbrainDocs.html");
        }

        try {
            BrowserLauncher.openURL(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts a floating point value into a color in HSB, with Saturation and Brightness 1.
     * @param fclr Float color
     * @return Hue, saturation, and brightness
     */
    public static Color floatToHue(final float fclr) {
        return Color.getHSBColor(fclr, 1, (float) 1);
    }

    /**
     * returns the Hue associated with a Color.
     * @param clr Color
     * @return Hue, saturation and brightness
     */
    public static float colorToFloat(final Color clr) {
        return Color.RGBtoHSB(clr.getRed(), clr.getGreen(), clr.getBlue(), null)[0];
    }

    /**
     * Shows the quick reference guide in the help menu.  The quick reference is an html page in the Simbrain/doc
     * directory.
     * @param helpPage Help page
     */
    public static void showQuickRef(final String helpPage) {
        String url = null;

        if (System.getProperty("os.name").startsWith("Windows")) {
            url = new String(/*"file:" +*/
            System.getProperty("user.dir") + FS + "docs" + FS + "Pages" + FS
                    + helpPage);
        } else {
            url = new String("file:" + System.getProperty("user.dir") + FS
                    + "docs" + FS + "Pages" + FS + helpPage);
        }

        try {
            BrowserLauncher.openURL(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
