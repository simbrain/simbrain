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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPreferences;
import org.simbrain.network.pnodes.PNodeLine;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.network.pnodes.PNodeSubNetwork;
import org.simbrain.network.pnodes.PNodeText;
import org.simbrain.network.pnodes.PNodeWeight;
import org.simnet.interfaces.Network;
import org.simnet.networks.Backprop;
import org.simnet.networks.Hopfield;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PNodeFilter;

/**
 * <b>NetworkSelectionEventHandler</b> was originally a PSelectionEventHandler but has a 
 * reference to the NetworkPanel and creates a differently colored marquis. 
 * (PSelectionEventHandler only provided a black marquis, which is difficult to see if
 * the background color is also black ).
 *
 *TODO: Separate regular mouse handling from selection event handling
 * @author Mai Ngoc Thang
 *
 */
public class MouseEventHandler extends PDragSequenceEventHandler {

	final static int DASH_WIDTH = 5;
	final static int NUM_STROKES = 10;

	// Temporary object lists for marquis selection
	private ArrayList allItems = null; 
	private ArrayList unselectList = null; 
	
	// Used by build tool to connect neurons
	private ArrayList connectionLines = new ArrayList();
	private ArrayList tempSources = new ArrayList();

	private PPath marquis = null;
	private PNode marquisParent = null; // Node that marquis is added to as a child
	private Point2D presspt = null;
	private Point2D canvasPressPt = null;
	private float strokeNum = 0;
	private Stroke[] strokes = null;
	private HashMap marquisMap = null;
	private PNode pressNode = null; // Node pressed on (or null if none)
	private boolean deleteKeyActive = true; // True if DELETE key should delete selection

	private NetworkPanel netPanel;
	private static Paint marquisColor = 
	    new Color(NetworkPreferences.getLassoColor());
	private boolean haveObjectInClipboard = false;
	private Point2D lastClickedPosition = new Point2D.Double(50,50);
	private PNode currentNode = null; // Used for popupMenu
	
	//Popup menu items
	private JMenuItem copyItem = new JMenuItem("Copy");
	private JMenuItem cutItem = new JMenuItem("Cut");
	private JMenuItem pasteItem = new JMenuItem("Paste");
	private JMenuItem deleteItem = new JMenuItem("Delete");
	private JMenuItem connectItem = new JMenuItem("Connect");
	private JMenuItem newNeuronMenuItem = new JMenuItem("New Neuron");
	private JMenuItem setNeuronPropsItem = new JMenuItem("Set neuron properties");
	private JMenuItem setWeightPropsItem = new JMenuItem("Set synapse properties");
	private JMenuItem netPropsItem = new JMenuItem("Set general network properties");
	private JMenuItem newWTAItem = new JMenuItem("Winner take all network");
	private JMenuItem newHopfieldItem = new JMenuItem("Hopfield network");
	private JMenuItem newBackpropItem = new JMenuItem("Backprop network");
	private JMenuItem newCustomItem = new JMenuItem("Custom network");
	private JMenuItem trainBackItem = new JMenuItem("Set backprop network properties");
	private JMenuItem randItem = new JMenuItem("Randomize network");

	private JMenuItem learnHopfieldItem = new JMenuItem("Train hopfield network");
	private JMenu newSubmenu = new JMenu("New network");
	private JMenu outputMenu = new JMenu("Set output");
	private JMenu inputMenu = new JMenu("Set input");
	
	private JMenuItem alignSubmenu = new JMenu("Align");
	private JMenuItem alignHorizontal = new JMenuItem("Horizontal");
	private JMenuItem alignVertical = new JMenuItem("Vertical");
	private JMenuItem spacingSubmenu = new JMenu("Spacing");
	private JMenuItem spacingHorizontal = new JMenuItem("Horizontally");
	private JMenuItem spacingVertical = new JMenuItem("Vertically");
	
