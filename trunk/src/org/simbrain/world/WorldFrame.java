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

package org.simbrain.world;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.simbrain.simulation.Simulation;

/**
 * <b>WorldPanel</b> is the container for the world component.  
 * Handles toolbar buttons, and serializing of world data.  The main
 * environment codes is in {@link World}.
 */
public class WorldFrame extends JFrame implements ActionListener {

	private static final String FS = Simulation.getFileSeparator();
	private static File current_file = null;
	private JScrollPane worldScroller = new JScrollPane();
	private World world;
	JMenuBar mb = new JMenuBar();
	JMenu fileMenu = new JMenu("File  ");
	JMenuItem saveItem = new JMenuItem("Save");
	JMenuItem saveAsItem = new JMenuItem("Save As");
	JMenuItem openItem = new JMenuItem("Open world");
	JMenuItem prefsItem = new JMenuItem("World preferences");
	
	JMenu scriptMenu = new JMenu("Script ");
	JMenuItem scriptItem = new JMenuItem("Open script dialog");
	
	/**
	 * Construct a new world panel.  Set up the toolbars.  Create an 
	 * instance of a world object.
	 */
	public WorldFrame() { 
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add("Center", worldScroller);
		setBounds(505, 35, 400, 400);
		world = new World();
		world.setPreferredSize(new Dimension(700, 700));
		worldScroller.setViewportView(world);
		worldScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		worldScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		setJMenuBar(mb);
		mb.add(fileMenu);
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.add(saveAsItem);
		fileMenu.addSeparator();
		fileMenu.add(prefsItem);
		mb.add(scriptMenu);
		scriptMenu.add(scriptItem);
		saveItem.addActionListener(this);
		saveAsItem.addActionListener(this);
		openItem.addActionListener(this);
		prefsItem.addActionListener(this);
		scriptItem.addActionListener(this);
		
		setVisible(true);
	}

	public File getCurrentFile() {
		return current_file;
	}
	public World getWorldRef() {
		return world;
	}

	/**
	 * Show the dialog for choosing a world to open
	 */
	public void openWorld() {

		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new xmlFilter());
		chooser.setCurrentDirectory(
			new File("." + FS + "simulations" + FS + "worlds"));
		int result = chooser.showDialog(this, "Open");
		if (result == JFileChooser.APPROVE_OPTION) {
			readWorld(chooser.getSelectedFile());
		}
	}

	/**
	 * Creates a world based on a .wld file
	 * 
	 * @param file the world file to be read
	 */	
	public void readWorld(File theFile) {
		
		setTitle("" + theFile.getName());		
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setValidating(false);
		spf.setNamespaceAware(true);
		try {
			
			// parse the document
			SAXParser parser = spf.newSAXParser();
			WorldFileReader handler = new WorldFileReader();
			parser.parse(theFile, handler);
			world.setObjectList(handler.getEntityList());
			current_file = theFile;

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Could not find world file \n" + theFile, "Warning", JOptionPane.ERROR_MESSAGE);
			return;
		}
		world.repaint();
	}

	/**
	 * File-filter for xml files
	 */
	class xmlFilter extends javax.swing.filechooser.FileFilter {
					public boolean accept(File file) {
							String filename = file.getName();
							return (filename.endsWith( ".xml" ) || file.isDirectory());
					}
					public String getDescription() {
							return "*.xml" ;
					}
	}
	
	/**
	* Check to see if the file has the extension, and if not, add it.
	*
	* @param theFile file to add extension to
	* @param extension extension to add to file
	*/
	private static void addExtension(File theFile, String extension) {
		if(theFile.getName().endsWith("." + extension)) return;
		theFile.renameTo(new File(theFile.getAbsolutePath().concat("." + extension)));
	}


	/**
	 * Opens a file-save dialog and saves world information to the specified file
	 */
	public void saveWorld() {
		String line = null;
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(
			new File("." + FS + "simulations" + FS + "worlds"));
		int result = chooser.showDialog(this, "Save");
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}
		saveWorld(chooser.getSelectedFile());
	}

	//TODO: Docs
	public void saveWorld(File worldFile) {
		try {
			FileOutputStream f = new FileOutputStream(worldFile);
			WorldFileWriter.write(f, world );
		} catch (Exception e) {
			System.out.println("Could not open file stream: " + e.toString());
		}
		addExtension(worldFile, "xml");
		setTitle("" + worldFile.getName());	
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		Object e1 = e.getSource();
		
		if(e1 == openItem) {
			openWorld();
		} else if (e1 == saveItem) {
			saveWorld(current_file);
		} else if (e1 == saveAsItem) {
			saveWorld();
		} else if (e1 == prefsItem) {
			world.showGeneralDialog();
		} else if (e1 == scriptItem) {
			world.showScriptDialog();
		}
		
	}
}
	
	
