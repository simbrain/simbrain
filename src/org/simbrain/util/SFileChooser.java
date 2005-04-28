/*
 * Created on Apr 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simbrain.util;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * @author Kyle Baron
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SFileChooser extends JFileChooser{
    private String extensionType;
    private String currentDirectory;
    
    /**
     * Creates file chooser dialog
     * 
     * @param cd 
     * 			Open and save directory
     * @param ext 
     * 			File type extension for open and save
     */
    public SFileChooser(String cd, String ext){
        extensionType = ext;
        currentDirectory = cd;
        addChoosableFileFilter(new fileFilter());
        setCurrentDirectory(new File(currentDirectory));
        //int result = chooser.showDialog(this, "Save");
    }
    
    /**
     * Shows dialog for opening files
     * 
     * @return File if selected
     */
    public File showOpenDialog(){
		int result = showDialog(this, "Open");
		if (result == JFileChooser.APPROVE_OPTION) {
		    currentDirectory = getCurrentDirectory().getPath();
			return getSelectedFile();
		}
		
		return null;
    }
    
    /**
     * Shows dialog for saving files
     * 
     * @return Name of file saved
     */
    public File showSaveDialog(){
        Object[] options = { "OK", "CANCEL" };
        int result = showDialog(this, "Save");
        if (result != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		if (getSelectedFile().exists()) {
		    int ret = JOptionPane.showOptionDialog(null, "The file \"" +
		            getSelectedFile().getName() + "\" already exists. Overwrite?", "Warning",
		            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
		            null, options, options[0]);
		    if(ret == JOptionPane.YES_OPTION) {
		        File tmpFile = getSelectedFile();
		        addExtension(tmpFile, "xml");
				currentDirectory = getCurrentDirectory().getPath();
				return tmpFile;
		    }
		} else {
		    File tmpFile = getSelectedFile();
	        addExtension(tmpFile, "xml");
			currentDirectory = getCurrentDirectory().getPath();
			return tmpFile;
		}
		return null;
    }
    
    
    
	/**
	 * File-filter
	 */
	class fileFilter extends javax.swing.filechooser.FileFilter {
					public boolean accept(File file) {
							String filename = file.getName();
							return (filename.endsWith( "." + extensionType ) || file.isDirectory());
					}
					public String getDescription() {
							return "*." + extensionType ;
					}
	}
	
	/**
	* Check to see if the file has the extension, and if not, add it.
	*
	* @param theFile File to add extension to
	* @param extension Extension to add to file
	*/
	private static void addExtension(File theFile, String extension) {
		if(theFile.getName().endsWith("." + extension)){
		    return;
		}
		theFile.renameTo(new File(theFile.getAbsolutePath().concat("." + extension)));
	}
	
	public String getCurrentLocation(){
	    return currentDirectory;
	}


}
