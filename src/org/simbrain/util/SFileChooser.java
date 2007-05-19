/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;


/**
 * <b>SFileChooser</b> extends java's JFileChooser, providing for automatic adding of file extensions, memory of
 * file-locations, and checks to prevent file-overwrites.
 */
public class SFileChooser extends JFileChooser {

    /** The type of extension used by files this JFileChooser chooses. */
    private String extensionType;

    /** A memory of the last directory this FileChooser was in. */
    private String currentDirectory;

    /**
     * Creates file chooser dialog.
     *
     * @param cd Open and save directory
     * @param ext File type extension for open and save
     */
    public SFileChooser(final String cd, final String ext) {
        extensionType = ext;
        currentDirectory = cd;

        //These convolutions are necessary to prevent
        //    exceptions on the mac os distribution
        File dir = new File(cd);

        try {
            setCurrentDirectory(dir.getCanonicalFile());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        addChoosableFileFilter(new FileFilter());
    }

    /**
     * Shows dialog for opening files.
     *
     * @return File if selected
     */
    public File showOpenDialog() {
        int result = showDialog(null, "Open");

        if (result == JFileChooser.APPROVE_OPTION) {
            currentDirectory = getCurrentDirectory().getPath();

            return getSelectedFile();
        }

        return null;
    }

    /**
     * Shows dialog for saving files.
     *
     * @return Name of file saved
     */
    public File showSaveDialog() {
        Object[] options = {"OK", "Cancel" };
        int result = showDialog(this, "Save");

        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        if (getSelectedFile().exists()) {
            int ret = JOptionPane.showOptionDialog(null, "The file \""
                    + getSelectedFile().getName()
                    + "\" already exists. Overwrite?", "Warning",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);

            if (ret == JOptionPane.YES_OPTION) {
                File tmpFile = getSelectedFile();
                tmpFile = addExtension(tmpFile, extensionType);
                currentDirectory = getCurrentDirectory().getPath();

                return tmpFile;
            }
        } else {
            File tmpFile = getSelectedFile();
            tmpFile = addExtension(tmpFile, extensionType);
            currentDirectory = getCurrentDirectory().getPath();

            return tmpFile;
        }

        return null;
    }

    /**
     * File-filter.
     */
    class FileFilter extends javax.swing.filechooser.FileFilter {

        /**
         * Determines if the file has the correct extension type.
         *
         * @param file File to be checked
         * @return whether the file has the correct extension type
         */
        public boolean accept(final File file) {
            String filename = file.getName();

            return (filename.endsWith("." + extensionType) || file.isDirectory());
        }

        /**
         * @return description of the extension.
         */
        public String getDescription() {
            return "*." + extensionType;
        }
    }

    /**
     * Check to see if the file has the extension, and if not, add it.
     *
     * @param theFile File to add extension to
     * @param extension Extension to add to file
     * @return The file name with the correct extension
     */
    private File addExtension(final File theFile, final String extension) {
        if (theFile.getName().endsWith("." + extension)) {
            return theFile;
        } else {
            File output = new File(theFile.getAbsolutePath().concat("." + extension));

            if (theFile.exists()) {
                theFile.renameTo(output);

                return theFile;
            } else {
                return output;
            }
        }
    }

    /**
     * Returns the directory this chooser is in.
     *
     * @return the directory this chooser is in.
     */
    public String getCurrentLocation() {
        return currentDirectory;
    }
}
