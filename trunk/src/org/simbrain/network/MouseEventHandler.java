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
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.simbrain.network.pnodes.PNodeLine;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.network.pnodes.PNodeText;
import org.simbrain.network.pnodes.PNodeWeight;
import org.simnet.interfaces.Network;
import org.simnet.networks.*;

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

	private Hashtable selection = null; // The current selection
	private PPath marquis = null;
	private PNode marquisParent = null; // Node that marquis is added to as a child
	private Point2D presspt = null;
	private Point2D canvasPressPt = null;
	private float strokeNum = 0;
	private Stroke[] strokes = null;
	private Hashtable allItems = null; // Used within drag handler temporarily
	private ArrayList unselectList = null; // Used within drag handler temporarily
	private HashMap marquisMap = null;
	private PNode pressNode = null; // Node pressed on (or null if none)
	private boolean deleteKeyActive = true; // True if DELETE key should delete selection

	private NetworkPanel netPanel;
	public static Paint marquisColor = Color.WHITE;

	private boolean haveObjectInClipboard = false;
	private Hashtable clipboard = null;
	private int numberOfPastes = 0;
	private int distance = 10;
	private Point2D lastClickedPosition = new Point2D.Double(50,50);
	private PNode currentNode = null; // Used for popupMenu
	
	//Popup menu items
	private JMenuItem copyItem = new JMenuItem("Copy");
	private JMenuItem cutItem = new JMenuItem("Cut");
	private JMenuItem pasteItem = new JMenuItem("Paste");
	private JMenuItem deleteItem = new JMenuItem("Delete");
	private JMenuItem connectItem = new JMenuItem("Connect");
	private JMenuItem setPropsItem = new JMenuItem("Set properties");
	private JMenuItem netPropsItem = new JMenuItem("Set network properties");
	private JMenuItem newWTAItem = new JMenuItem("Winner take all network");
	private JMenuItem hopfieldItem = new JMenuItem("Hopfield network");
	private JMenuItem backpropItem = new JMenuItem("Backprop network");
	private JMenuItem trainBackItem = new JMenuItem("Train backprop network");
	private JMenuItem randItem = new JMenuItem("Randomize network");

	private JMenuItem learnHopfieldItem = new JMenuItem("Train hopfield network");
	private JMenu newSubmenu = new JMenu("New");
	private JMenu outputMenu = new JMenu("Set output");
	private JMenu inputMenu = new JMenu("Set input");
	
	private JMenuItem alignSubmenu = new JMenu("Align");
	private JMenuItem alignHorizontal = new JMenuItem("Horizontal");
	private JMenuItem alignVertical = new JMenuItem("Vertical");
	private JMenuItem spacingSubmenu = new JMenu("Spacing");
	private JMenuItem spacingHorizontal = new JMenuItem("Horizontally");
	private JMenuItem spacingVertical = new JMenuItem("Vertically");
	


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

		selection = new Hashtable();
		clipboard = new Hashtable();
		allItems = new Hashtable();
		unselectList = new ArrayList();
		marquisMap = new HashMap();
		
		copyItem.addActionListener(netPanel);
		cutItem.addActionListener(netPanel);
		pasteItem.addActionListener(netPanel);
		connectItem.addActionListener(netPanel);
		deleteItem.addActionListener(netPanel);
		setPropsItem.addActionListener(netPanel);		
		netPropsItem.addActionListener(netPanel);
		outputMenu.addActionListener(netPanel);
		inputMenu.addActionListener(netPanel);
		newWTAItem.addActionListener(netPanel);
		hopfieldItem.addActionListener(netPanel);
		backpropItem.addActionListener(netPanel);
		alignHorizontal.addActionListener(netPanel);
		alignVertical.addActionListener(netPanel);
		spacingHorizontal.addActionListener(netPanel);
		spacingVertical.addActionListener(netPanel);
		randItem.addActionListener(netPanel);
		trainBackItem.addActionListener(netPanel);
		learnHopfieldItem.addActionListener(netPanel);
	}

	/**
	 * Return the last position clicked on screen
	 * 
	 * @return the last position which was left-clicked
	 */
	public Point2D getLastLeftClicked() {

		return lastClickedPosition;
	}

	/**
	 * @param Collection Collection of items to be selected
	 */
	public void select(Collection items) {
		Iterator itemIt = items.iterator();
		while (itemIt.hasNext()) {
			PNode node = (PNode) itemIt.next();
			select(node);
		}
	}

	/**
	 * @param items List of items to be selected
	 */
	public void select(Map items) {
		Iterator itemIt = items.keySet().iterator();
		while (itemIt.hasNext()) {
			PNode node = (PNode) itemIt.next();
			select(node);
		}
	}

	/**
	 * @param node PNode to be selected.  Add selection box.
	 */
	public void select(PNode node) {
		if (isSelected(node) || (netPanel.getCursorMode() != NetworkPanel.NORMAL)) {
			return;
		} //TODO
		if ((node instanceof PNodeNeuron) || (node instanceof ScreenElement)  ) {
			netPanel.select(node);
			selection.put(node, Boolean.TRUE);
			SelectionHandle.addSelectionHandleTo(node);
		} else if (node instanceof PNodeWeight) {
			// used when selectAllWeights is called
			netPanel.select(node);
			selection.put(((PNodeWeight) node).getWeightBall(), Boolean.TRUE);
			SelectionHandle.addSelectionHandleTo(
				((PNodeWeight) node).getWeightBall());
		} else if (node.getParent() instanceof PNodeWeight) {
			netPanel.select(node.getParent());
			selection.put(node, Boolean.TRUE);
			SelectionHandle.addSelectionHandleTo(node);
		}

	}

	/**
	 * Unselect a collection of objects
	 * @param items objects to be unselect
	 */
	public void unselect(Collection items) {
		Iterator itemIt = items.iterator();
		while (itemIt.hasNext()) {
			PNode node = (PNode) itemIt.next();
			unselect(node);
		}
	}

	/**
	 * Unselect a PNode object
	 * @param node PNode to be unselect
	 */
	public void unselect(PNode node) {
		if (!isSelected(node)) {
			return;
		}

		SelectionHandle.removeSelectionHandleFrom(node);
		selection.remove(node);
		/**/
		this.netPanel.unselect(node);
	}

	/**
	 * Unselects all current objects
	 */
	public void unselectAll() {
		Enumeration en = selection.keys();
		while (en.hasMoreElements()) {
			PNode node = (PNode) en.nextElement();
			SelectionHandle.removeSelectionHandleFrom(node);
		}
		selection.clear();
		this.netPanel.unselectAll();
	}

	/**
	 * Determines if a node is already selected
	 * 
	 * @param node node to be checked
	 * @return true if the input object is already selected; false otherwise
	 */
	public boolean isSelected(PNode node) {
		if ((node != null) && (selection.containsKey(node))) {
			return true;
		} else {
			return false;
		}
	}

	public Collection getSelection() {
		ArrayList sel = new ArrayList();
		Enumeration en = selection.keys();
		while (en.hasMoreElements()) {
			PNode node = (PNode) en.nextElement();
			sel.add(node);
		}

		return sel;
	}

	/**
	 * Determines whether or not a node is a child of a selectable parent.  
	 * 
	 * @param node the node to check
	 * @return true if the  node is a child of a node in the list of selectable parents; false otherwise.
	 */
	protected boolean isSelectable(PNode node) {
		
		boolean selectable = false;
		
		if (node instanceof PNodeLine) {
			return false;
		}
		
		Iterator parentsIt = netPanel.getNodeList().iterator();
		while (parentsIt.hasNext()) {
			PNode parent = (PNode) parentsIt.next();
			if (parent.getAllNodes().contains(node)) {
				selectable = true;
				break;
			} else if (parent instanceof PCamera) {
				for (int i = 0; i < ((PCamera) parent).getLayerCount(); i++) {
					PLayer layer = ((PCamera) parent).getLayer(i);
					if (layer.getChildrenReference().contains(node)) {
						selectable = true;
						break;
					}
				}
			}
		}

		return selectable;
	}

	///////////////////////////////////////////////////////
	// Methods for modifying the set of selectable nodes //
	///////////////////////////////////////////////////////

	public void addSelectableNode(PNode node) {
		netPanel.getSelection().add(node);
	}

	public void removeSelectableNode(PNode node) {
		netPanel.getSelection().remove(node);
	}

	public void setSelectableNode(PNode node) {
		netPanel.getSelection().clear();
		netPanel.getSelection().add(node);
	}

	public void setSelectableNodes(Collection c) {
		netPanel.getSelection().clear();
		netPanel.getSelection().addAll(c);
	}

	public Collection getSelectableNodes() {
		return (ArrayList) netPanel.getSelection().clone();
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
		unselectAll();
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
		if (!isSelected(pressNode)) {
			unselectAll();
			if (isSelectable(pressNode)) {
				// Avoid select weight's line.  
				if (!(pressNode instanceof PNodeLine)) {
					select(pressNode);
				}
			}
		}
	}

	protected void startStandardOptionSelection(PInputEvent pie) {
		// Option indicator is down, toggle selection
		if (isSelectable(pressNode)) {
			if (isSelected(pressNode)) {
				unselect(pressNode);
			} else {
				select(pressNode);
			}
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
				allItems.put(itemsIt.next(), Boolean.TRUE);
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
		Enumeration selectionEn = selection.keys();
		while (selectionEn.hasMoreElements()) {
			PNode node = (PNode) selectionEn.nextElement();
			if (!allItems.containsKey(node)) {
				unselectList.add(node);
			}
		}
		unselect(unselectList);

		// Then select the rest
		selectionEn = allItems.keys();
		while (selectionEn.hasMoreElements()) {
			PNode node = (PNode) selectionEn.nextElement();
			if (!selection.containsKey(node)
				&& !marquisMap.containsKey(node)) {
				marquisMap.put(node, Boolean.TRUE);
			}
		}
		
		select(allItems);
	}

	// Drag lasso with option (shift) button down
	// TODO: Make toggling possible
	protected void computeOptionmarquisSelection(PInputEvent pie) {

		unselectList.clear();
		Enumeration selectionEn = selection.keys();
		while (selectionEn.hasMoreElements()) {
			PNode node = (PNode) selectionEn.nextElement();
			if (allItems.containsKey(node)) {
				unselectList.add(node);
			}
		}
		unselect(unselectList);
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

		// There was a press node, so drag selection
		PDimension d = e.getDeltaRelativeTo(pressNode);
		Enumeration selectionEn = selection.keys();
		while (selectionEn.hasMoreElements()) {
			PNode node = (PNode) selectionEn.nextElement();
			/**/
			if (!(node.getParent() instanceof PNodeWeight)) {
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

	public boolean haveObjectInClipboard() {
		return haveObjectInClipboard;
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

		//		if(e.isShiftDown()) {
		//			System.out.println("HERE");
		//			netPanel.addText("TEST", (int)e.getCanvasPosition().getX(),(int)e.getCanvasPosition().getY());
		//		}
		
	}

	
	//TODO Migrate zoom stuff into a separate event handler
	/**
	 * Handle double clicks
	 */
	public void mouseClicked(PInputEvent e) {
		
		//Zoom in on clicked area of screen
		if(netPanel.getCursorMode() == NetworkPanel.ZOOMIN) {			
			double VIEW = netPanel.getCamera().getViewBounds().getWidth() / 2;
			PBounds rec = new PBounds(e.getPosition().getX() - (VIEW/2), e.getPosition().getY() - (VIEW/2), VIEW, VIEW);
			PPath n = new PPath(rec.getBounds2D());
			netPanel.getCamera().animateViewToCenterBounds(rec, true, 1000);
		}
		
		//Zoom out from clicked area of screen
		if(netPanel.getCursorMode() == NetworkPanel.ZOOMOUT) {			
			double VIEW = netPanel.getCamera().getViewBounds().getWidth() * 1.5;
			PBounds rec = new PBounds(e.getPosition().getX() - (VIEW/2), e.getPosition().getY() - (VIEW/2), VIEW, VIEW);
			PPath n = new PPath(rec.getBounds2D());
			netPanel.getCamera().animateViewToCenterBounds(rec, true, 1000);
		}
			
		PNode theNode = e.getPickedNode();
		if (e.getClickCount() == 2) {
			netPanel.showPrefsDialog(theNode);
		} 
		
	}

	public void setmarquisColor(Paint color) {
		marquisColor = color;
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
	}

	////////////////////////////////////////////////////////
	// The overridden methods from PDragSequenceEventHandler
	////////////////////////////////////////////////////////

	protected void startDrag(PInputEvent e) {

		super.startDrag(e);

		initializeSelection(e);

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

	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.event.PDragSequenceEventHandler#drag(edu.umd.cs.piccolo.event.PInputEvent)
	 */
	protected void drag(PInputEvent e) {
		super.drag(e);
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
		if (e.isAltDown() || (netPanel.getCursorMode() == NetworkPanel.ZOOMIN) || (netPanel.getCursorMode() == NetworkPanel.ZOOMOUT)) {

			PCamera cam = netPanel.getCamera();
			if (marquis != null) {
				PBounds rec = marquis.getBounds();
				if (rec.height > 10) {
					cam.animateViewToCenterBounds(rec, true, 1000);					
				}
			}
		} 
		
		if (ismarquisSelection(e)) {
			endmarquisSelection(e);
		} else {
			endStandardSelection(e);
		}

	}

	/**
	 * Return the center point of selected objects
	 * 
	 * @param hash
	 * @return the center-point
	 */
	private synchronized Point2D calCenterPoint(Hashtable selectObj) {
		double centerX = 0;
		double centerY = 0;
		//boolean first = true;
		int numberOfNeuron = 0;
		for (Enumeration e = selectObj.keys(); e.hasMoreElements();) {
			Object o = e.nextElement();
			if (!(o instanceof ClipboardElement)) {
				throw new ClassCastException("Failed to cast element to ClipboardElement!!");
			}
			ClipboardElement element = (ClipboardElement) o;
//			if (!((element.getClipboardObject() instanceof PNode) || (element.getClipboardObject() instanceof ScreenElement))) //TODO: Change to ScreenElement
//				throw new ClassCastException("Clipboard object must be instance of PNode");
			PNode node = (PNode) element.getClipboardObject();
			if (node instanceof PNodeNeuron) {
				if (numberOfNeuron == 0) {
					centerX = NetworkPanel.getGlobalX(node);
					centerY = NetworkPanel.getGlobalY(node);
				} else {
					centerX = NetworkPanel.getGlobalX(node) + centerX;
					centerY = NetworkPanel.getGlobalY(node) + centerY;
				}
				numberOfNeuron++;
			}

		}
		if (numberOfNeuron == 0) {
			return null;
		} else {
			centerX = (double) (centerX / numberOfNeuron);
			centerY = (double) (centerY / numberOfNeuron);
			return new Point2D.Double(centerX, centerY);
		}
	}

	///////////////////////////
	// Copy / Paste methods  //
	///////////////////////////

	/**
	 * Copy: Place selected objects in clipboard
	 */
	public void copyToClipboard() {
		//System.out.println("Copy to Clipboard");
		numberOfPastes = 0;
		clearClipboard();
		haveObjectInClipboard = true;

		for (Enumeration e = selection.keys(); e.hasMoreElements();) {
			PNode node = (PNode) e.nextElement();
			if ((node.getParent() instanceof PNodeWeight)) {
				node = node.getParent();
			}
			if (testValidity(node)) {
				Object cloneNode = null;
				if (node instanceof PNodeNeuron) {
					cloneNode = PNodeNeuron.getDuplicate((PNodeNeuron)node, netPanel);
				}
				else if (node instanceof PNodeWeight) {
					cloneNode = PNodeWeight.getDuplicate((PNodeWeight)node);
				}
				ClipboardElement cObj = new ClipboardElement();
				cObj.setClipboardObject(cloneNode);
				cObj.setSourceObject(node);
				clipboard.put(cObj, Boolean.TRUE);
			}
		}
		setNewSourceAndTargetForWeight(clipboard);
	}

	/**
	 * Cut: Place selected objects in clipbarod and delete them
	 */
	public void cutToClipboard() {
		this.copyToClipboard();
		this.netPanel.deleteSelection();
	}

	/**
	 *
	 * Re-associates weights in the clipboard with new source and target neurons
	 *
	 * @param hash The selected elements
	 */
	private void setNewSourceAndTargetForWeight(Hashtable hash) {
		for (Enumeration e = hash.keys(); e.hasMoreElements();) {

			ClipboardElement element = (ClipboardElement) e.nextElement();
			if (element.getClipboardObject() instanceof PNodeWeight) {
				PNodeNeuron srcObj =
					((PNodeWeight) (element.getClipboardObject())).getSource();
				PNodeNeuron tarObj =
					((PNodeWeight) (element.getClipboardObject())).getTarget();
				Object newSourceNeuron = findAppropriateNewObject(hash, srcObj);
				Object newTargetNeuron = findAppropriateNewObject(hash, tarObj);
				PNodeNeuron nS = (PNodeNeuron) newSourceNeuron;
				PNodeNeuron nT = (PNodeNeuron) newTargetNeuron;
				PNodeWeight w = (PNodeWeight) (element.getClipboardObject());
				w.setSource(nS);
				w.setTarget(nT);
				// w.render();
			}
		}

	}

	/**
	 * Make a copy hashtable of a hashtable consist of Clipboard objects
	 * ( specific to Simbrain ).  It helps to paste more than one times.
	 * 
	 * @param sourceHash The Selected Objects
	 * @param return hashtable of selected objects
	 */
	public Hashtable copyTo(Hashtable sourceHash) {
		Point2D center = null;
		try {
			center = calCenterPoint(sourceHash);
		} catch (ClassCastException bie) {
			System.out.println(
				"Can not calculate center point, use default position for paste");
			bie.printStackTrace();
		}

		Hashtable hash = new Hashtable();
		//haveObjectInClipboard = true;
		for (Enumeration e = sourceHash.keys(); e.hasMoreElements();) {
			ClipboardElement element = (ClipboardElement) e.nextElement();
			PNode node = (PNode) element.getClipboardObject();
			/*  if ( ( node instanceof PNodeWeight)) {
				   node = node.getParent();
			   }*/
			PNode cloneNode = node;
			if (cloneNode instanceof PNodeNeuron) {
				cloneNode = PNodeNeuron.getDuplicate((PNodeNeuron)cloneNode, netPanel);
				//TODO: Work here on location of pasted items: either last clicked if any, or previous + offset
				if (center != null) {
					((PNodeNeuron) cloneNode).translate(
						this.lastClickedPosition.getX() - center.getX(),
						this.lastClickedPosition.getY() - center.getY());
				} else
					((PNodeNeuron) cloneNode).translate(
						numberOfPastes * distance,
						numberOfPastes * distance);
			} else if (cloneNode instanceof PNodeWeight) {
				cloneNode = PNodeWeight.getDuplicate((PNodeWeight)cloneNode);
			}
			ClipboardElement cObj = new ClipboardElement();
			cObj.setClipboardObject(cloneNode);
			cObj.setSourceObject(node);
			hash.put(cObj, Boolean.TRUE);
		}
		setNewSourceAndTargetForWeight(hash);

		return hash;
	}
	
	//TODO: Make general
	
	/**
	 * Pastes objects ( PNodeNeurons or PNodeWeights ) in a Hashtable to NetworkPanel
	 * 
	 * @param hash objects to paste
	 */
	private void pasteFrom(Hashtable hash) {
		unselectAll();
		for (Enumeration e = hash.keys(); e.hasMoreElements();) {
			ClipboardElement element = (ClipboardElement) e.nextElement();
			if (element.getClipboardObject() instanceof PNodeNeuron) {
				netPanel.addNode((PNode) element.getClipboardObject(), false);
				select((PNode) element.getClipboardObject());
				// So that pasted nodes are selected
			} else if (element.getClipboardObject() instanceof PNodeWeight) {
				PNodeWeight wt = (PNodeWeight) element.getClipboardObject();
				netPanel.addWeight(
					wt.getSource(),
					wt.getTarget(),
					wt.getWeight());
			} else if (element.getClipboardObject() instanceof PNodeText) {
				netPanel.addNode((PNode) element.getClipboardObject(), false);
				select((PNode) element.getClipboardObject());
			}
		}
		netPanel.renderObjects();
	}
	
	/**
	 *  Make a copy of clipboard and paste its objects to NetworkPanel
	 */
	public void pasteFromClipboard() {
		numberOfPastes++;
		Hashtable temp = copyTo(clipboard);
		pasteFrom(temp);
	}

	/**
	 * Finds the ClipboardElement object that has a sourceObject which matches
	 * the given srcObj, after that it returns the clipboardObject associated
	 * with that ClipboardElement object.  It helps to find the new,
	 * appropriate source and target PNodeNeuron for a PNodeWeight.
	 *
	 * @param hash Hashtable that contains ClipboardElement objects to look up.
	 * @param srcObj the PNodeNeuron that is the key for finding
	 * @return the new object the PNodeWeight that has been copied
	 * from the input PNodeNeuron; null otherwise
	 */
	private Object findAppropriateNewObject(Hashtable hash, Object srcObj) {
		for (Enumeration e = hash.keys(); e.hasMoreElements();) {
			Object element = e.nextElement();
			if (element instanceof ClipboardElement) {
				Object sourceObj =
					((ClipboardElement) (element)).getSourceObject();
				if (sourceObj instanceof PNodeNeuron) {
					PNodeNeuron n = (PNodeNeuron) sourceObj;
					PNodeNeuron n1 = (PNodeNeuron) srcObj;
					if (n.equals(n1))
						return ((ClipboardElement) (element))
							.getClipboardObject();
				}
			}
		}
		return null;
	}
	
	//TODO: Shift this to ScreenElement, isSelectable
	/**
	 * Determines whether or not a node is able to be copied. For example, 
	 * if a node is an instance of Neuron, it returns true since there is no need
	 * to copy it. However, if the input node is a weight, testValidity() returns true
	 * only if its source and target neurons are in the to-be-copied list. That
	 * way no free-floating weights are copied.
	 * 
	 * @param node node to be checked
	 * @return true if this node can be copied, false otherwise
	 */
	private boolean testValidity(PNode node) {
		if ((node instanceof PNodeNeuron) || (node instanceof PNodeText)) {
			return true;
		}
		if (node instanceof PNodeWeight) {
			PNodeWeight w = (PNodeWeight) node;
			if (w.getSource() != null) {
				if (selection.containsKey(w.getSource())
					&& selection.containsKey(w.getTarget())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Clear clipboard of objects
	 */
	private void clearClipboard() {
		clipboard.clear();
		this.haveObjectInClipboard = false;
	}

	///////////////////////////
	// Selection Methods     //
	///////////////////////////

	/**
	 * Select all neurons and weights
	 */
	public void selectAll() {
		unselectAll();
		this.netPanel.selectAll();
		Iterator i = netPanel.getSelection().iterator();
		while (i.hasNext()) {
			select((PNode) i.next());
		}
	}
	/**
	 * Select all neurons
	 */
	public void selectAllNeurons() {
		unselectAll();
		this.netPanel.selectNeurons();
		Iterator i = netPanel.getSelection().iterator();
		while (i.hasNext()) {
			select((PNode) i.next());
		}

	}
	/**
	 * Select all weights
	 */
	public void selectAllWeights() {
		unselectAll();
		this.netPanel.selectWeights();
		Iterator i = netPanel.getSelection().iterator();
		while (i.hasNext()) {
			select((PNode) i.next());
		}
	}
	
	
	/**
	 * Create a popup menu based on location of mouse click
	 * 
	 * @return the popup menu
	 */	
	public JPopupMenu buildPopupMenu(PNode theNode) {
		
		currentNode = theNode;
		JPopupMenu ret = new JPopupMenu();
		
		if (theNode instanceof PCamera)  {
			if(clipboard.size() > 0){
				ret.add(pasteItem);	
			}
			ret.add(newSubmenu);
			newSubmenu.add(newWTAItem);
			newSubmenu.add(hopfieldItem);
			newSubmenu.add(backpropItem);			
		} else if (theNode instanceof PNodeNeuron ){
			Network parent = ((PNodeNeuron)theNode).getNeuron().getNeuronParent();
			Network parent_parent = parent.getNetworkParent();
			if(parent_parent != null) {
				if (parent_parent instanceof Backprop) {
					ret.add(trainBackItem);
					ret.add(randItem);
				}
			}
			if(parent != null) {
				if (parent instanceof Hopfield) {
					ret.add(learnHopfieldItem);
					ret.add(randItem);
				}
			}
			
			ret.add(copyItem);
			ret.add(cutItem);
			ret.add(connectItem);
			ret.add(setPropsItem);
			ret.addSeparator();
			ret.add(alignSubmenu);
			alignSubmenu.add(alignHorizontal);
			alignSubmenu.add(alignVertical);
			ret.addSeparator();
			ret.add(spacingSubmenu);
			spacingSubmenu.add(spacingHorizontal);
			spacingSubmenu.add(spacingVertical);
			ret.addSeparator();
			ret.add(deleteItem);
			ret.add(inputMenu);
			setInputMenu();
			ret.add(outputMenu);
			setOutputMenu();
		} else if (theNode instanceof PPath) {
			ret.add(setPropsItem);
			ret.add(deleteItem);			
		}
		
		ret.add(netPropsItem);
		
		return ret;
	}
	
	
	/**
	 * Populate the "set outputs" submenu
	 */
	public void setOutputMenu() {
		ArrayList outputs = netPanel.getWorld().get_outputs();
		outputMenu.removeAll();
		
		for (int i = 0; i < outputs.size(); i++) {
			JMenuItem mi = new JMenuItem("" + (String)outputs.get(i));
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String name = ((JMenuItem)e.getSource()).getText();
					setOutput(name);
				}
			});
			outputMenu.add(mi);
		}
		
		JMenuItem notOutput = new JMenuItem("Set to non-output node");
		notOutput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((PNodeNeuron)currentNode).setOutput(false);
			}});
		outputMenu.add(notOutput);
		
	}

	/**
	 * Populate the "set input" submenu
	 */
	public void setInputMenu() {
		ArrayList inputs = netPanel.getWorld().get_inputs();
		inputMenu.removeAll();
					
		for (int i = 0; i < inputs.size(); i++) {
			JMenuItem mi = new JMenuItem("" + (String)inputs.get(i));
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String name = ((JMenuItem)e.getSource()).getText();
					setInput(name);
				}
			});
			inputMenu.add(mi);
		}
		
		JMenuItem notInput = new JMenuItem("Set to non-input node");
		notInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((PNodeNeuron)currentNode).setInput(false);
			}});
		inputMenu.add(notInput);

	}

	/**
	 * Set the motor command this output node is associated with.
	 * 
	 * @param name name of the  output to associate this node with
	 */
	public void setOutput(String name) {
			((PNodeNeuron)currentNode).setOutput(true);
			((PNodeNeuron)currentNode).getNeuron().setOutputLabel(name);
			netPanel.renderObjects();
	}
	
	/**
	 * Set the stimulus this input node is associated with
	 * 
	 * @param name name of the  output to associate this node with
	 */
	public void setInput(String name) { //Make this an integer
			((PNodeNeuron)currentNode).setInput(true);
			((PNodeNeuron)currentNode).getNeuron().setInputLabel(name);
			netPanel.renderObjects();
	}
		
	public PNode getCurrentNode() {
		return currentNode;
	}

	public Hashtable getClipboard() {
		return clipboard;
	}

}