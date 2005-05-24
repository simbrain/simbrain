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

package org.simbrain.simulation;

import org.simbrain.network.NetworkFrame;
import org.simbrain.util.SFileChooser;
import org.simbrain.world.WorldFrame;
import org.simbrain.network.UserPreferences;

import javax.swing.JDesktopPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import java.awt.Dimension;
import java.awt.Toolkit;

import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class Simulation extends JFrame implements ActionListener{

	private JDesktopPane desktop;
	
	private static final String FS = System.getProperty("file.separator");
	// File separator.  For platfrom independence.
	private static final String defaultFile =
		"." + FS + "simulations" + FS + "sims" + FS + "default.sim";
	
	// To be replaced, soon, with lists of networks, worlds, and gauges
	NetworkFrame network = new NetworkFrame();
	WorldFrame world = new WorldFrame();

	public Simulation()
	{
	
		super("Simbrain");

		//Make the big window be indented 50 pixels from each edge
		//of the screen.
		int inset = 50;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(inset, inset,
							screenSize.width  - inset*2,
							screenSize.height - inset*2);

		//Set up the GUI.
		desktop = new JDesktopPane(); //a specialized layered pane
		createFrame(); //create first "window"
		setContentPane(desktop);
		setJMenuBar(createMenuBar());

    //Make dragging a little faster but perhaps uglier.
    //desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
  }
	
	

	protected JMenuBar createMenuBar() {
			JMenuBar menuBar = new JMenuBar();

			//Set up the lone menu.
			JMenu menu = new JMenu("File");
			menu.setMnemonic(KeyEvent.VK_D);
			menuBar.add(menu);

			//Set up the first  item.
			JMenuItem menuItem = new JMenuItem("Open Workspace");
			menuItem.setMnemonic(KeyEvent.VK_O);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_O, ActionEvent.ALT_MASK));
			menuItem.setActionCommand("openWorkspace");
			menuItem.addActionListener(this);
			menu.add(menuItem);
			
			menuItem = new JMenuItem("Save Workspace");
			menuItem.setMnemonic(KeyEvent.VK_S);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_S, ActionEvent.ALT_MASK));
			menuItem.setActionCommand("saveWorkspace");
			menuItem.addActionListener(this);
			menu.add(menuItem);
			
			menuItem = new JMenuItem("New Network");
			menuItem.setMnemonic(KeyEvent.VK_N);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_N, ActionEvent.ALT_MASK));
			menuItem.setActionCommand("newNetwork");
			menuItem.addActionListener(this);
			menu.add(menuItem);
			
			
			menuItem = new JMenuItem("New World");
			menuItem.setMnemonic(KeyEvent.VK_W);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_W, ActionEvent.ALT_MASK));
			menuItem.setActionCommand("newWorld");
			menuItem.addActionListener(this);
			menu.add(menuItem);

			//Set up the second menu item.
			menuItem = new JMenuItem("Quit");
			menuItem.setMnemonic(KeyEvent.VK_Q);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(
							KeyEvent.VK_Q, ActionEvent.ALT_MASK));
			menuItem.setActionCommand("quit");
			menuItem.addActionListener(this);
			menu.add(menuItem);

			return menuBar;
	}

	//React to menu selections.
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand(); 
		
		if (cmd.equals("newNetwork"))
		{
			NetworkFrame network = new NetworkFrame();
			desktop.add(network);
			network.setVisible(true);
		} else if (cmd.equals("newWorld")) {
			WorldFrame world = new WorldFrame();
			desktop.add(world);
			world.setVisible(true);
	
		}  else if (cmd.equals("openWorkspace")) {
			showOpenFileDialog();
	
		}  else if (cmd.equals("saveWorkspace")) {
			showSaveFileDialog();
		} 
		else if (cmd.equals("quit")) {
				quit();
		}
	}

	//Create a new internal frame.
	protected void createFrame() {
			
			desktop.add(network);
			desktop.add(world);

			network.setWorld(world);
			
			network.setVisible(true); //necessary as of 1.3
			world.setVisible(true);
						
			try {
					network.setSelected(true);
			} catch (java.beans.PropertyVetoException e) {}
			
	}

	//Quit the application.
	protected void quit() {
			UserPreferences.saveAll(); // Save all user preferences
			System.exit(0);
	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 */
	private static void createAndShowGUI() {
			//Make sure we have nice window decorations.
			//JFrame.setDefaultLookAndFeelDecorated(true);

			//Create and set up the window.
			Simulation sim = new Simulation();
			sim.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			//Display the window.
			sim.setVisible(true);
	}
	

	//////////////////////////////////////
	// Read and Write Workspace Files  //
	//////////////////////////////////////


	/**
	 * Shows the dialog for opening a simulation file
	 */
	public void showOpenFileDialog() {
	    SFileChooser simulationChooser = new SFileChooser("." + FS 
	        	        + "simulations"+ FS + "sims", "sim");
		File simFile = simulationChooser.showOpenDialog();
		if(simFile != null){
		    readSim(simFile);
		    repaint();
		}
	}

	/**
	 * Shows the dialog for saving a simulation file
	 */
	public void showSaveFileDialog(){
	    SFileChooser simulationChooser = new SFileChooser("." + FS 
    	        + "simulations"+ FS + "sims", "sim");
	    File simFile = simulationChooser.showSaveDialog();
	    if(simFile != null){
	        writeSim(simFile);
	    }
	}

	/**
	 * Reads in a simulation file, which is essentially two or three lines,
	 * containing the names of a network, a world, and a gauge file, respectively.  The gauge
	 * file can be omitted.  This method calls the read methods in the network, world, and gauge 
	 * packages.
	 * 
	 * @param theFile the simulation file to be read
	 */
	public void readSim(File theFile) {
		FileInputStream f = null;
		String line = null;
		try {
			f = new FileInputStream(theFile);
		}catch (java.io.FileNotFoundException e) {
		    JOptionPane.showMessageDialog(null, "Could not read simulation file \n"
			        + f, "Warning", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();       
		    return;
		} catch (NullPointerException e){
		    JOptionPane.showMessageDialog(null, "Could not find simulation file \n"
			        + f, "Warning", JOptionPane.ERROR_MESSAGE);
		    return;
		}
		catch (Exception e){
		    e.printStackTrace();
		    return;
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(f));

		if (f == null) {
			return;
		}

		String localDir = new String(System.getProperty("user.dir"));		
		
		//Read in network file
		try {
			line = br.readLine();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("br.readLine");
		}

		line.replace('/', FS.charAt(0));	// For windows machines..
	    File netFile = new File(localDir + line);
		network.getNetPanel().open(netFile);

		//Read in world file
		try {
			line = br.readLine();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("br.readLine");
		}
		
		line.replace('/', FS.charAt(0));	// For windows machines..	
		File worldFile = new File(localDir + line);
		world.readWorld(worldFile);

		// Gauge files not currently dealt with
		//		do {
		//			try {
		//				line = br.readLine();
		//			} catch (Exception e) {
		//				e.printStackTrace();
		//			}
		//			if (line != null) {
		//				line.replace('/', FS.charAt(0));	// For windows machines..	
		//				File gaugeFile = new File(localDir + line);
		//				netPanel.addGauge(gaugeFile);
		//				
		//			}
		//		} while(line != null);
		
	}

	/**
	 * Writes a simulation file which contains two lines containing the names of a network, 
	 * and a world file.  Gauge files are not currently written.
	 * This method calls the write methods in the network and world packages
	 * 
	 * @param simFile The file to be written to
	 */
	public void writeSim(File simFile) {
		
		FileOutputStream f = null;

		try {
			f = new FileOutputStream(simFile);
		} catch (Exception e) {
			System.out.println("Could not open file stream: " + e.toString());
		}

		if (f == null) {
			return;
		}

		PrintStream ps = new PrintStream(f);
		String localDir = new String(System.getProperty("user.dir"));

		// Get relative path for network file
		String absoluteNetPath = network.getNetPanel().getCurrentFile().getAbsolutePath();
		String relativeNetPath = getRelativePath(localDir, absoluteNetPath);
		//Save network file
		ps.println("" + relativeNetPath);

		// Get relative path for world file
		String absoluteWldPath = world.getCurrentFile().getAbsolutePath();
		String relativeWldPath = getRelativePath(localDir, absoluteWldPath);
		//Save world file		
		ps.println("" + relativeWldPath);
		
		ps.close();
		//System.gc();
				
		// Note Gauge data not currently saved
		

	}

	/**
	 * Helper method to create a relative path for use in saving simulation files
	 * which refer to files within directories.   Substracts the absolutePath of 
	 * the local user directory from the absolute path of the file to be saved,
	 * and converts  file-separators into forward slashes, which are used for saving
	 * simualtion files. 
	 * 
	 * @param baseDir absolute path of the local simbrain directory.
	 * @param absolutePath the absolute path of the file to be saved
	 * @return the relative path from the local directory to the file to be saved
	 */
	public static String getRelativePath(String baseDir, String absolutePath) {
		
		int localLength =  baseDir.length();
		int totalLength = absolutePath.length();
		int diff = totalLength - localLength;
		String relativePath = absolutePath.substring(totalLength - diff);
		relativePath = relativePath.replaceAll("/./", "/");
		relativePath.replace('/', FS.charAt(0));	// For windows machines..	

		return relativePath;
	}
	
	////////////////////////////
	// Main method		      //
	///////////////////////////

	/**
	 * Simbrain main method.  Creates a single instance of the Simulation class
	 * 
	 * @param args currently not used
	 */
	public static void main(String[] args) {
		try {
			//UIManager.setLookAndFeel(new MetalLookAndFeel());
			
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
						createAndShowGUI();
				}
			});
		} catch (Exception e) {
			System.err.println("Couldn't set look and feel!");
		}
	}
	
	

}
