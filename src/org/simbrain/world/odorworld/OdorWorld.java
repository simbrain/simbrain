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

package org.simbrain.world.odorworld;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.simbrain.coupling.CouplingMenuItem;
import org.simbrain.coupling.MotorCoupling;
import org.simbrain.coupling.SensoryCoupling;
import org.simbrain.network.NetworkPanel;
import org.simbrain.workspace.Workspace;
import org.simbrain.world.Agent;
import org.simbrain.world.World;


/**
 * <b>OdorWorld</b> is the lowest-level environment panel which contains most
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
public class OdorWorld extends JPanel implements MouseListener, MouseMotionListener, ActionListener, KeyListener, World{

	private static final int SCROLLBAR_HEIGHT = 75;
	private static final int SCROLLBAR_WIDTH = 29;
	
	
	/** Color of the world background */
	private Color backgroundColor = Color.white;
	private int objectSize = 35;
	
	//Adjustable properties of worlds
	// General world properties	TODO make persistable
	private int worldWidth = 300; 
	private int worldHeight = 300;
	private boolean useLocalBounds = false;
	private boolean updateWhileDragging = true;
	private boolean objectDraggingInitiatesMovement = true;
	private boolean objectInhibitsMovement = true;
	private boolean drawingWalls = false;


	//World entities and entity selection
	private ArrayList abstractEntityList = new ArrayList();
	private OdorWorldAgent currentCreature = null;
	private AbstractEntity selectedEntity = null;
	private Point selectedPoint; 
	private Point draggingPoint;
	private Point wallPoint1;
	private Point wallPoint2;
	private Color wallColor = Color.RED;

	private int distanceX = 0;
	private int distanceY = 0;
	
	// List of neural networks to update when this world is updated
	private ArrayList commandTargets = new ArrayList();
	
	private String worldName = "Default World";
	private OdorWorldFrame parentFrame;
	private Workspace parentWorkspace;
	
	private OdorWorldMenu menu;

	public OdorWorld() {}
	
	/**
	 * Construct a world, set its background color
	 */
	public OdorWorld(OdorWorldFrame wf) {
		
		parentFrame = wf;
		
		setBackground(backgroundColor);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addKeyListener(this);
		this.setFocusable(true);
		
		menu = new OdorWorldMenu(this);
		
		menu.initMenu();
		
	}

	////////////////////
	// Initialization //
	////////////////////
	
	/**
	 * Remove all objects from world
	 */
	public void clear() {
		abstractEntityList.clear();
	}
	
	/**
	 * Initialize world; used by Castor for persistences.
	 *
	 */
	public void init() {
		for (int i = 0; i < getAbstractEntityList().size(); i++) {
			AbstractEntity temp = (AbstractEntity)getAbstractEntityList().get(i);
			temp.setParent(this);
		}
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
		if (drawingWalls) {
			wallPoint2 = mouseEvent.getPoint();
			addWall();
			draggingPoint = null;
			this.getParentFrame().setChangedSinceLastSave(true);
		}
	}
	public void mouseDragged(MouseEvent e) {
		if(drawingWalls){
			draggingPoint = e.getPoint();
			repaint();
		}
		Point test = new Point(e.getPoint().x+distanceX,e.getPoint().y+distanceY);
		if(selectedEntity != null && this.getBounds().contains(selectedEntity.getRectangle(test))){
			selectedEntity.setX(test.x);
			selectedEntity.setY(test.y);
			repaint();
			this.getParentFrame().setChangedSinceLastSave(true);
			if(updateWhileDragging == true) { 
				updateNetwork();
			}
		}
	} 

	public void mousePressed(MouseEvent mouseEvent) {

		selectedEntity = null;
		
		selectedPoint = mouseEvent.getPoint();
		
		for(int i=0;i < abstractEntityList.size() && selectedEntity == null;i++){
			AbstractEntity temp = (AbstractEntity)abstractEntityList.get(i);
			if(temp.getRectangle().contains(selectedPoint)){
				selectedEntity = temp;
			}
		}
		
		if (selectedEntity != null){
			distanceX = selectedEntity.getX()-mouseEvent.getPoint().x;
			distanceY = selectedEntity.getY()-mouseEvent.getPoint().y;
		}
		
		//submits point for wall drawing
		if (drawingWalls) {
			mouseEvent.getPoint();
			wallPoint1 = selectedPoint;
		}
		if (selectedEntity instanceof OdorWorldAgent) {
			currentCreature = (OdorWorldAgent)selectedEntity;
		}
		
		//Show popupmenu for right click
		if(mouseEvent.isControlDown() || (mouseEvent.getButton() == 3)) {
			JPopupMenu menu  = buildPopupMenu(selectedEntity);
			menu.show(this, (int)selectedPoint.getX(), (int)selectedPoint.getY());
		} 	
		//open dialogue for that world-item		
		else if (mouseEvent.getClickCount() == 2) {
			if (selectedEntity instanceof Wall){
				showWallDialog((Wall)selectedEntity);
			} else{
				showEntityDialog((OdorWorldEntity)selectedEntity);
			}
			this.getParentFrame().setChangedSinceLastSave(true);
		}
		
		if (updateWhileDragging){
			updateNetwork();
		}

		java.awt.Container container = this.getParent().getParent();
		container.repaint();
	}


	
	public void actionPerformed(ActionEvent e) {

		// Handle pop-up menu events
		Object o = e.getSource();
		if (o instanceof JMenuItem) {
			if (o == menu.deleteItem ) {
				removeEntity(selectedEntity);
				this.getParentFrame().setChangedSinceLastSave(true);
			} else if (o == menu.addItem) {
				addEntity(selectedPoint);
				this.getParentFrame().setChangedSinceLastSave(true);
			} else if (o == menu.propsItem) { 
				showGeneralDialog();	
				this.getParentFrame().setChangedSinceLastSave(true);
			} else if (o == menu.objectPropsItem){
				showEntityDialog((OdorWorldEntity)selectedEntity);
				this.getParentFrame().setChangedSinceLastSave(true);
			} else if (o == menu.addAgentItem){
				addAgent(selectedPoint);
				this.getParentFrame().setChangedSinceLastSave(true);
			} else if (o == menu.wallItem) {
				drawingWalls = true;
				this.getParentFrame().setChangedSinceLastSave(true);
			} else if (o == menu.wallPropsItem){
				showWallDialog((Wall)selectedEntity);
				this.getParentFrame().setChangedSinceLastSave(true);
			} else if (o == menu.copyItem || o == getParentFrame().getMenu().copyItem){
				WorldClipboard.copyItem(selectedEntity);
			} else if (o == menu.cutItem || o == getParentFrame().getMenu().cutItem){
				WorldClipboard.cutItem(selectedEntity,this);
				this.getParentFrame().setChangedSinceLastSave(true);
			} else if (o == menu.pasteItem || o == getParentFrame().getMenu().pasteItem){
				WorldClipboard.pasteItem(selectedPoint,this);
				this.getParentFrame().setChangedSinceLastSave(true);
			} else if (o == menu.clipboardClearItem || o == getParentFrame().getMenu().clipboardClearItem){
				WorldClipboard.clearClipboard();
			} else if (o == getParentFrame().getMenu().clearAllItem){
				clearAllEntities();
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

		 if(k.getKeyCode() == KeyEvent.VK_SPACE)
			 updateNetwork();
		 
		 if (currentCreature == null) {
	 		return;
		 }
	 	
	 	
	 	if(k.getKeyCode() == KeyEvent.VK_UP) {
	 		currentCreature.goStraightForward(1);
	 	} else if(k.getKeyCode() == KeyEvent.VK_DOWN) {
	 		currentCreature.goStraightBackward(1);
	 	} else if(k.getKeyCode() == KeyEvent.VK_RIGHT) {
	 		currentCreature.turnRight(4);
	 	} else if(k.getKeyCode() == KeyEvent.VK_LEFT) {
	 		currentCreature.turnLeft(4);
	 	} else if(k.getKeyCode() == KeyEvent.VK_DELETE || k.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
	 		removeEntity(selectedEntity);
	 	}

	 	if(k.getKeyCode() != KeyEvent.VK_SPACE)
	 		updateNetwork();
	 	
	 	repaint();
		this.getParentFrame().setChangedSinceLastSave(true);
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
				if ((np.getInteractionMode() == NetworkPanel.BOTH_WAYS) || (np.getInteractionMode() == NetworkPanel.WORLD_TO_NET)) {
				
					if((objectDraggingInitiatesMovement == true)   && (np.getInteractionMode() == NetworkPanel.BOTH_WAYS)){
				    	    np.updateNetworkAndWorld();
				    	} else {
				    	    np.updateNetwork();
				    	}
					
					if (np != null) {
						np.repaint();
					}
				}
			}
		}

		
	public void clearAllEntities(){
		while(abstractEntityList.size()>0)
			removeEntity((AbstractEntity)abstractEntityList.get(0));
		
		this.getParentFrame().setChangedSinceLastSave(true);
	}
	
	/**
	 * Remove the specified world entity
	 * 
	 * @param e world entity to delete
	 */
	public void removeEntity(AbstractEntity e) {
		if (e != null) {
			abstractEntityList.remove(e);
			
			if (e instanceof OdorWorldAgent) {
				ArrayList a = new ArrayList();
				a.add(e);
				this.getParentFrame().getWorkspace().removeAgentsFromCouplings(a);
				this.getParentFrame().getWorkspace().resetCommandTargets();
			}
			
			e = null;
			repaint();
		}
		this.getParentWorkspace().getNetworkList().repaintAllNetworkPanels();
	}
	
	/**
	 * Add a world object at point p.  Note that it currently has a set of default values specified within the code.
	 * 
	 * @param p the location where the object should be added
	 */
	public void addEntity(Point p) {
	    OdorWorldEntity we = new OdorWorldEntity();
		we.setLocation(p);
		we.setImageName("Swiss.gif");
		we.getStimulus().setStimulusVector(new double[] {10,10,0,0,0,0,0,0});
		abstractEntityList.add(we);
		repaint();
		this.getParentWorkspace().getNetworkList().repaintAllNetworkPanels();
		
	}
	
	/**
	 * Add an agent at point p. 
	 * 
	 * @param p the location where the agent should be added
	 */
	public void addAgent(Point p) {
	    OdorWorldAgent a = new OdorWorldAgent(this, "Mouse " + (getAgentList().size() + 1), "Mouse.gif", p.x, p.y, 45 );
		a.getStimulus().setStimulusVector(new double[] {0,0,0,0,0,0,0,0});
		abstractEntityList.add(a);
		this.getParentFrame().getWorkspace().attachAgentsToCouplings();
		repaint();
		this.getParentWorkspace().getNetworkList().repaintAllNetworkPanels();
	}
	
	/**
	 * passed two points, determineUpperLeft returns the upperleft point of the rect. they form
	 * @param p1
	 * @param p2
	 * @return
	 */
	private Point determineUpperLeft(Point p1,Point p2){
		Point temp = new Point();

		if (p1.x < p2.x) {
			temp.x = p1.x;
		} else if (p1.x >= p2.x) {
			temp.x = p2.x;
		}
		if (p1.y < p2.y) {
			temp.y = p1.y;
		} else if (p1.y >= p2.y) {
			temp.y = p2.y;
		}
		
		return temp;
		
	}
	
	public void addWall() {
		Wall newWall = new Wall(this);
		Point upperLeft = determineUpperLeft(wallPoint1,wallPoint2);

		newWall.setWidth(Math.abs(wallPoint2.x - wallPoint1.x));
		newWall.setHeight(Math.abs(wallPoint2.y - wallPoint1.y));
		newWall.setX(upperLeft.x);
		newWall.setY(upperLeft.y);
		
		newWall.getStimulus().setStimulusVector(new double[] {0,0,0,0,0,0,0,0});


		abstractEntityList.add(newWall);
		wallPoint1 = wallPoint2 = null;

		drawingWalls = false;
		this.repaint();
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

		for (int i = 0; i < abstractEntityList.size(); i++) {
			AbstractEntity theEntity = (AbstractEntity) abstractEntityList.get(i);
			theEntity.paintThis(g);
		}
		g.setColor(Color.WHITE);
		setBackground(backgroundColor);
		
		if (drawingWalls && (draggingPoint != null)){
			Point upperLeft = determineUpperLeft(wallPoint1,draggingPoint);
			int width = Math.abs(wallPoint1.x-draggingPoint.x);
			int height = Math.abs(wallPoint1.y-draggingPoint.y);
			g.setColor(Color.BLACK);
			g.drawRect(upperLeft.x,upperLeft.y,width,height);
		}

	}

	/**
	 * Call up a {@link DialogOdorWorldEntity} for a world object nearest to a specified point
	 * 
	 * @param theEntity the non-creature entity closest to this point will have a dialog called up
	 */
	public void showEntityDialog(OdorWorldEntity theEntity) {
		DialogOdorWorldEntity theDialog = null;
		
		if(theEntity != null) {
			theDialog = new DialogOdorWorldEntity(theEntity);
			theDialog.pack();
			theDialog.setVisible(true);
			if(!theDialog.hasUserCancelled())
			{
				theDialog.commitChanges();
			    if(theEntity instanceof OdorWorldAgent){
			        theDialog.stimPanel.commitChanges();
			        theDialog.agentPanel.commitChanges();
			    } else {
			        theDialog.stimPanel.commitChanges();
			    }
			}
			repaint();			
		}
		
	}
	
	public void showWallDialog(Wall theWall){
		DialogOdorWorldWall theDialog = null;
		
		theDialog = new DialogOdorWorldWall(this,theWall);
		theDialog.pack();
		theDialog.setVisible(true);
		if(!theDialog.hasUserCancelled()){
			theDialog.stimPanel.commitChanges();
		}
		repaint();
	}
	
	public void showGeneralDialog() {
		DialogOdorWorld theDialog = new DialogOdorWorld(this);
		theDialog.pack();
		theDialog.setVisible(true);
		if(!theDialog.hasUserCancelled())
		{
			theDialog.getValues();
		}
		repaint();
	}
	
	public void showScriptDialog() {
		DialogScript theDialog = new DialogScript(this);
		theDialog.setVisible(true);
		theDialog.pack();
		repaint();
	}
	
	/**
	 * Sets maximum size for the parent window
	 */
	public void resize() {
		this.getParentFrame().setMaximumSize(
				new Dimension(worldWidth + SCROLLBAR_WIDTH, worldHeight + SCROLLBAR_HEIGHT));
		this.setSize(
				new Dimension(worldWidth, worldHeight));
		this.getParentFrame()
				.setBounds(this.getParentFrame().getX(),
						this.getParentFrame().getY(), worldWidth + SCROLLBAR_WIDTH,
						worldHeight + SCROLLBAR_HEIGHT);
	}

	public ArrayList getAbstractEntityList() {
		return abstractEntityList;
	}

	public void setUseLocalBounds(boolean val) {
		useLocalBounds = val;
	}
	public  boolean getUseLocalBounds() {
		return useLocalBounds;
	}

    public AbstractEntity getSelectedEntity() {
        return selectedEntity;
    }
    
	public ArrayList getCommandTargets() {
		return commandTargets;
	}
	public void setCommandTargets(ArrayList ct) {
		commandTargets = ct;
	}
	
	/**
	 * Add a network to this world's list of command targets
	 * That neural net will be updated when the world is
	 */
	public void addCommandTarget(NetworkPanel np) {
		if(commandTargets.contains(np) == false) {
			commandTargets.add(np);
		}
	}

	/**
	 * Remove a network from the list of command targets
	 * that are updated when the world is
	 */
	public void removeCommandTarget(NetworkPanel np) {
		commandTargets.remove(np);
	}
	
	public void setAbstractEntityList(ArrayList theList) {
		abstractEntityList = theList;
	}
	

	/**
	 * @return true if the network should be updated as the creature is dragged, false otherwise
	 */
	public boolean getUpdateWhileDragging() {
		return updateWhileDragging;
	}

	/**
	 * @param b true if the network should be updated as the creature is dragged, false otherwise
	 */
	public void setUpdateWhileDragging(boolean b) {
		updateWhileDragging = b;
	}
	
	
	/**
	 * Create a popup menu based on location of mouse click
	 * 
	 * @return the popup menu
	 */	
	public JPopupMenu buildPopupMenu(AbstractEntity theEntity) {
		
		JPopupMenu ret = new JPopupMenu();

		if (theEntity instanceof AbstractEntity){
			ret.add(menu.copyItem);
			ret.add(menu.cutItem);
			ret.add(menu.deleteItem);
		}
		if (theEntity instanceof OdorWorldEntity){
			ret.addSeparator();
			ret.add(menu.objectPropsItem);
		} else if (theEntity instanceof Wall){
			ret.addSeparator();
			ret.add(menu.wallPropsItem);
		} else {
			if (WorldClipboard.clipboardEntity != null){
				ret.add(menu.pasteItem);
				ret.add(menu.clipboardClearItem);
				ret.addSeparator();
			}
			ret.add(menu.addItem);
			ret.add(menu.addAgentItem);	
			ret.add(menu.wallItem);
		}
		ret.addSeparator();
		ret.add(menu.propsItem);
		return ret;
	}

	//TODO: Delete once worlds are converted.
	public void initAgentList() {
		if (getAgentList().size() == 0) {
			addAgent(new Point(100,100));
		} 
		
		currentCreature = (OdorWorldAgent)getAgentList().get(0);

	}
	
	/**
	 * Go through entities in this world and find the one with the greatest number of dimensions.
	 * This will determine the dimensionality of the proximal stimulus sent to the network
	 * 
	 * @return the number of dimensions in the highest dimensional stimulus
	 */
	public int getHighestDimensionalStimulus() {
		Stimulus temp = null;
		int max = 0;
		for (int i = 0; i < getEntityList().size(); i++) {
				temp = ((OdorWorldEntity) getEntityList().get(i)).getStimulus();
				if(temp.getStimulusDimension() > max) max = temp.getStimulusDimension();
		}
		return max;
	}	
	
	/**
	 * 
	 * @return
	 */
	public ArrayList getEntityNames() {
		ArrayList temp = new ArrayList();
		for(int i =0; i< abstractEntityList.size(); i++){
			AbstractEntity tempElement = (AbstractEntity) abstractEntityList.get(i);
			if(tempElement instanceof OdorWorldEntity){
				temp.add(((OdorWorldEntity)tempElement).getName());
			}
		}
		return temp;
	}
	
	/**
	 * 
	 * @return a list of entity names
	 */
	public ArrayList getEntityList() {
		ArrayList temp = new ArrayList();
		for(int i =0; i< abstractEntityList.size(); i++){
			AbstractEntity tempElement = (AbstractEntity) abstractEntityList.get(i);
			if(tempElement instanceof OdorWorldEntity){
				temp.add(tempElement);
			}
		}
		return temp;
	}

	/**
	 * Returns a menu with a sub-menu for each agent
	 * 
	 * @param al the action listener (currently in the network panel) which listens to these menu events
	 * @return a JMenu with a list of sensors for each agent
	 */
	public JMenu getSensorIdMenu(ActionListener al) {
		JMenu ret = new JMenu(getName());
		int dims = getHighestDimensionalStimulus();
	
		for(int i = 0; i < getAgentList().size(); i++) {
			Agent agent = (Agent)getAgentList().get(i);
			JMenu agentMenu = new JMenu(agent.getName());

			JMenu centerMenu = new JMenu("Center");
			for(int j = 0; j < dims; j++) {
				CouplingMenuItem stimItem  = new CouplingMenuItem("" + (j + 1),new SensoryCoupling(agent, new String[] {"Center", "" + (j + 1)}));
				stimItem.addActionListener(al);
				centerMenu.add(stimItem);				
			}
			agentMenu.add(centerMenu);
			
			JMenu leftMenu = new JMenu("Left");
			for(int j = 0; j < dims; j++) {
				CouplingMenuItem stimItem  = new CouplingMenuItem("" + (j + 1),new SensoryCoupling(agent, new String[] {"Left", "" + j}));
				stimItem.addActionListener(al);
				leftMenu.add(stimItem);				
			}
			agentMenu.add(leftMenu);
			
			JMenu rightMenu = new JMenu("Right");
			for(int j = 0; j < dims; j++) {
				CouplingMenuItem stimItem  = new CouplingMenuItem("" + (j + 1),new SensoryCoupling(agent, new String[] {"Right", "" + j}));
				stimItem.addActionListener(al);
				rightMenu.add(stimItem);				
			}
			agentMenu.add(rightMenu);	
			ret.add(agentMenu);
		}
			
		return ret;
		
	}
	
	/**
	 * Returns a menu with the motor commands available to this agent
	 * 
	 * @param al the action listener (currently in the network panel) which listens to these menu events
	 * @return a JMenu with the motor commands available for this agent
	 */
	public JMenu getMotorCommandMenu(ActionListener al) {

		JMenu ret = new JMenu("" + this.getName());

		for(int i = 0; i < getAgentList().size(); i++) {
			Agent agent = (Agent)getAgentList().get(i);
			JMenu agentMenu = new JMenu(agent.getName());
		
			CouplingMenuItem motorItem  = new CouplingMenuItem("Forward",new MotorCoupling(agent, new String[] {"Forward"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				

		    motorItem  = new CouplingMenuItem("Backward",new MotorCoupling(agent, new String[] {"Backward"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				

			motorItem  = new CouplingMenuItem("Right",new MotorCoupling(agent, new String[] {"Right"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				
	
		    motorItem  = new CouplingMenuItem("Left",new MotorCoupling(agent, new String[] {"Left"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				
	
		    motorItem  = new CouplingMenuItem("North",new MotorCoupling(agent, new String[] {"North"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				
	
		    motorItem  = new CouplingMenuItem("West",new MotorCoupling(agent, new String[] {"West"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				
	
		    motorItem  = new CouplingMenuItem("East",new MotorCoupling(agent, new String[] {"East"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				
	
		    motorItem  = new CouplingMenuItem("North-east",new MotorCoupling(agent, new String[] {"North-east"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				
	
		    motorItem  = new CouplingMenuItem("North-west",new MotorCoupling(agent, new String[] {"North-west"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				
	
		    motorItem  = new CouplingMenuItem("South-east",new MotorCoupling(agent, new String[] {"South-east"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				
	
		    motorItem  = new CouplingMenuItem("South-west",new MotorCoupling(agent, new String[] {"South-west"}));
			motorItem.addActionListener(al);
			agentMenu.add(motorItem);				
			
			ret.add(agentMenu);
		}

		return ret;
		
	}
		
	/**
	 * @return Returns the agentList.
	 */
	public ArrayList getAgentList() {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < abstractEntityList.size(); i++) {
			AbstractEntity temp = (AbstractEntity) abstractEntityList.get(i);
			if(temp instanceof OdorWorldAgent) {
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
		this.getParentFrame().setTitle(worldName);
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
    public boolean getObjectDraggingInitiatesMovement() {
        return objectDraggingInitiatesMovement;
    }
    /**
     * @param objectDraggingInitiateMovement The objectDraggingInitiateMovement to set.
     */
    public void setObjectDraggingInitiatesMovement(
            boolean objectDraggingInitiatesMovement) {
        this.objectDraggingInitiatesMovement = objectDraggingInitiatesMovement;
    }
    /**
     * @return Returns the objectInhibitsMovement.
     */
    public boolean getObjectInhibitsMovement() {
        return objectInhibitsMovement;
    }
    /**
     * @param objectInhibitsMovement The objectInhibitsMovement to set.
     */
    public void setObjectInhibitsMovement(boolean objectInhibitsMovement) {
        this.objectInhibitsMovement = objectInhibitsMovement;
    }
    
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

	/**
	 * @return Returns the parentFrame.
	 */
	public OdorWorldFrame getParentFrame() {
		return parentFrame;
	}
	/**
	 * @param parentFrame The parentFrame to set.
	 */
	public void setParentFrame(OdorWorldFrame parentFrame) {
		this.parentFrame = parentFrame;
	}
	
	/**
	 * return type of world
	 */
	public String getType() {
		return "OdorWorld";
	}

	/**
	 * @return Returns the wallColor.
	 */
	public int getWallColor() {
		return wallColor.getRGB();
	}
	/**
	 * @param wallColor The wallColor to set.
	 */
	public void setWallColor(int wallColor) {
		this.wallColor = new Color(wallColor);
	}

	public Workspace getParentWorkspace() {
		return parentWorkspace;
	}

	public void setParentWorkspace(Workspace parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	public int getBackgroundColor() {
		return backgroundColor.getRGB();
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = new Color(backgroundColor);
	}
}
