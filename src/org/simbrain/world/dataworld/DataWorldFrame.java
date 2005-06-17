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

package org.simbrain.world.dataworld;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.util.LocalConfiguration;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;
import org.simbrain.world.odorworld.OdorWorld;

/**
 * <b>WorldPanel</b> is the container for the world component.  
 * Handles toolbar buttons, and serializing of world data.  The main
 * environment codes is in {@link OdorWorld}.
 */
public class DataWorldFrame extends JInternalFrame implements InternalFrameListener {

	private static final String FS = "/"; //System.getProperty("file.separator");Separator();
	private File current_file = null;
	private String currentDirectory = "." + FS + "simulations" + FS + "worlds";
	private JScrollPane worldScroller = new JScrollPane();
	private Workspace workspace;
	private DataWorld world;
	
	// For workspace persistence 
	private String path;
	private int xpos;
	private int ypos;
	private int the_width;
	private int the_height;
	
	public DataWorldFrame() {
	}
	
	/**
	 * Construct a new world panel.  Set up the toolbars.  Create an 
	 * instance of a world object.
	 */
	public DataWorldFrame(Workspace ws) { 
		
		workspace = ws;
		init();
	}
	
	public void init() {
		
		this.setResizable(true);
		this.setMaximizable(true);
		this.setIconifiable(true);
		this.setClosable(true);	
		this.addInternalFrameListener(this);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add("Center", worldScroller);
		world = new DataWorld();
		world.addMenuBar(this,world);
		worldScroller.setViewportView(world);
		worldScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		worldScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		worldScroller.setEnabled(false);
		
		setVisible(true);
		
	}

	public File getCurrentFile() {
		return current_file;
	}

	public DataWorld getWorld() {
		return world;
	}

	/**
	 * Show the dialog for choosing a world to open
	 */
	public void openWorld() {
		SFileChooser chooser = new SFileChooser(currentDirectory, "xml");
		File theFile = chooser.showOpenDialog();
		if (theFile != null) {
			readWorld(theFile);
			currentDirectory = chooser.getCurrentLocation();
		}
	}


	/**
	 * Read a world from a world-xml file.
	 * 
	 * @param theFile the xml file containing world information
	 */
	public void readWorld(File theFile) {
		
		current_file = theFile;
		try {
			Reader reader = new FileReader(theFile);
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "world_mapping.xml");
			Unmarshaller unmarshaller = new Unmarshaller(world);
			unmarshaller.setMapping(map);
			//unmarshaller.setDebug(true);
			world = (DataWorld) unmarshaller.unmarshal(reader);
			world.setParentFrame(this);
		} catch (java.io.FileNotFoundException e) {
		    JOptionPane.showMessageDialog(null, "Could not find network file \n"
			        + theFile, "Warning", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();       
		    return;
		} catch (Exception e){
		    JOptionPane.showMessageDialog(null, "There was a problem opening file \n"
			        + theFile, "Warning", JOptionPane.ERROR_MESSAGE);
		    e.printStackTrace();
		    return;
		}
		getWorkspace().attachAgentsToCouplings();
		setWorldName(theFile.getName());

		//Set Path; used in workspace persistence
		String localDir = new String(System.getProperty("user.dir"));		
		setPath(Utils.getRelativePath(localDir, theFile.getAbsolutePath()));		
	}
	
	/**
	 * Opens a file-save dialog and saves world information to the specified file
	 * 
	 * Called by "Save As"
	 */
	public void saveWorld() {
		SFileChooser chooser = new SFileChooser(currentDirectory, "xml");
		File worldFile = chooser.showSaveDialog();
		if (worldFile != null){
		    saveWorld(worldFile);
		    current_file = worldFile;
		    currentDirectory = chooser.getCurrentLocation();
		}

	}

	/**
	 * Save a specified file
	 * 
	 * Called by "save"
	 * 
	 * @param worldFile
	 */
	public void saveWorld(File worldFile) {
		
		current_file = worldFile;
		
		LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");

		try {
			FileWriter writer = new FileWriter(worldFile);
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "world_mapping.xml");
			Marshaller marshaller = new Marshaller(writer);
			marshaller.setMapping(map);
			//marshaller.setDebug(true);
			marshaller.marshal(world);
			

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		setWorldName("" + worldFile.getName());	
	}
	
	
	public void internalFrameOpened(InternalFrameEvent e){
	}
	
	public void internalFrameClosing(InternalFrameEvent e){
	}

	public void internalFrameClosed(InternalFrameEvent e){
		clearWorkspace();
	}
	
	public void internalFrameIconified(InternalFrameEvent e){
	}

	public void internalFrameDeiconified(InternalFrameEvent e){
	}
	
	public void internalFrameActivated(InternalFrameEvent e){
	}

	public void internalFrameDeactivated(InternalFrameEvent e){
	}
	
	public void clearWorkspace() {
		this.getWorkspace().getWorldList().remove(this);
	}
	
	/**
	 * @param path The path to set; used in persistence.
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
	/**
	 * 
	 * @return path information; used in persistence
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * 
	 * @return platform-specific path
	 */
	public String getGenericPath() {
		String ret =  path;
		if (path == null) {
			return null;
		}
		ret.replace('/', System.getProperty("file.separator").charAt(0));
		return ret;
	}
	
	/**
	 * @return Returns the workspace.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}
	
	/**
	 * @param workspace The workspace to set.
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}
	
	/**
	 * For Castor.  Turn Component bounds into separate variables.  
	 */
	public void initBounds() {
		xpos = this.getX();
		ypos = this.getY();
		the_width = this.getBounds().width;
		the_height = this.getBounds().height;
	}
	
	/**
	 * @return Returns the xpos.
	 */
	public int getXpos() {
		return xpos;
	}
	/**
	 * @param xpos The xpos to set.
	 */
	public void setXpos(int xpos) {
		this.xpos = xpos;
	}
	/**
	 * @return Returns the ypos.
	 */
	public int getYpos() {
		return ypos;
	}
	/**
	 * @param ypos The ypos to set.
	 */
	public void setYpos(int ypos) {
		this.ypos = ypos;
	}


	/**
	 * @return Returns the the_height.
	 */
	public int getThe_height() {
		return the_height;
	}
	/**
	 * @param the_height The the_height to set.
	 */
	public void setThe_height(int the_height) {
		this.the_height = the_height;
	}
	/**
	 * @return Returns the the_width.
	 */
	public int getThe_width() {
		return the_width;
	}
	/**
	 * @param the_width The the_width to set.
	 */
	public void setThe_width(int the_width) {
		this.the_width = the_width;
	}
	
	public void setWorldName(String name) {
		setTitle(name);		
		world.setName(name);
		
	}
}