	private JMenu setGaugeSubmenu = new JMenu("Set gauge"); //TODO: Make a submenu


	/**
	 * Constructor
	 * 
	 * @param network Reference to network panel
	 * @param marquisParent 
	 */
	public MouseEventHandler(NetworkPanel network, PNode marquisParent) {
		this.marquisParent = marquisParent;
		this.netPanel = network;
		init();
	}

	/**
	 * Initilize the network event handler
	 *
	 */
	protected void init() {
		float[] dash = { DASH_WIDTH, DASH_WIDTH };
		strokes = new Stroke[NUM_STROKES];
		for (int i = 0; i < NUM_STROKES; i++) {
			strokes[i] =
				new BasicStroke( 1,
					BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER,
					1,dash, i);
		}

		allItems = new ArrayList();
		unselectList = new ArrayList();
		marquisMap = new HashMap();
		
		newNeuronMenuItem.addActionListener(netPanel);
		newNeuronMenuItem.setActionCommand("newNeuron");
		copyItem.addActionListener(netPanel);
		copyItem.setActionCommand("copy");
		cutItem.addActionListener(netPanel);
		cutItem.setActionCommand("cut");
		pasteItem.addActionListener(netPanel);
		pasteItem.setActionCommand("paste");
		connectItem.addActionListener(netPanel);
		connectItem.setActionCommand("connect");
		deleteItem.addActionListener(netPanel);
		deleteItem.setActionCommand("delete");
		setNeuronPropsItem.addActionListener(netPanel);	
		setNeuronPropsItem.setActionCommand("setNeuronProps");
		setWeightPropsItem.addActionListener(netPanel);	
		setWeightPropsItem.setActionCommand("setSynapseProps");
		netPropsItem.addActionListener(netPanel);
		netPropsItem.setActionCommand("setGeneralNetProps");
		outputMenu.addActionListener(netPanel);
		outputMenu.setActionCommand("setOutput");
		inputMenu.addActionListener(netPanel);
		inputMenu.setActionCommand("setInput");
		newWTAItem.addActionListener(netPanel);
		newWTAItem.setActionCommand("winnerTakeAllNetwork");
		newHopfieldItem.addActionListener(netPanel);
		newHopfieldItem.setActionCommand("hopfieldNetwork");
		newBackpropItem.addActionListener(netPanel);
		newBackpropItem.setActionCommand("backpropNetwork");
		newCustomItem.addActionListener(netPanel);
		newCustomItem.setActionCommand("customNetwork");
		alignHorizontal.addActionListener(netPanel);
		alignHorizontal.setActionCommand("horizontal");
		alignVertical.addActionListener(netPanel);
		alignVertical.setActionCommand("vertical");
		spacingHorizontal.addActionListener(netPanel);
		spacingHorizontal.setActionCommand("spacingHorizontal");
		spacingVertical.addActionListener(netPanel);
		spacingVertical.setActionCommand("spacingVertical");
		randItem.addActionListener(netPanel);
		randItem.setActionCommand("randomizeNetwork");
		trainBackItem.addActionListener(netPanel);
		trainBackItem.setActionCommand("trainBackpropNetwork");
		learnHopfieldItem.addActionListener(netPanel);
		learnHopfieldItem.setActionCommand("trainHopfieldNetwork");

	}

	/**
	 * Return the last position clicked on screen
	 * 
	 * @return the last position which was left-clicked
	 */
	public Point2D getLastLeftClicked() {

		return lastClickedPosition;
	}

	////////////////////////////
	// Additional methods
	////////////////////////////

	public boolean isOptionSelection(PInputEvent pie) {
		return pie.isShiftDown();
	}
	public boolean isControlSelection(PInputEvent pie) {
		return pie.isControlDown();
	}

	protected boolean ismarquisSelection(PInputEvent pie) {
		return (pressNode == null);
	}

