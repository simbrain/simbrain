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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.util.SimbrainMath;


/**
 * <b>World</b> is the lowest-level environment panel which contains most
 * of the world's "logic". Creature and flower/food icons are drawn here.
 * Movement of the mouse in response to clicks and (very 
 * minimal) world-editing features are also handled here.  
 * Finally, the stimulus to the network is  calculated here, on the 
 * basis of the creature's distance from objects, as follows:
 *  
 *  <li> Get the vector of values, the "smell signature," associated with each object </li>
 *  <li> Scale this signature by the creature's distance fromm each object.</li>
 *  <li> Use the sum of these scaled smell signatures as input to the creature's network. </li>
 *  
 */
public class World extends JPanel implements MouseListener, MouseMotionListener, ActionListener, KeyListener {

	/** Color of the world background */
	private Color backgroundColor = Color.white;

	private int objectSize = 35;
	/**
	 * @return Returns the objectSize.
	 */
	public int getObjectSize() {
		return objectSize;
	}
	/**
	 * @param objectSize The objectSize to set.
	 */
	public void setObjectSize(int objectSize) {
		this.objectSize = objectSize;
	}
	private int worldWidth = 300; // X_Bounds
	private int worldHeight = 300; // Y_Bounds
	
	//	TODO: Wraparound on/off
	private boolean useLocalBounds = false;
	private boolean updateWhileDragging = true; // Update network as objects are dragged
	private boolean objectDraggingInitiateMovement = false;
	private boolean objectInhibitsMovement = false;

	//All world entities
	private ArrayList entityList = new ArrayList();
	private Agent currentCreature;
	
	private Point selectedPoint; 
	private WorldEntity selectedEntity = null;
	private Agent selectedCreature = null;
	
	private ArrayList commandTargets = new ArrayList();
	
	private JMenuItem deleteItem = new JMenuItem("Delete object");
	private JMenuItem addItem = new JMenuItem("Add new object");
	private JMenuItem addAgentItem = new JMenuItem("Add new agent"); //TODO: menu with submenus
	private JMenuItem objectPropsItem = new JMenuItem("Set object Properties");
	private JMenuItem creaturePropsItem = new JMenuItem("Set creature Properties");
	private JMenuItem propsItem = new JMenuItem("Set world properties");

	// Used to populate network popup menus
	private ArrayList input_list = new ArrayList();
	private ArrayList output_list = new ArrayList();
	
	private String worldName = "Default World";

    private boolean objectDraggingInitiatesMovement;


	/**
	 * Construct a world, set its background color
	 */
	public World() {
		
		setBackground(backgroundColor);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addKeyListener(this);
		this.setFocusable(true);
		
		init_popupMenu();
		init_outputs();
		init_inputs();
		
	}

	////////////////////
	// Initialization //
	////////////////////
	
	public void clear() {
		entityList.clear();
	}
	
	public void init() {
		for (int i = 0; i < entityList.size(); i++) {
			WorldEntity temp = (WorldEntity) entityList.get(i);
			temp.setParent(this);
		}
	}

	public void init_popupMenu() {
		deleteItem.addActionListener(this);
		objectPropsItem.addActionListener(this);
		addItem.addActionListener(this);
		addAgentItem.addActionListener(this);
		propsItem.addActionListener(this);
	}
	/**
	 * Initialize list of motor commands
	 */
	public void init_outputs() {
		output_list.add("North");
		output_list.add("South");
		output_list.add("East");
		output_list.add("West");
		output_list.add("North-east");
		output_list.add("North-west");
		output_list.add("South-east");
		output_list.add("South-west");
		output_list.add("Straight");
		output_list.add("Left");
		output_list.add("Right");
		
	}

	/**
	 * Initialize list of stimuli labels
	 */
	public void init_inputs() {

		// Currently a fixed list,
		// later add names for objects and
		// ability to have different sized input vectors
		// depending on the world that has been loaded.

		input_list.add("1");
		input_list.add("2");
		input_list.add("3");
		input_list.add("4");
		input_list.add("5");
		input_list.add("6");
		input_list.add("7");
		input_list.add("8");
		input_list.add("L1");
		input_list.add("L2");
		input_list.add("R1");
		input_list.add("R2");

	}

	
	//////////////////////
	// Graphics Methods //
	//////////////////////

