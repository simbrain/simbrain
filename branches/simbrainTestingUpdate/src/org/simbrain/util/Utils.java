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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;

/**
 * <b>Utils</b>. Utility class for simbrain package.
 */
public class Utils {

    /** File system separator. */
    private static final String FS = System.getProperty("file.separator");

    /**
     * Read a csv (comma-separated-values) files.
     *
     * @param theFile the file to read in
     * @return an two-dimensional array of comma-separated values
     */
    public static double[][] getDoubleMatrix(final File theFile) {
        String[][] stringMatrix = getStringMatrix(theFile);

        // convert strings to doubles
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
     * @return an two-dimensional array of comma-separated values
     */
    public static String[][] getStringMatrix(final File theFile) {
        CSVParser theParser = null;

        String[][] stringMatrix;

        try {
            // # is a comment delimeter in net files
            theParser = new CSVParser(new FileInputStream(theFile), "", "", "#");
            stringMatrix = theParser.getAllValues();
        } catch (java.io.FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Could not find the file \n"
                    + theFile, "Warning", JOptionPane.ERROR_MESSAGE);

            return null;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "There was a problem opening the file \n" + theFile,
                    "Warning", JOptionPane.ERROR_MESSAGE);
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
     * @return the relative path from the local directory to the file to be
     *         saved
     */
    public static String getRelativePath(final String baseDir,
            final String absolutePath) {
        int localLength = baseDir.length();
        int totalLength = absolutePath.length();
        int diff = totalLength - localLength;
        String relativePath = absolutePath.substring(totalLength - diff);
        relativePath = relativePath.replaceAll("/./", "/");
        relativePath.replace('/', System.getProperty("file.separator")
                .charAt(0)); // For windows machines..
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
        return theFile.getAbsolutePath()
                .substring(
                        0,
                        theFile.getAbsolutePath().length()
                                - theFile.getName().length());
    }

    /**
     * Convert an array of doubles into a String.
     *
     * @param theVec the array of doubles to convert
     * @param delimiter Delimiter
     * @return the String representation of the array
     */
    public static String getVectorString(final double[] theVec,
            final String delimiter) {
        String retString = "";

        for (int i = 0; i < (theVec.length - 1); i++) {
            retString = retString.concat("" + round(theVec[i], 1) + delimiter);
        }

        retString = retString.concat("" + round(theVec[theVec.length - 1], 1));

        return retString;
    }

    /**
     * Convert a delimited string of doubles into an array of doubles. Undoes
     * String getVectorString.
     *
     * @param theVec string version of vector
     * @param delimiter delimiter used in that string
     * @return the corresponding array of doubles
     */
    public static double[] getVectorString(final String theVec,
            final String delimiter) {
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
     * Returns a string rounded to the desired precision.
     *
     * @param num double to convert
     * @param precision number of decimal places
     * @return string representation of rounded decimal
     */
    public static String round(final double num, final int precision) {
        BigDecimal bd = new BigDecimal(num);
        return bd.setScale(precision, BigDecimal.ROUND_HALF_UP).toString();
    }

    /**
     * Display a documentation page under {simbrainhome}/docs/...
     *
     * @param helpPage Help page
     */
    public static void showHelpPage(final String helpPage) {
        String url = new String(System.getProperty("user.dir") + FS + "docs"
                + FS + helpPage);
        displayLocalHtmlInBrowser(url);

    }

    /**
     * Launch an .html page using the system's default browser.
     *
     * @param url the url to display. Assumes it is in the local file system.
     */
    public static void displayLocalHtmlInBrowser(final String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new File(url).toURI());
        } catch (IOException e) {
            System.err.println("Problem loading URL: " + url);
            e.printStackTrace();
        }
    }

    /**
     * Converts a floating point value into a color in HSB, with Saturation and
     * Brightness 1.
     *
     * @param fclr Float color
     * @return Hue, saturation, and brightness
     */
    public static Color floatToHue(final float fclr) {
        return Color.getHSBColor(fclr, 1, 1);
    }

    /**
     * returns the Hue associated with a Color.
     *
     * @param clr Color
     * @return Hue, saturation and brightness
     */
    public static float colorToFloat(final Color clr) {
        return Color
                .RGBtoHSB(clr.getRed(), clr.getGreen(), clr.getBlue(), null)[0];
    }

    /**
     * Convert a 2-d array of doubles to a string.
     *
     * @param matrix the matrix to print
     * @return the formatted string
     */
    public static String doubleMatrixToString(double[][] matrix) {
        String result = "";
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                result += Utils.round(matrix[i][j], 2) + " ";
            }
            result += "\n";
        }
        return result;
    }

    /**
     * <p>
     * Decides if the operating system matches.
     * </p>
     * Source adapted from org.apache.commons.lang.SystemUtils
     *
     * @param osNamePrefix the prefix for the os name
     * @return true if matches, or false if not or can't determine
     */
    static boolean getOSMatches(String osNamePrefix) {
        final String OS_NAME = System.getProperty("os.name");
        if (OS_NAME == null) {
            return false;
        }
        return OS_NAME.startsWith(osNamePrefix);
    }

    /**
     * Determines whether the system is a Mac os x.
     *
     * @return whether the system is a Mac os x.
     */
    public static boolean isMacOSX() {
        return Utils.getOSMatches("Mac OS X");
    }

    /**
     * Reimplementation of same method from
     * org.apache.commons.collections.CollectionUtils.
     *
     * @param selection the collection to filter
     * @param filter the predicate to be used in filtering.
     * @return those members of the selection to which the predicate applies
     */
    public static Collection select(final Collection selection,
            final Predicate filter) {
        Collection ret = new ArrayList();
        for (Object object : selection) {
            if (filter.evaluate(object)) {
                ret.add(object);
            }
        }
        return ret;
    }

    /**
     * Re-implementation of same method from
     * org.apache.commons.collections.CollectionUtils.
     *
     * @param a Collection
     * @param b Collection
     * @return union of two collections
     */
    public static Collection union(final Collection a, final Collection b) {
        Collection ret = new ArrayList();
        ret.addAll(a);
        ret.addAll(b);
        return ret;
    }

    /**
     * Re-implementation of same method from
     * org.apache.commons.collections.CollectionUtils.
     *
     * @param a Collection
     * @param b Collection
     * @return intersection of two collections
     */
    public static Collection intersection(final Collection a, final Collection b) {
        Collection ret = new ArrayList();
        for (Object object : a) {
            if (b.contains(object)) {
                ret.add(object);
            }
        }
        return ret;
    }

    /**
     * Return the Simbrain properties file, or null if it is not found.
     *
     * @return the Simbrain properties file
     */
    public static Properties getSimbrainProperties() {
        try {
            Properties properties = new Properties();
            String fs = System.getProperty("file.separator");
            properties.load(new FileInputStream("." + fs + "etc" + fs
                    + "config.properties"));
            return properties;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the contents of a file as a String.
     *
     * @param file the file to read
     * @return the string contents of the file
     */
    public static String readFileContents(File file) {
        StringBuilder scriptText = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        Scanner scanner = null;
        try {
            scanner = new Scanner(new FileInputStream(file));
            while (scanner.hasNextLine()) {
                scriptText.append(scanner.nextLine() + newLine);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
        return scriptText.toString();
    }

}