	protected void initializeSelection(PInputEvent pie) {
		canvasPressPt = pie.getCanvasPosition();
		presspt = pie.getPosition();
		pressNode = pie.getPath().getPickedNode();
		if (pressNode instanceof PCamera) {
			pressNode = null;
		}
	}

	protected void initializeMarquis(PInputEvent e) {
		marquis =
			PPath.createRectangle(
				(float) presspt.getX(),
				(float) presspt.getY(),
				0,
				0);
		marquis.setPaint(null);
		// use WHITE Color instead of black like PSelectionEventHandler
		marquis.setStrokePaint(marquisColor);
		marquis.setStroke(strokes[0]);
		marquisParent.addChild(marquis);
		marquisMap.clear();
	}

	protected void startOptionMarquisSelection(PInputEvent e) {
	}

	protected void startMarquisSelection(PInputEvent e) {
		netPanel.unselectAll();
	}

	/**
	 * Single clicks are taken care of here
	 */
	protected void startStandardSelection(PInputEvent pie) {
		
		// Right clicking on top of a node; don't deselect all!
		if (pie.isControlDown() || (pie.getButton() == 3)) {
			return;
		}
			
		// Option indicator not down - clear selection, and start fresh
		if (!netPanel.isSelected(pressNode)) {
			netPanel.unselectAll();
			// Avoid select weight's line.  
			if (!(pressNode instanceof PNodeLine)) {
				netPanel.select(pressNode);
			}
		}
	}

	protected void startStandardOptionSelection(PInputEvent pie) {
		// Option indicator is down, toggle selection
		if (netPanel.isSelected(pressNode)) {
			netPanel.unselect(pressNode);
		} else {
			netPanel.select(pressNode);
		}
	}

	/**
	 * Used in lasso selection. First,Êcalculate how many objects areÊcurrentlyÊin the lasso and
	 * add a rectangle to represent a selected object.Ê Whenever the lassoÊgets changedÊdue to theÊ
	 * movement of the mouse ( computer mouse, not the mice), updateMasquee updates the selection setÊto fit that change.Ê
	 */
	protected void updatemarquis(PInputEvent pie) {
		PBounds b = new PBounds();

		if (marquisParent instanceof PCamera) {
			b.add(canvasPressPt);
			b.add(pie.getCanvasPosition());
		} else {
			b.add(presspt);
			b.add(pie.getPosition());
		}

		//Adds the dash-line rectangle around the selected area.
		marquis.setPathToRectangle(
			(float) b.x,
			(float) b.y,
			(float) b.width,
			(float) b.height);
		b.reset();
		b.add(presspt);
		b.add(pie.getPosition());

		allItems.clear();
		PNodeFilter filter = createNodeFilter(b);
		// Create an instance of inner class BoundsFilter, which selects only neurons and weight-ball
		Iterator nodes = netPanel.getNodeList().iterator();
		while (nodes.hasNext()) {
			PNode node = (PNode) nodes.next();
			Collection items;
			if (node instanceof PCamera) { 
				items = new ArrayList();
				for (int i = 0; i < ((PCamera) node).getLayerCount(); i++) {
					((PCamera) node).getLayer(i).getAllNodes(filter, items);
				}
			} else {
				items = node.getAllNodes(filter, null);
			}
			Iterator itemsIt = items.iterator();
			while (itemsIt.hasNext()) {
				allItems.add(itemsIt.next());
			}
		}
	}

	/**
	 * Here is where the drag and select action happens
	 */
	protected void computemarquisSelection(PInputEvent pie) {
		unselectList.clear();
		// Make just the items in the list selected
		// Do this efficiently by first unselecting things not in the list
		Iterator i = netPanel.getSelection().iterator();
		while (i.hasNext()) {
			PNode node = (PNode) i.next();
			if (!allItems.contains(node)) {
				unselectList.add(node);
			}
		}
		netPanel.unselect(unselectList);

		// Then select the rest
		Iterator j = allItems.iterator();
		while (j.hasNext()) {
			PNode node = (PNode) j.next();
			if (!netPanel.getSelection().contains(node)
				&& !marquisMap.containsKey(node)) {
				marquisMap.put(node, Boolean.TRUE);
			}
		}
		
		netPanel.select(allItems);
	}

