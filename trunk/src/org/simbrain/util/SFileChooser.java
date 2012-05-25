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

import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * <b>SFileChooser</b> extends java's JFileChooser, providing for automatic
 * adding of file extensions, memory of file-locations, and checks to prevent
 * file-overwrites.
 *
 * 2008-10-09 Matt Watson - modified to support new serialization approach more
 * easily.
 */
public class SFileChooser {

    /** Default serial version id */
    private static final long serialVersionUID = 1L;

    /**
     * Whether to use the native file chooser, or the Swing file chooser.
     *
     * TODO: Move this to choice to a property file.
     */
    private boolean useNativeFileChooser = true;

    /**
     * The map of extensions and their descriptions in the order
     * of their addition.
     */
    private final LinkedHashMap<String, String> exts = new LinkedHashMap<String, String>();

    /**
     * The description of the file formats that are acceptable (shown in the
     * "File Format field" of the file chooser). For example,
     * "image files (.jpg, .gif, .png)".
     */
    private String description;

    /** A memory of the last directory this FileChooser was in. */
    private String currentDirectory;

    /** Use the image viewer to preview image files. */
    private boolean useViewer = false;

    /** File separator. */
    private static final String FS = System.getProperty("file.separator");

    /**
     * Creates file chooser dialog.
     *
     * @param currentDirectory Open and save directory
     * @param description the description for the full set of extensions
     */
    public SFileChooser(final String currentDirectory, final String description) {
        this.currentDirectory = currentDirectory;
        this.description = description;
    }

    /**
     * Creates the file chooser dialog.  Use this constructor when
     * only one extension is supported.
     *
     * @param currentDirectory Open and save directory
     * @param description a description of the extension
     * @param extension File type extension for open and save
     */
    public SFileChooser(final String currentDirectory, final String description, final String extension) {
        this.currentDirectory = currentDirectory;
        this.description = description;

        if (description == null) {
            addExtension(extension);
        } else {
            addExtension(description, extension);
        }
    }

    /**
     * Adds an extension with the provided description.
     *
     * @param extension the extension
     * @param description the description
     */
    public void addExtension(final String description, final String extension) {
        exts.put(extension, description);
    }

    /**
     * Adds an extension with the default description.
     *
     * @param extension the extension to add
     */
    public void addExtension(final String extension) {
        addExtension("*." + extension, extension);
    }

    /**
     * Adds the filters for the extensions to the provided chooser.
     *
     * @param chooser the file chooser to add filters to
     * @return filter map
     */
    private Map<String, ExtensionFileFilter> addExtensions(JFileChooser chooser) {
        Map<String, ExtensionFileFilter> filters = new HashMap<String, ExtensionFileFilter>();

        for (Map.Entry<String, String> entry : exts.entrySet()) {
            ExtensionFileFilter filter = new ExtensionFileFilter(
                    entry.getKey(), entry.getValue());
            filters.put(entry.getKey(), filter);
            chooser.addChoosableFileFilter(filter);
        }
        return filters;
    }

    /**
     * Shows dialog for opening files.
     *
     * @return File if selected
     */
    public File showOpenDialog() {
        if (useNativeFileChooser) {
            return showOpenDialogNative();
        } else {
            return showOpenDialogSwing();
        }
    }

    /**
     * Native open dialog.
     *
     * @return file
     */
    private File showOpenDialogNative() {

        FileDialog chooser = new FileDialog(new JFrame(), "Open",
                FileDialog.LOAD);
        chooser.setDirectory(getCurrentLocation());
        if (exts.size() >= 1) {
            chooser.setFilenameFilter(new ExtensionSetFileFilter(exts.keySet(),
                    description));
        }
        chooser.setVisible(true);

        if (chooser.getFile() != null) {
            currentDirectory = chooser.getDirectory();
            return new File(chooser.getDirectory() + FS + chooser.getFile());
        } else {
            // User canceled
            return null;
        }
    }


