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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFileChooser;
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
    /** default serial version id */
    private static final long serialVersionUID = 1L;
    /** 
     * the map of extension and their descriptions in the order
     * of their addition
     */
    private final LinkedHashMap<String, String> exts = new LinkedHashMap<String, String>();
    /** the description for the entire set of formats */
    private String description;
    
    /** A memory of the last directory this FileChooser was in. */
    private String currentDirectory;

    /** Use the image viewer to preview image files. */
    private boolean useViewer = false;

    /**
     * Creates file chooser dialog.
     * 
     * @param currentDirectory Open and save directory
     * @param ext File type extension for open and save
     */
    public SFileChooser(final String currentDirectory, final String description) {
        this.currentDirectory = currentDirectory;
        this.description = description;
    }

    /**
     * adds an extension with the provided description
     * 
     * @param extension the extension
     * @param description the description
     */
    public void addExtension(String extension, String description) {
        exts.put(extension, description);
    }
    
    /**
     * Adds an extension with the default description
     * 
     * @param extension the extension to add
     */
    public void addExtension(String extension) {
        addExtension(extension, "*." + extension);
    }
    
    /**
     * Adds the filters for the extensions to the provided chooser
     * 
     * @param chooser the file chooser to add filters to
     */
    private Map<String, ExtensionFileFilter> addExtensions(JFileChooser chooser) {
        Map<String, ExtensionFileFilter> filters = new HashMap<String, ExtensionFileFilter>();
        
        for (Map.Entry<String, String> entry : exts.entrySet()) {
            ExtensionFileFilter filter = new ExtensionFileFilter(entry.getKey(), entry.getValue());
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
        JFileChooser chooser = new JFileChooser();
        
        setCurrentDirectory(chooser);

        if (exts.size() > 1) {
            chooser.addChoosableFileFilter(new ExtensionSetFileFilter(exts.keySet(), description));
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
     * Sets the current directory for the chooser
     * 
     * @param chooser the file chooser
     */
    private void setCurrentDirectory(JFileChooser chooser) {
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
    public File showSaveDialog(File file) {
        JFileChooser chooser = new JFileChooser();
        
        setCurrentDirectory(chooser);
        chooser.setAcceptAllFileFilterUsed(false);
        Map<String, ExtensionFileFilter> filters = addExtensions(chooser);
        
        if (file != null) {
            chooser.setSelectedFile(file);
            String extension = getExtension(file);
            
            System.out.println("extension: " + extension);
            
            if (extension != null) chooser.setFileFilter(filters.get(extension));
        }
        
        // TODO real parent?
        int result = chooser.showDialog(chooser, "Save");

        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File tmpFile = addExtension(chooser.getSelectedFile(), chooser);
        
        if (tmpFile.exists() && !confirmOverwrite(tmpFile)) return null;
        
        currentDirectory = chooser.getCurrentDirectory().getPath();

        return tmpFile;
    }
    
    /**
     * ASk user whether to overwrite the give existing file
     * 
     * @param file the file in question
     * @return whether the user selected "yes"
     */
    public boolean confirmOverwrite(File file) {
        String message = "The file \"" + file.getName() + "\" already exists. Overwrite?";
        Object[] options = { "OK", "Cancel" };
        
        return JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(null, message, 
            "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
            null, options, options[0]);
    }
    
    /**
     * Shows the save dialog for the given string name
     * 
     * @param file the name of the file
     * @return the file name to save to
     */
    public File showSaveDialog(String file) {
        return showSaveDialog(new File(file));
    }
    
    /**
     * Shows the save dialog
     * 
     * @return the selected file
     */
    public File showSaveDialog() {
        return showSaveDialog((File) null);
    }
    
    /**
     * File-filter.
     */
    private class ExtensionFileFilter extends FileFilter {
        private final String extension;
        private final String description;
        
        ExtensionFileFilter(String extension, String description) {
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
    }

    /**
     * Filter for a set of extensions.
     * 
     * @author Matt Watson
     */
    private class ExtensionSetFileFilter extends FileFilter {
        private final Collection<String> extensions;
        private final String description;
        
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
            if (description != null) return description;
            
            StringBuilder builder = new StringBuilder();
            
            for (Iterator<String> i = extensions.iterator(); i.hasNext();) {
                builder.append("*.");
                builder.append(i.next());
                if (i.hasNext()) builder.append(", ");
            }
            
            return builder.toString();
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
     * returns all the characters after the last period in the file name
     * 
     * @param theFile the file
     * @return all the characters after the last period in the file name
     */
    private String getExtension(final File theFile) {
        String fileName = theFile.getName();
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
    private File addExtension(final File theFile, final JFileChooser chooser) {
        if (exts.size() < 1) return theFile;
        
        FileFilter selected = chooser.getFileFilter();
        
        String extension;
        
        if (selected instanceof ExtensionFileFilter) {
            extension = ((ExtensionFileFilter) selected).extension;
        } else {
           extension = exts.keySet().iterator().next();
        }
        
        if (hasExtension(theFile, extension)) {
            return theFile;
        } else {
            // TODO JMW - this seems strange.
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

    /**
     * Sets the file chooser to use image preview viewer.
     *
     * @param useViewer use image preview viewer 
     */
    public void setUseImagePreview(boolean useViewer) {
        this.useViewer = useViewer;
    }
}