	// Drag lasso with option (shift) button down
	// TODO: Make toggling possible
	protected void computeOptionmarquisSelection(PInputEvent pie) {

		unselectList.clear();
		Iterator i = netPanel.getSelection().iterator();
		while (i.hasNext()) {
			PNode node = (PNode) i.next();
			if (allItems.contains(node)) {
				unselectList.add(node);
			}
		}
		netPanel.unselect(unselectList);
	}

	protected void computeControlmarquisSelection(PInputEvent pie) {
	}

	protected PNodeFilter createNodeFilter(PBounds bounds) {
		return new BoundsFilter(bounds);
	}

	protected PBounds getmarquisBounds() {
		if (marquis != null) {
			return marquis.getBounds();
		}
		return new PBounds();
	}

	/**
	 * Move selected objects
	 */
	protected void dragStandardSelection(PInputEvent e) {

		if (netPanel.getMode() == NetworkPanel.BUILD) {
			return;
		}
		
		// There was a press node, so drag selection
		PDimension d = e.getDeltaRelativeTo(pressNode);
		Iterator i = netPanel.getSelection().iterator();
		while (i.hasNext()) {
			PNode node = (PNode) i.next();
			if (!(node instanceof PNodeWeight)) {
				node.localToParent(d);
				node.offset(d.getWidth(), d.getHeight());
			}
		}
		netPanel.renderObjects();
	}

	protected void endmarquisSelection(PInputEvent e) {
		// Remove marquis
		marquis.removeFromParent();
		marquis = null;
	}

	protected void endStandardSelection(PInputEvent e) {
		pressNode = null;
	}

	/**
	 * This gets called continuously during the drag, and is used to animate the marquis
	 */
	protected void dragActivityStep(PInputEvent aEvent) {
		if (marquis != null) {
			float origStrokeNum = strokeNum;
			strokeNum = (strokeNum + 0.5f) % NUM_STROKES;
			// Increment by partial steps to slow down animation
			if ((int) strokeNum != (int) origStrokeNum) {
				marquis.setStroke(strokes[(int) strokeNum]);
			}
		}
	}

	public boolean getSupportDeleteKey() {
		return deleteKeyActive;
	}

	public boolean isDeleteKeyActive() {
		return deleteKeyActive;
	}

	/**
	 * Specifies if the DELETE key should delete the selection
	 */
	public void setDeleteKeyActive(boolean deleteKeyActive) {
		this.deleteKeyActive = deleteKeyActive;
	}

	/**
	 * Filter to determine whether a PNode within bounds should be selected.  
	 */
	protected class BoundsFilter implements PNodeFilter {
		PBounds bounds;

		protected BoundsFilter(PBounds bounds) {
			this.bounds = bounds;
		}

		public boolean accept(PNode node) {
			
			Rectangle2D rec = node.getBounds().getBounds2D();
			node.localToGlobal(rec);
			
			boolean boundsIntersects =
				rec.intersects(bounds);
	
			// Reasons not to count this as a section		
			if (	(boundsIntersects == false) ||
					(node == marquis) || 
					(node instanceof PNodeLine) || 
					isCameraLayer(node)) {
						return false;
			}
			
			return true;
					
		}

		public boolean acceptChildrenOf(PNode node) {
			return netPanel.getNodeList().contains(node) || isCameraLayer(node);
		}