    /**
     * Swing open dialog.
     *
     * @return file
     */
    private File showOpenDialogSwing() {

        JFileChooser chooser = new JFileChooser();
        setCurrentDirectory(chooser);

        if (exts.size() > 1) {
            chooser.addChoosableFileFilter(new ExtensionSetFileFilter(exts
                    .keySet(), description));
        }

        if (useViewer) {
            ImagePreviewPanel preview = new ImagePreviewPanel();
            chooser.setAccessory(preview);
            chooser.addPropertyChangeListener(preview);
        }

        addExtensions(chooser);

        if (chooser.showDialog(null, "Open") == JFileChooser.APPROVE_OPTION) {
            currentDirectory = chooser.getCurrentDirectory().getPath();
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    /**
     * Sets the current directory for the swing filechooser.
     *
     * @param chooser the file chooser
     */
    private void setCurrentDirectory(final JFileChooser chooser) {
        File dir = new File(currentDirectory);

        try {
            chooser.setCurrentDirectory(dir.getCanonicalFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Shows dialog for saving files.
     *
     * @return Name of file saved
     */
    public File showSaveDialog(final File file) {
        if (useNativeFileChooser) {
            return showSaveDialogNative(file);
        } else {
            return showSaveDialogSwing(file);
        }
    }

    /**
     * Native save dialog.
     *
     * @return Name of file saved
     */
    private File showSaveDialogNative(final File file) {
        FileDialog chooser = new FileDialog(new JFrame(), "Save",
                FileDialog.SAVE);
        chooser.setDirectory(getCurrentLocation());
        if (file != null) {
            if (exts.size() >= 1) {
                chooser.setFile(addExtension(file,
                        new ExtensionSetFileFilter(exts.keySet(), description))
                        .getName());
            } else {
                chooser.setFile(file.getName());
            }
        }
        chooser.setVisible(true);

        if (chooser.getFile() == null) {
            return null;
        }

        File tmpFile = new File(chooser.getDirectory() + FS + chooser.getFile());
        if (tmpFile.exists() && !confirmOverwrite(tmpFile)) {
            return null;
        }
        currentDirectory = chooser.getDirectory();
        return tmpFile;
    }

    /**
     * Swing save dialog.
     *
     * @return Name of file saved
     */
    private File showSaveDialogSwing(final File file) {
        JFileChooser chooser = new JFileChooser();

        setCurrentDirectory(chooser);
        chooser.setAcceptAllFileFilterUsed(false);
        Map<String, ExtensionFileFilter> filters = addExtensions(chooser);

        if (file != null) {
            chooser.setSelectedFile(file);
            String extension = getExtension(file);

            //System.out.println("extension: " + extension);

            if (extension != null) {
                chooser.setFileFilter(filters.get(extension));
            }
        }

        // TODO real parent?
        int result = chooser.showDialog(chooser, "Save");
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File tmpFile = addExtension(chooser.getSelectedFile(), chooser.getFileFilter());
        if (tmpFile.exists() && !confirmOverwrite(tmpFile)) {
            return null;
        }
        currentDirectory = chooser.getCurrentDirectory().getPath();
        return tmpFile;
    }

    /**
     * Ask user whether to overwrite the give existing file.
     *
     * @param file the file in question
     * @return whether the user selected "yes"
     */
    public boolean confirmOverwrite(final File file) {
        String message = "The file \"" + file.getName()
                + "\" already exists. Overwrite?";

        Object[] options = { "OK", "Cancel" };

        return JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(null,
                message, "Warning", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
    }

    /**
     * Shows the save dialog for the given string name.
     *
     * @param file the name of the file
     * @return the file name to save to
     */
    public File showSaveDialog(final String file) {
        return showSaveDialog(new File(file));
    }

    /**
     * Shows the save dialog.
     *
     * @return the selected file
     */
    public File showSaveDialog() {
        return showSaveDialog((File) null);
    }

    /**
     * File-filter.
     */
    private class ExtensionFileFilter extends FileFilter implements FilenameFilter {

        /** Extension to filter. */
        private final String extension;

        /** Human readable description of extension. */
        private final String description;

        /**
         * Construct a file filter.
         *
         * @param extension extension
         * @param description description
         */
        ExtensionFileFilter(final String extension, final String description) {
            this.extension = extension;
            this.description = description;
        }

        /**
         * Determines if the file has the correct extension type.
         *
         * @param file File to be checked
         * @return whether the file has the correct extension type
         */
        public boolean accept(final File file) {
            return file.isDirectory() || hasExtension(file, extension);
        }

        /**
         * @return description of the extension.
         */
        public String getDescription() {
            return description != null ? description : "*." + extension;
        }

        /**
         * Implements file name filter for native file dialog.
         */
        public boolean accept(File dir, String name) {
            return extension.equalsIgnoreCase(getExtension(name));
        }
    }

    /**
     * Filter for a set of extensions.
     *
     * @author Matt Watson
     */
    private class ExtensionSetFileFilter extends FileFilter implements FilenameFilter {

        /** A collection of extension names. */
        private final Collection<String> extensions;

        /** A human readable description for the set of extensions. */
        private final String description;

        /**
         * Construct the file set filter.
         *
         * @param extensions extension
         * @param description description
         */
        ExtensionSetFileFilter(Collection<String> extensions, String description) {
            this.extensions = extensions;
            this.description = description;
        }

        /**
         * Determines if the file has the correct extension type.
         *
         * @param file File to be checked
         * @return whether the file has the correct extension type
         */
        public boolean accept(final File file) {
            return (file.isDirectory()) || extensions.contains(getExtension(file));
        }

        /**
         * @return description of the extension.
         */
        public String getDescription() {
            if (description != null) {
                return description;
            }

            StringBuilder builder = new StringBuilder();

            for (Iterator<String> i = extensions.iterator(); i.hasNext();) {
                builder.append("*.");
                builder.append(i.next());
                if (i.hasNext()) builder.append(", ");
            }

            return builder.toString();
        }

        /**
         * Implements file name filter for native file dialog.
         */
        public boolean accept(File dir, String name) {
            return extensions.contains(getExtension(name));
        }
    }

    /**
     * Returns whether the given file has the given extension
     *
     * @param theFile the file to check
     * @param extension the extension to look for
     * @return whether the given file has the given extension
     */
    private boolean hasExtension(final File theFile, final String extension) {
        return extension.equals(getExtension(theFile));
    }

    /**
     * Returns all the characters after the last period in the file name.
     *
     * @param theFile the file
     * @return all the characters after the last period in the file name
     */
    public static String getExtension(final File theFile) {
        return getExtension(theFile.getName());
    }

    /**
     * Returns all the characters after the last period in the file name.
     *
     * @param fileName the file's name
     * @return all the characters after the last period in the file name
     */
    public static String getExtension(final String fileName) {
        int position = fileName.lastIndexOf('.');
        if (position > 0 && position < fileName.length()) {
            return fileName.substring(position + 1);
        } else {
            return null;
        }
    }

    /**
     * Check to see if the file has the extension, and if not, add it.
     *
     * @param theFile File to add extension to
     * @param extension Extension to add to file
     * @return The file name with the correct extension
     */
    private File addExtension(final File theFile, final FileFilter filter) {
        if (exts.size() < 1) {
            return theFile;
        }

        String extension;

        if (filter instanceof ExtensionFileFilter) {
            extension = ((ExtensionFileFilter) filter).extension;
        } else {
           extension = exts.keySet().iterator().next();
        }

        if (hasExtension(theFile, extension)) {
            return theFile;
        } else {
            // TODO JMW - this seems strange.
            File output = new File(theFile.getAbsolutePath().concat(
                    "." + extension));

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

    /**
     * Sets the file chooser to use image preview viewer.
     *
     * @param useViewer use image preview viewer.
     */
    public void setUseImagePreview(final boolean useViewer) {
        this.useViewer = useViewer;
    }
}