	public void mouseEntered(MouseEvent mouseEvent) {
	}
	public void mouseExited(MouseEvent mouseEvent) {
	}
	public void mouseMoved(MouseEvent e) {
	}
	public void mouseClicked(MouseEvent mouseEvent) {
	}
	public void mouseReleased(MouseEvent mouseEvent) {
	}
	public void mouseDragged(MouseEvent e) {
		if(selectedEntity != null) {
			selectedEntity.setLocation(e.getPoint());
			repaint();
			if(updateWhileDragging == true) { 
				updateNetwork();
			}
		}	
	
	} 

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent mouseEvent) {

		selectedPoint = mouseEvent.getPoint();
		selectedEntity = findClosestEntity(selectedPoint, objectSize/2);
		
		if ((selectedEntity instanceof Agent) && (currentCreature == null)) {
			currentCreature = selectedCreature;
		}
		//Show popupmenu for right click
		if(mouseEvent.isControlDown() || (mouseEvent.getButton() == 3)) {
			JPopupMenu menu  = buildPopupMenu(selectedEntity);
			menu.show(this, (int)selectedPoint.getX(), (int)selectedPoint.getY());
		} 	
		//open dialogue for that world-item		
		else if (mouseEvent.getClickCount() == 2) {
			showEntityDialog(selectedEntity);
		} 	
		
		updateNetwork();

		java.awt.Container container = this.getParent().getParent();
		container.repaint();
	}

	public void actionPerformed(ActionEvent e) {

		Object e1 = e.getSource();

		// Handle pop-up menu events
		Object o = e.getSource();
		if (o instanceof JMenuItem) {
			if (o == deleteItem ) {
				deleteEntity(selectedEntity);
			} else if (o == addItem) {
				addEntity(selectedPoint);
			} else if (o == propsItem) { 
				showGeneralDialog();	
			} else if (o == objectPropsItem){
				showEntityDialog(selectedEntity);
			} else if (o == addAgentItem){
				addAgent(selectedPoint);
			}
			return;
		}
	}
	
	 public void keyReleased(KeyEvent k)
	 {
	 }
	 public void keyTyped(KeyEvent k)
	 {
	 }
	 
	 public void keyPressed(KeyEvent k)
	 {
	 	if (currentCreature == null) {
	 		return;
	 	}
	 	
	 	if(k.getKeyCode() == KeyEvent.VK_UP) {
	 		currentCreature.goStraight(1);
	 	} else if(k.getKeyCode() == KeyEvent.VK_DOWN) {
	 		currentCreature.goStraight(-1);
	 	} else if(k.getKeyCode() == KeyEvent.VK_RIGHT) {
	 		currentCreature.turnRight(4);
	 	} else if(k.getKeyCode() == KeyEvent.VK_LEFT) {
	 		currentCreature.turnLeft(4);
	 	}

	 	updateNetwork();
	 	
	 	repaint();
	 }
	              
		/**
		 * Used when the creature is directly moved in the world.
		 * 
		 * Used to update network from world, in a way which avoids iterating 
		 * the net more than once
		 */
		public void updateNetwork() {
			// When the creature is manually moved, target networks are updated
			for(int i = 0; i < commandTargets.size(); i++) {
				NetworkPanel np = (NetworkPanel)commandTargets.get(i);
				if ((np.getInteractionMode() == NetworkPanel.BOTH_WAYS)
						|| (np.getInteractionMode() == NetworkPanel.WORLD_TO_NET)) {
				    	if(objectDraggingInitiatesMovement){
				    	    np.updateNetworkAndWorld();
				    	} else {
				    	    np.updateNetwork();
				    	}
					
					} 
					if (np != null) {
						np.repaint();
					}
				
			}
		}

	
	/**
	 * Delete the specified world entity
	 * 
	 * @param e world entity to delete
	 */
	public void deleteEntity(WorldEntity e) {
		if (e != null) {
			entityList.remove(e);
			repaint();
		}
		e = null;
	}
	
	/**
	 * Add a world object at point p.  Note that it currently has a set of default values specified within the code.
	 * 
	 * @param p the location where the object should be added
	 */
	public void addEntity(Point p) {
	    WorldEntity we = new WorldEntity();
		we.setLocation(p);
		we.setImageName("Swiss.gif");
		we.getStimulus().setStimulusVector(new double[] {10,10,0,0,0,0,0,0});
		entityList.add(we);
		repaint();
	}
	
	/**
	 * Add an agent at point p. 
	 * 
	 * @param p the location where the agent should be added
	 */
	public void addAgent(Point p) {
	    Agent a = new Agent(this, "Mouse " + (getAgentList().size() + 1), "Mouse.gif", p.x, p.y, 45 );
		a.getStimulus().setStimulusVector(new double[] {0,0,0,0,0,0,0,0});
		entityList.add(a);
		repaint();
	}
	
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintWorld(g);
	}

	/**
	 * Paint all the objects in the world
	 * 
	 * @param g Reference to the world's graphics object
	 */
	public void paintWorld(Graphics g) {

		for (int i = 0; i < entityList.size(); i++) {
			WorldEntity theEntity = (WorldEntity) entityList.get(i);
			paintEntity(theEntity, g);
		}

		g.setColor(Color.black);
		g.setColor(Color.white);

	}

	/**
	 * Paint the entity
	 * 
	 * @param theEntity the entity to paint
	 * @param g reference to the World's graphics object
	 */
	public void paintEntity(WorldEntity theEntity, Graphics g) {

		theEntity.paintIcon(
			this,
			g,
			theEntity.getLocation().x - 20,
			theEntity.getLocation().y - 20);
	}

	/**
	 * Erase an entity in the world
	 * 
	 * @param theEntity the entity to erase
	 * @param g reference to the World's graphics object
	 */
	public void eraseEntity(WorldEntity theEntity, Graphics g) {
		g.setColor(backgroundColor);
		g.fillRect(
			theEntity.getLocation().x,
			theEntity.getLocation().y,
			theEntity.getIconWidth(),
			theEntity.getIconHeight());
	}
	/**
	 * Call up a {@link DialogWorldEntity} for a world object nearest to a specified point
	 * 
	 * @param theEntity the non-creature entity closest to this point will have a dialog called up
	 */
	public void showEntityDialog(WorldEntity theEntity) {
		DialogWorldEntity theDialog = null;
		
		if(theEntity != null) {
			theDialog = new DialogWorldEntity(theEntity);
			theDialog.pack();
			theDialog.show();
			if(!theDialog.hasUserCancelled())
			{
			    if(theEntity instanceof Agent){
			        theDialog.stimPanel.commitChanges();
			        theDialog.agentPanel.commitChanges();
			    } else {
			        theDialog.stimPanel.commitChanges();
			    }
			}
			repaint();			
		}
		
	}
	
	public void showGeneralDialog() {
		DialogWorld theDialog = new DialogWorld(this);
		theDialog.pack();
		theDialog.show();
		if(!theDialog.hasUserCancelled())
		{
			theDialog.getValues();
		}
		repaint();
	}
	
	public void showScriptDialog() {
		DialogScript theDialog = new DialogScript(this);
		theDialog.show();
		theDialog.pack();
		repaint();
	}
	

	//TODO: This returns the first entity found within a distance of radius; make it return the closest 
	//			entity within that radius
	private WorldEntity findClosestEntity(Point thePoint, double radius) {
		for (int i = 0; i < entityList.size(); i++) {
			WorldEntity temp = (WorldEntity) entityList.get(i);
			int distance = SimbrainMath.distance(thePoint, temp.getLocation());
			if (distance < radius) {
				return temp;
			}
		}
		return null;
	}

	/////////////////////////
	// Getters and Setters //
	/////////////////////////

	//This will be part of a world interface
	public ArrayList get_outputs() {
		return output_list;
	}
	public ArrayList get_inputs() {
		return input_list;
	}
	public ArrayList getEntityList() {
		return entityList;
	}

	public void setBounds(boolean val) {
		useLocalBounds = val;
	}
	public  boolean getLocalBounds() {
		return useLocalBounds;
	}
    
    /**
     * @return Returns the selectedEntity.
     */
    public WorldEntity getSelectedEntity() {
        return selectedEntity;
    }
    
	public ArrayList getCommandTargets() {
		return commandTargets;
	}
	public void setCommandTargets(ArrayList ct) {
		commandTargets = ct;
	}
	public void addCommandTarget(NetworkPanel np) {
		if(commandTargets.contains(np) == false) {
			commandTargets.add(np);
		}
	}
	
	public void removeCommandTarget(NetworkPanel np) {
		commandTargets.remove(np);
	}
	
	public void setEntityList(ArrayList theList) {
		entityList = theList;
	}
	

	/**
	 * @return true if the network should be updated as the creature is dragged, false otherwise
	 */
	public boolean isUpdateWhileDragging() {
		return updateWhileDragging;
	}

	/**
	 * @param b true if the network should be updated as the creature is dragged, false otherwise
	 */
	public void setUpdateWhileDragging(boolean b) {
		updateWhileDragging = b;
	}
	
	
	public String getRandomMovementCommand() {
		return((String)(output_list.get((int)(Math.random() * 8))));
	}
	
	/**
	 * Create a popup menu based on location of mouse click
	 * 
	 * @return the popup menu
	 */	
	public JPopupMenu buildPopupMenu(WorldEntity theEntity) {
		
		JPopupMenu ret = new JPopupMenu();

		if (theEntity instanceof WorldEntity){
			ret.add(objectPropsItem);
			ret.add(deleteItem);
		} else {
			ret.add(addItem);
			ret.add(addAgentItem);	
		}
		ret.add(propsItem);
		return ret;
	}

	//TODO: Delete once worlds are converted.
	public void initAgentList() {
		if (getAgentList().size() == 0) {
			addAgent(new Point(100,100));
		} 
		
		currentCreature = (Agent)getAgentList().get(0);

	}
		

	/**
	 * @return Returns the agentList.
	 */
	public ArrayList getAgentList() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < entityList.size(); i++) {
			WorldEntity temp = (WorldEntity) entityList.get(i);
			if(temp instanceof Agent) {
				ret.add(temp);
			}
		}
		return ret;
	}

	/**
	 * @return Returns the worldName.
	 */
	public String getName() {
		return worldName;
	}
	
	/**
	 * @param worldName The worldName to set.
	 */
	public void setName(String worldName) {
		this.worldName = worldName;
	}

	/**
	 * @return Returns the worldHeight.
	 */
	public int getWorldHeight() {
		return worldHeight;
	}

	/**
	 * @param worldHeight The worldHeight to set.
	 */
	public void setWorldHeight(int worldHeight) {
		this.worldHeight = worldHeight;
	}

	/**
	 * @return Returns the worldWidth.
	 */
	public int getWorldWidth() {
		return worldWidth;
	}

	/**
	 * @param worldWidth The worldWidth to set.
	 */
	public void setWorldWidth(int worldWidth) {
		this.worldWidth = worldWidth;
	}
	
    /**
     * @return Returns the objectDraggingInitiateMovement.
     */
    public boolean isObjectDraggingInitiateMovement() {
        return objectDraggingInitiateMovement;
    }
    /**
     * @param objectDraggingInitiateMovement The objectDraggingInitiateMovement to set.
     */
    public void setObjectDraggingInitiateMovement(
            boolean objectDraggingInitiateMovement) {
        this.objectDraggingInitiateMovement = objectDraggingInitiateMovement;
    }
    /**
     * @return Returns the objectInhibitsMovement.
     */
    public boolean isObjectInhibitsMovement() {
        return objectInhibitsMovement;
    }
    /**
     * @param objectInhibitsMovement The objectInhibitsMovement to set.
     */
    public void setObjectInhibitsMovement(boolean objectInhibitsMovement) {
        this.objectInhibitsMovement = objectInhibitsMovement;
    }
}