		public boolean isCameraLayer(PNode node) {
			if (node instanceof PLayer) {
				for (Iterator i = netPanel.getSelection().iterator(); i.hasNext();) {
					PNode n = (PNode) i.next();
					if (n instanceof PCamera) {
						if (((PCamera) n).indexOfLayer((PLayer) node)
							!= -1) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	/**
	 * If control key is pressed, add a new weight between last select
	 * neuron and current one.  If double-click, show preference dialog.
	 * 
	 * @param e piccolo input events
	 */
	public void mousePressed(PInputEvent e) {

		super.mousePressed(e);
		lastClickedPosition = e.getPosition();
		PNode theNode = e.getPickedNode();
	
		// Handle right click
		if (e.isControlDown() || (e.getButton() == 3)) {

					//Quick connecting,w/o popup
					if (e.isAltDown()) {
						if(theNode instanceof PNodeNeuron) {
							netPanel.connectSelectedTo((PNodeNeuron)theNode);
						}
						//netPanel.unselectAll();
						return;
					}

			JPopupMenu menu  = buildPopupMenu(theNode);
			menu.show(
				netPanel, 
				(int)e.getCanvasPosition().getX(),
				(int)e.getCanvasPosition().getY());
		
		} 

	}

	
	//TODO Migrate zoom stuff into a separate event handler
	/**
	 * Handle double clicks
	 */
	public void mouseClicked(PInputEvent e) {
		
		netPanel.setNumberOfPastes(0);
		
		//Zoom in on clicked area of screen
		if(netPanel.getMode() == NetworkPanel.ZOOMIN) {			
			double VIEW = netPanel.getCamera().getViewBounds().getWidth() / 2;
			PBounds rec = new PBounds(e.getPosition().getX() - (VIEW/2), e.getPosition().getY() - (VIEW/2), VIEW, VIEW);
			netPanel.getCamera().animateViewToCenterBounds(rec, true, 1000);
		}
		
		//Zoom out from clicked area of screen
		if(netPanel.getMode() == NetworkPanel.ZOOMOUT) {			
			double VIEW = netPanel.getCamera().getViewBounds().getWidth() * 1.5;
			PBounds rec = new PBounds(e.getPosition().getX() - (VIEW/2), e.getPosition().getY() - (VIEW/2), VIEW, VIEW);
			netPanel.getCamera().animateViewToCenterBounds(rec, true, 1000);
		}
			
		PNode theNode = e.getPickedNode();
		
		if (e.getClickCount() == 2) {
			if(theNode.getClass() == PNodeText.class)
			{
				System.err.println("Editing text node!");
				KeyEventHandler keh = netPanel.getKeyEventHandler();
				keh.setEditingText((PNodeText)theNode);
			} else {
				netPanel.showPrefsDialog(theNode);
			}
		} 
		
	}

	/**
	 * @param e piccolo input event
	 */
	public void mouseDragged(PInputEvent e) {

		Iterator nodes = this.netPanel.getNodeList().iterator();
		while (nodes.hasNext()) {
			PNode p = (PNode) nodes.next();
			if (p instanceof PNodeWeight) {
				((PNodeWeight) p).updatePosition();
			}
		}
		super.mouseDragged(e);
		//netPanel.repaint(); Make this an option?
	}

	////////////////////////////////////////////////////////
	// The overridden methods from PDragSequenceEventHandler
	////////////////////////////////////////////////////////

	protected void startDrag(PInputEvent e) {

		super.startDrag(e);

		initializeSelection(e);

		if(netPanel.getMode() == NetworkPanel.BUILD) {
			tempSources = netPanel.getSelectedPNodeNeurons();
		}
		
		if (ismarquisSelection(e)) {
			initializeMarquis(e);

			if (!isOptionSelection(e)) {
				startMarquisSelection(e);
			} else {
				startOptionMarquisSelection(e);
			}
		} else {
			if (!isOptionSelection(e)) {
				startStandardSelection(e);
			} else {
				startStandardOptionSelection(e);
			}
		}

	}

	protected void drag(PInputEvent e) {
		super.drag(e);
		
		if(netPanel.getMode() == NetworkPanel.BUILD) {
			destroyConnectionLines();
			createConnectionLines(e);
		}
		
		if (ismarquisSelection(e)) {
			updatemarquis(e);

			if (!isOptionSelection(e)) {
				computemarquisSelection(e);
			} else {
				computeOptionmarquisSelection(e);
			}

		} else {
			dragStandardSelection(e);
		}
	}
		
	protected void endDrag(PInputEvent e) {

		super.endDrag(e);

		// If the alt/option key is down zoom to the current marquis
		if (e.isAltDown() || (netPanel.getMode() == NetworkPanel.ZOOMIN) || (netPanel.getMode() == NetworkPanel.ZOOMOUT)) {
			PCamera cam = netPanel.getCamera();
			if (marquis != null) {
				PBounds rec = marquis.getBounds();
				if (rec.height > 10) {
					cam.animateViewToCenterBounds(rec, true, 1000);					
				}
				netPanel.setAutoZoom(false);
			}
		} 
		
		
		if (ismarquisSelection(e)) {
			endmarquisSelection(e);
		} else {
			endStandardSelection(e);
		}
		
		// Make the connections
		if(netPanel.getMode() == NetworkPanel.BUILD) {
			destroyConnectionLines();
			netPanel.connect(tempSources, netPanel.getSelectedPNodeNeurons());
		}



	}

	////////////////////////////
	// Connection management  //
	///////////////////////////

	/**
	 * Create the connection lines
	 */
	private void createConnectionLines(PInputEvent e) {
		for (int i = 0; i < tempSources.size(); i++) {
			Point2D point = ((PNodeNeuron)tempSources.get(i)).getBounds().getCenter2D();
			PPath line = PPath.createPolyline(new Point2D[] {point, e.getPosition()});
			line.setStrokePaint(Color.cyan);
			connectionLines.add(line);
			marquisParent.addChild(line);
		}
		
	}
	
	/**
	 * Destroy the connection lines
	 */
	private void destroyConnectionLines() {
		for (int i = 0; i < connectionLines.size(); i++) {
			marquisParent.removeChild((PNode)connectionLines.get(i));
		}
		connectionLines.clear();
	}

	///////////////////////////
	// Copy / Paste methods  //
	///////////////////////////

	/**
	 * Copy: Place selected objects in clipboard
	 */
	/**
	 * Cut: Place selected objects in clipbarod and delete them
	 */
	public void cutToClipboard() {
		this.copyToClipboard();
		this.netPanel.deleteSelection();
	}
	
	public void copyToClipboard() {

		NetworkClipboard.clear();	
		netPanel.setNumberOfPastes(0);
		haveObjectInClipboard = true;
		
		ArrayList copiedObjects = new ArrayList();

		for (int i = 0; i < netPanel.getSelection().size(); i++) {
			PNode node = (PNode) netPanel.getSelection().get(i);
			if ((node.getParent() instanceof PNodeWeight)) {
				node = node.getParent();
			}
			if (canBeCopied(node)) {
				copiedObjects.add(node);
			}			
		}

		System.out.println(getSubnets(copiedObjects).size());

		
		// If copied objects contains everything in some subnet, add subnet to copied objects
		//   in networkclipboard, copy using a getDuplicate command akin to those there
		//	when pasting back use initCastor?
		
		NetworkClipboard.add(copiedObjects, netPanel);
	}

	private ArrayList getSubnets(ArrayList toCheck) {
		ArrayList ret = new ArrayList();
		ArrayList subnets = netPanel.getPNodeSubnets();
		
		for (int i = 0; i < subnets.size(); i++) {
			PNodeSubNetwork pn = (PNodeSubNetwork)subnets.get(i);
			if(toCheck.containsAll(pn.getPNodeNeurons())) {
				ret.add(pn);
			}
		}
		return ret;
	}

	
	/**
	 * Determines whether or not a pnode can be copied.  Free-floating
	 * weights, for example, cannot be copied
	 * 
	 * @param node node to be checked
	 * @return true if this node can be copied, false otherwise
	 */
	private boolean canBeCopied(PNode node) {
		if ((node instanceof PNodeNeuron) || (node instanceof PNodeText)) {
			return true;
		}
		if (node instanceof PNodeWeight) {
			PNodeWeight w = (PNodeWeight) node;
			if (w.getSource() != null) {
				if (netPanel.getSelection().contains(w.getSource())
					&& netPanel.getSelection().contains(w.getTarget())) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Create a popup menu based on location of mouse click
	 * 
	 * @return the popup menu
	 */	
	public JPopupMenu buildPopupMenu(PNode theNode) {
		
		currentNode = theNode;
		JPopupMenu ret = new JPopupMenu();
		
		//Nothing was clicked on
		if (theNode instanceof PCamera)  {
			if(!NetworkClipboard.isEmpty()){
				ret.add(pasteItem);	
			}
			ret.add(newNeuronMenuItem);
			ret.add(newSubmenu);
			newSubmenu.add(newWTAItem);
			newSubmenu.add(newHopfieldItem);
			newSubmenu.add(newBackpropItem);
			newSubmenu.add(newCustomItem);
			ret.add(netPropsItem);
		
		// A neuron was clicked on
		} else if (theNode instanceof PNodeNeuron ){

			// Network specific items
			Network parent =  ((PNodeNeuron)theNode).getNeuron().getParentNetwork();
			Network parent_parent = parent.getNetworkParent();
			if(parent_parent != null) {
				if (parent_parent instanceof Backprop) {
					ret.add(trainBackItem);
					ret.add(randItem);
					ret.addSeparator();
				}
			}
			if(parent != null) {
				if (parent instanceof Hopfield) {
					ret.add(learnHopfieldItem);
					ret.add(randItem);
					ret.addSeparator();
				}
			}
			
			// Edit items
			ret.add(copyItem);
			ret.add(cutItem);
			ret.add(deleteItem);
			ret.addSeparator();
		
			// Gauges
			addGaugeMenu(ret);
			ret.addSeparator();
			
			// Formatting
			ret.add(alignSubmenu);
			alignSubmenu.add(alignHorizontal);
			alignSubmenu.add(alignVertical);
			ret.add(spacingSubmenu);
			spacingSubmenu.add(spacingHorizontal);
			spacingSubmenu.add(spacingVertical);
			
			// Connections 
			ret.addSeparator();
			ret.add(connectItem);

			// Couplings
			ret.addSeparator();
			ret.add(netPanel.getParentFrame().getWorkspace().getMotorCommandMenu(netPanel,(PNodeNeuron)theNode));
			ret.add(netPanel.getParentFrame().getWorkspace().getSensorIdMenu(netPanel,(PNodeNeuron)theNode));

			// Set Properties
			ret.addSeparator();
			ret.add(setNeuronPropsItem);
			ret.add(netPropsItem);
			
			
			
		// A line or synapse was clicked on
		} else if (theNode instanceof PPath) {
			ret.add(deleteItem);		
			ret.add(setWeightPropsItem);
			ret.add(netPropsItem);
			addGaugeMenu(ret);
		}
		
		return ret;
	}
	
	// Set up setGauge Submenu
	private void addGaugeMenu(JPopupMenu theMenu) {
		JMenu gaugeMenu = netPanel.getParentFrame().getWorkspace().getGaugeMenu(netPanel);
		if (gaugeMenu != null) {
			theMenu.add(gaugeMenu);
		}
	}

		
	public PNode getCurrentNode() {
		return currentNode;
	}


    /**
     * @return Returns the marquisColor.
     */
    public static Paint getMarquisColor() {
        return marquisColor;
    }
    /**
     * @param marquisColor The marquisColor to set.
     */
    public static void setMarquisColor(Paint marquisColor) {
        MouseEventHandler.marquisColor = marquisColor;
    }
}