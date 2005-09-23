package org.simbrain.world.odorworld;

import javax.swing.JMenuItem;

/**
 * 
 * <b>OdorWorldMenu</b>
 */
public class OdorWorldMenu {
	
	private OdorWorld parentWorld;

	public JMenuItem deleteItem = new JMenuItem("Delete object");
	public JMenuItem addItem = new JMenuItem("Add new object");
	public JMenuItem addAgentItem = new JMenuItem("Add new agent"); //TODO: menu with submenus
	public JMenuItem objectPropsItem = new JMenuItem("Set object Properties");
	public JMenuItem propsItem = new JMenuItem("Set world properties");
	public JMenuItem wallItem = new JMenuItem("Draw a wall");
	public JMenuItem wallPropsItem = new JMenuItem("Set Wall Properties");
	public JMenuItem copyItem = new JMenuItem("Copy");
	public JMenuItem cutItem = new JMenuItem("Cut");
	public JMenuItem pasteItem = new JMenuItem("Paste");

	
	public OdorWorldMenu(OdorWorld world){
		parentWorld = world;
	}
	
	
	/**
	 * Build the popup menu displayed when users right-click in world
	 *
	 */
	public void initMenu() {
		
		deleteItem.addActionListener(parentWorld);
		objectPropsItem.addActionListener(parentWorld);
		addItem.addActionListener(parentWorld);
		addAgentItem.addActionListener(parentWorld);
		propsItem.addActionListener(parentWorld);
		wallItem.addActionListener(parentWorld);
		wallPropsItem.addActionListener(parentWorld);
		cutItem.addActionListener(parentWorld);
		copyItem.addActionListener(parentWorld);
		pasteItem.addActionListener(parentWorld);
	}

	
}
