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

package org.simbrain.network;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.hisee.core.Gauge;
import org.simbrain.network.NetworkPanel;
import org.simbrain.network.UserPreferences;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.*;
import org.simbrain.world.World;
import org.simbrain.world.WorldFrame;

import calpa.html.CalHTMLPane;
import calpa.html.CalHTMLPreferences;

/**
 * This is the top-level container for the simulation environment.
 * It contains a neural network (a {@link NetworkPanel}), a simulated environment
 * (a {@link WorldFrame}), and a set of <a href ="http://hisee.sourceforge.net/">hisee</a> gauges.
 */
public class NetworkFrame
	extends JInternalFrame
	implements ActionListener, MenuListener {

	private static final String FS = System.getProperty("file.separator");
	// File separator.  For platfrom independence.
	private static final String defaultFile =
		"." + FS + "simulations" + FS + "sims" + FS + "default.sim";

	private NetworkPanel netPanel = new NetworkPanel(this);
	/** the network component */
	private WorldFrame worldFrame;
	/** the world component */
	private World theWorld;
	/** reference to the world object */

	private JButton openBtn =
		new JButton(ResourceManager.getImageIcon("Open.gif"));
	private JButton saveBtn =
		new JButton(ResourceManager.getImageIcon("Save.gif"));
	private JButton prefsBtn =
		new JButton(ResourceManager.getImageIcon("Prefs.gif"));
	private JToolBar theToolBar = new JToolBar();
	

	JMenuBar mb = new JMenuBar();
	JMenu simMenu = new JMenu("Network");
	JMenuItem saveItem = new JMenuItem("Save Network");
	JMenuItem openItem = new JMenuItem("Open Network");
	JMenuItem quitItem = new JMenuItem("Quit");
	JMenu netMenu = new JMenu("Network  ");
	JMenuItem newNetSubmenu = new JMenu("New");
	JMenuItem newWTAItem = new JMenuItem("Winner take all network");
	JMenuItem newHopfieldItem = new JMenuItem("Hopfield network");
	JMenuItem newBackpropItem = new JMenuItem("Backprop network");
	JMenuItem openNetItem = new JMenuItem("Open");
	JMenuItem placeItem = new JMenuItem("Place network");
	JMenuItem saveNetItem = new JMenuItem("Save");
	JMenuItem saveAsItem = new JMenuItem("Save As");
	JMenuItem copyItem = new JMenuItem("Copy Selection");
	JMenuItem pasteItem = new JMenuItem("Paste Selection");
	JMenuItem setNeuronItem = new JMenuItem("Set Neuron(s)");
	JMenuItem setWeightItem = new JMenuItem("Set Weight(s)");
	JMenuItem selectAll = new JMenuItem("Select All");
	JMenuItem alignSubmenu = new JMenu("Align");
	JMenuItem alignHorizontal = new JMenuItem("Horizontal");
	JMenuItem alignVertical = new JMenuItem("Vertical");
	JMenuItem spacingSubmenu = new JMenu("Spacing");
	JMenuItem spacingHorizontal = new JMenuItem("Horizontal");
	JMenuItem spacingVertical = new JMenuItem("Vertical");
	JMenuItem setInOutItem = new JCheckBoxMenuItem("Show I/O Info", false);	
	JMenuItem setAutozoom = new JCheckBoxMenuItem("Autozoom", true);	
	JMenuItem prefsItem = new JMenuItem("Preferences");
	JMenu gaugeMenu = new JMenu("Gauges  ");
	JMenuItem addGaugeItem = new JMenuItem("Add Gauge");
	JMenu gaugeSubmenu = new JMenu("Set Gauges");
	JMenu helpMenu = new JMenu("Help");
	JMenuItem quickRefItem = new JMenuItem("Simbrain Help");

	/**
	 * Creates the (single) simulation object.  Performs basic setup.  Creates a network and a world
	 * object and passes refereces of each to the other.
	 */
	public NetworkFrame() {

		super("Simbrain");	

		// Basic setup        
		setUpMenus();
		this.getContentPane().add("Center", netPanel);
		this.setBounds(5, 35, 450, 450);

		//Set up gauges
		setGauges();			
	}
	
	public void init()
	{
		// Read default simulation files (?silly?)
		readSim(new File(defaultFile));
		netPanel.repaint();
	}
	
	public void setWorld(WorldFrame wf)
	{
		this.worldFrame = wf;

		//Set up world component
		worldFrame.setNetworkPanel(netPanel);
		netPanel.setWorld(worldFrame.getWorldRef());
		worldFrame.repaint();		
	}

	/**
	 * Sets up the main menu bar
	 */
	private void setUpMenus() {
		this.setJMenuBar(mb);
		mb.add(simMenu);
		simMenu.add(openItem);
		simMenu.add(saveItem);
		simMenu.addSeparator();
		simMenu.add(quitItem);
		mb.add(netMenu);
		netMenu.add(newNetSubmenu);
		newNetSubmenu.add(newWTAItem);
		newNetSubmenu.add(newHopfieldItem);
		newNetSubmenu.add(newBackpropItem);
		netMenu.addSeparator();
		openNetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		netMenu.add(openNetItem);
		saveNetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		netMenu.add(saveNetItem);
		netMenu.add(saveAsItem);
		netMenu.add(placeItem);
		netMenu.addSeparator();
		copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		netMenu.add(copyItem);
		pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		netMenu.add(pasteItem);
		netMenu.addSeparator();
		selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		netMenu.add(selectAll);
		netMenu.addSeparator();
		netMenu.add(alignSubmenu);
		alignSubmenu.add(alignHorizontal);
		alignSubmenu.add(alignVertical);
		netMenu.add(spacingSubmenu);
		spacingSubmenu.add(spacingHorizontal);
		spacingSubmenu.add(spacingVertical);
		netMenu.addSeparator();
		netMenu.add(setNeuronItem);
		netMenu.add(setWeightItem);
		netMenu.addSeparator();
		netMenu.add(setInOutItem);
		netMenu.add(setAutozoom);
		netMenu.addSeparator();
		netMenu.add(prefsItem);
		netMenu.addMenuListener(this);
		mb.add(gaugeMenu);
		gaugeMenu.add(addGaugeItem);
		gaugeMenu.add(gaugeSubmenu);
		gaugeMenu.addMenuListener(this);
		mb.add(helpMenu);
		quickRefItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		helpMenu.add(quickRefItem);
					
		saveItem.addActionListener(this);
		openItem.addActionListener(this);
		placeItem.addActionListener(this);
		openNetItem.addActionListener(this);
		saveAsItem.addActionListener(this);
		saveNetItem.addActionListener(this);
		selectAll.addActionListener(this);
		prefsItem.addActionListener(this);
		setNeuronItem.addActionListener(this);
		setWeightItem.addActionListener(this);
		setInOutItem.addActionListener(this);
		setAutozoom.addActionListener(this);
		prefsItem.addActionListener(this);
		addGaugeItem.addActionListener(this);
		newWTAItem.addActionListener(this);
		newHopfieldItem.addActionListener(this);
		newBackpropItem.addActionListener(this);
		copyItem.addActionListener(this);
		pasteItem.addActionListener(this);
		alignHorizontal.addActionListener(this);
		alignVertical.addActionListener(this);
		spacingHorizontal.addActionListener(this);
		spacingVertical.addActionListener(this);
		quickRefItem.addActionListener(this);
		quitItem.addActionListener(this);

				
	}

	public static String getFileSeparator() {

		return FS;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if(e.getSource().getClass() == JButton.class)
		{
	
			JButton b = (JButton) e.getSource();

			if ( b == openBtn) {
				showOpenFileDialog();
			} else if (b == saveBtn) {
				showSaveFileDialog();
			} 

		} else if( (e.getSource().getClass() == JMenuItem.class) || (e.getSource().getClass() == JCheckBoxMenuItem.class) ) {

			JMenuItem jmi = (JMenuItem) e.getSource();
			
			if(jmi == saveItem)  {
				showSaveFileDialog();
			} else if(jmi == openItem)  {
				showOpenFileDialog();
			} else if(jmi == placeItem)  {
				netPanel.getSerializer().showPlaceFileDialog();
			} else if(jmi == openNetItem)  {
				netPanel.open();
			} else if(jmi == saveAsItem)  {
				netPanel.saveAs();
			} else if(jmi == saveNetItem)  {
				netPanel.save();
			} else if(jmi == selectAll)  {
				netPanel.getHandle().selectAll();
			} else if(jmi == prefsItem)  {
			//netPanel.showPrefs();
			} else if(jmi == setNeuronItem)  {
				netPanel.showNeuronPrefs();
			} else if(jmi == setWeightItem)  {
				netPanel.showWeightPrefs();
			} else if(jmi == setInOutItem)  {
				netPanel.showInOut(setInOutItem.isSelected());
			} else if(jmi == setAutozoom)  {
				netPanel.setAutoZoom(setAutozoom.isSelected());
				netPanel.repaint();
			} else if(jmi == prefsItem)  {
				netPanel.showNetworkPrefs();
			} else if(jmi == addGaugeItem)  {
				netPanel.addGauge();
			} else if(jmi == newWTAItem)  {
				netPanel.showWTADialog();
			} else if(jmi == newHopfieldItem)  {
				netPanel.showHopfieldDialog();
			} else if(jmi == newBackpropItem)  {
				netPanel.showBackpropDialog();
			} else if(jmi == copyItem)  {
				netPanel.getHandle().copyToClipboard();
			} else if(jmi == pasteItem)  {
				netPanel.getHandle().pasteFromClipboard();
			} else if(jmi == alignHorizontal)  {
				netPanel.alignHorizontal();
			} else if(jmi == alignVertical)  {
				netPanel.alignVertical();
			} else if(jmi == spacingHorizontal)  {
				netPanel.spacingHorizontal();
			} else if(jmi == spacingVertical)  {
				netPanel.spacingVertical();
			} else if(jmi == quickRefItem)  {
				showQuickRef();
			} else if(jmi == quitItem)  {
				System.exit(1);
			} 
		}
		
		
	}

	/**
	 * Shows the quick reference guide in the help menu.  The quick reference
	 * is an html page in the Simbrain/doc directory
	 */
	public void showQuickRef() {
		
		URL url = null;
		try {
		   url = new URL("file:" + System.getProperty("user.dir") 
		           + FS + "docs" + FS + "SimbrainDocs.html");
		} catch (java.net.MalformedURLException e) {
		   System.err.println("Malformed URL");
		   return;
		}
		
		JFrame f = new JFrame();

		//create a Preferences object
		CalHTMLPreferences pref = new CalHTMLPreferences();

		//use one of its methods to enable the test navbar
		pref.setShowTestNavBar(true);

		//now pass the pref object to the Pane's constructor
		CalHTMLPane pane = new CalHTMLPane(pref, null, null);
		f.getContentPane().add(pane, "Center");
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		f.setSize(new Dimension(Math.min(d.width - 10, 800), Math.min(d.height - 40, 600)));
		f.setVisible(true);
		if (url != null) {
		   pane.showHTMLDocument(url);
		}

	 
	}

	//////////////////////////////////////
	// Read and Write NetworkFrame Files  //
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
		netPanel.open(netFile);

		//Read in world file
		try {
			line = br.readLine();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("br.readLine");
		}
		
		line.replace('/', FS.charAt(0));	// For windows machines..	
		File worldFile = new File(localDir + line);
		worldFrame.readWorld(worldFile);

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
		String absoluteNetPath = netPanel.getCurrentFile().getAbsolutePath();
		String relativeNetPath = getRelativePath(localDir, absoluteNetPath);
		//Save network file
		ps.println("" + relativeNetPath);

		// Get relative path for world file
		String absoluteWldPath = worldFrame.getCurrentFile().getAbsolutePath();
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
	// Menu Even      //
	////////////////////////////

	public void menuCanceled(MenuEvent e) {
	}

	public void menuDeselected(MenuEvent e) {
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.MenuListener#menuSelected(javax.swing.event.MenuEvent)
	 */
	public void menuSelected(MenuEvent e) {

		setGauges(); // Populate gauge sub-menu

		// Handle set-neuron and set-weight menu-items.
		int num_neurons = netPanel.getSelectedNeurons().size();
		if (num_neurons > 0) {
			setNeuronItem.setText(
				"Set "
					+ num_neurons
					+ (num_neurons > 1
						? " Selected Neurons"
						: " Selected Neuron"));
			setNeuronItem.setEnabled(true);
		} else {
			setNeuronItem.setText("Set Selected Neuron(s)");
			setNeuronItem.setEnabled(false);
		}
		int num_weights = netPanel.getSelectedWeights().size();
		if (num_weights > 0) {
			setWeightItem.setText(
				"Set "
					+ num_weights
					+ (num_weights > 1
						? " Selected Weights"
						: " Selected Weight"));
			setWeightItem.setEnabled(true);
		} else {
			setWeightItem.setText("Set Selected Weight(s)");
			setWeightItem.setEnabled(false);
		}

	}

	////////////////////////////
	// Gauge stuff            //
	////////////////////////////
	
	/**
	 * Populate the "set gauges" submenu
	 */
	public void setGauges() {
		ArrayList gauges = netPanel.getGauges();
		gaugeSubmenu.removeAll();
		for (int i = 0; i < gauges.size(); i++) {
			Gauge theGauge = (Gauge) gauges.get(i);
			JMenuItem mi = new JMenuItem("" + theGauge.getName());
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JMenuItem mi = (JMenuItem) e.getSource();
					String text = mi.getText();
					String last =
						text.substring(text.length() - 1, text.length());
					int gaugeNum = (Integer.parseInt(last)) - 1;
					// TODO: Remove this dependence on the menuitem text!
					updateGauge(gaugeNum);
				}
			});

			gaugeSubmenu.add(mi);
		}

	}

	/**
	 * Forwards to sim.network.NetworkPanel.updateGauge
	 * 
	 * @param num the number of the gauge to update. 
	 * @see NetworkPanel#updateGauge(int)
	 */
	public void updateGauge(int num) {
		netPanel.updateGauge(num);
	}

	////////////////////////////
	// Main method		      //
	///////////////////////////

	/**
	 * Simbrain main method.  Creates a single instance of the NetworkFrame class
	 * 
	 * @param args currently not used
	 */
	public static void main(String[] args) {
		
		NetworkFrame theSim = new NetworkFrame();

	}
	
	

}
