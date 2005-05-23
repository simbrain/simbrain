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
import org.simbrain.world.WorldFrame;
import org.simbrain.network.UserPreferences;

import javax.swing.JDesktopPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import java.awt.Dimension;
import java.awt.Toolkit;

import java.awt.event.*;

public class Simulation extends JFrame implements ActionListener{

	private JDesktopPane desktop;
	
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
    desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
  }
	
	

	protected JMenuBar createMenuBar() {
			JMenuBar menuBar = new JMenuBar();

			//Set up the lone menu.
			JMenu menu = new JMenu("File");
			menu.setMnemonic(KeyEvent.VK_D);
			menuBar.add(menu);

			//Set up the first  item.
			JMenuItem menuItem = new JMenuItem("New Network");
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
	
		} else if (cmd.equals("quit")) {
				quit();
		}
	}

	//Create a new internal frame.
	protected void createFrame() {
			NetworkFrame network = new NetworkFrame();
			WorldFrame world = new WorldFrame();
			
			network.setWorld(world);
			network.init();
			
			network.setVisible(true); //necessary as of 1.3
			world.setVisible(true);
			
			desktop.add(network);
			desktop.add(world);
			
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
			JFrame.setDefaultLookAndFeelDecorated(true);

			//Create and set up the window.
			Simulation sim = new Simulation();
			sim.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			//Display the window.
			sim.setVisible(true);
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
			UIManager.setLookAndFeel(new MetalLookAndFeel());
			
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
