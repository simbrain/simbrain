/*
 * Created on Aug 21, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simbrain.network;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.network.pnodes.PNodeText;
import org.simbrain.network.pnodes.PNodeWeight;
import org.simbrain.world.odorworld.AbstractEntity;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldAgent;
import org.simbrain.world.odorworld.OdorWorldEntity;
import org.simbrain.world.odorworld.Wall;

import edu.umd.cs.piccolo.PNode;

/**
 * Buffer which holds network objects
 */
public class NetworkClipboard {
	
	private static ArrayList clipboard;
	
	private static int PASTE_INCREMENT = 15;

	/**
	 * Clear the clipboard
	 *
	 */
	public static void clear() {
		clipboard = new ArrayList();
	}

	/**
	 * Add objects to the clipboard
	 * 
	 * @param objects objects to add
	 */
	public static void add(ArrayList objects, NetworkPanel net) {
		clipboard.addAll(objects);
		clipboard = copyClipboard(net);
	}
	
	/**
	 * Paste objects into the netPanel
	 * 
	 * @param net the network to paste into
	 */
	public static void paste(NetworkPanel net) {
		if (isEmpty()) return;
		ArrayList clipboardCopy = copyClipboard(net);
		translateObjects(clipboardCopy, net);
		for (int i = 0; i < clipboardCopy.size(); i++) {
			ScreenElement element = (ScreenElement)clipboardCopy.get(i);
			net.addNode((PNode)element, true);
		}
		net.select(clipboardCopy);
		net.renderObjects();
		net.repaint();
	}

	/**
	 * @return true if there's nothing in the clipboard, false otherwise
	 */
	public static boolean isEmpty() {
		return clipboard.isEmpty();
	}
	
	/**
	 * Make a copy of the clipboard
	 */
	private static ArrayList copyClipboard(NetworkPanel net) {
		ArrayList ret = new ArrayList();
		Hashtable nodeMappings = new Hashtable();
		
		for (int i = 0; i < clipboard.size(); i++) {
			ScreenElement oldNode = (ScreenElement)clipboard.get(i);
			if (oldNode instanceof PNodeNeuron) {
				PNodeNeuron newNeuron = PNodeNeuron.getDuplicate((PNodeNeuron)oldNode, net);
				nodeMappings.put(oldNode, newNeuron);
				ret.add(newNeuron);
			} 
		}
		
		for (int i = 0; i < clipboard.size(); i++) {
			ScreenElement element = (ScreenElement)clipboard.get(i);
			if (element instanceof PNodeWeight) {
				PNodeWeight oldWeight = (PNodeWeight)element;
				PNodeNeuron newSource = (PNodeNeuron)nodeMappings.get(oldWeight.getSource());
				PNodeNeuron newTarget = (PNodeNeuron)nodeMappings.get(oldWeight.getTarget());
				PNodeWeight newWeight = new PNodeWeight(newSource, newTarget, oldWeight.getWeight().duplicate());
				ret.add(newWeight);
			} 
		}		
		return ret;
	}

	/**
	 * Return the upper left corner of the clipbard objects
	 */
	private static Point2D getUpperLeft(ArrayList clip) {
		double centerX = 0;
		double centerY = 0;
		
		for (int i = 0; i < clip.size(); i++) {
			ScreenElement element = (ScreenElement)clip.get(i);
			if (element instanceof PNodeNeuron) {
				centerX = NetworkPanel.getGlobalX((PNode)element);
				centerY = NetworkPanel.getGlobalY((PNode)element);
				break;
			} 
		}
		
		for (int i = 0; i < clip.size(); i++) {
			ScreenElement element = (ScreenElement)clip.get(i);
			if (element instanceof PNodeNeuron) {
				if(NetworkPanel.getGlobalX((PNode)element) < centerX) {
					centerX = NetworkPanel.getGlobalX((PNode)element);
				}
				if(NetworkPanel.getGlobalY((PNode)element) < centerY) {
					centerY = NetworkPanel.getGlobalY((PNode)element);
				}
			} 
		}
		
		return new Point2D.Double(centerX, centerY);
	}

	
	/**
	 * Move the designated objects over based on number of pastes that have occurred in the specified network
	 */
	public static void translateObjects(ArrayList clip, NetworkPanel net) {
		Point2D center = null;
		try {
			center = getUpperLeft(clip);
		} catch (ClassCastException bie) {
			System.out.println("Can not calculate center point, use default position for paste");
			bie.printStackTrace();
		}

		for (int i = 0; i < clip.size(); i++) {
			ScreenElement element = (ScreenElement)clip.get(i);
			if (element instanceof PNodeNeuron) {
				if (net.getNumberOfPastes() == 0) {
					((PNodeNeuron) element).translate(
						net.getLastClicked().getX() - center.getX(),
						net.getLastClicked().getY() - center.getY());
				} else
					((PNodeNeuron) element).translate(
						(net.getLastClicked().getX()  - center.getX()) + (net.getNumberOfPastes() * PASTE_INCREMENT),
						(net.getLastClicked().getY() - center.getY()) + (net.getNumberOfPastes() * PASTE_INCREMENT));
			} 
		}
	}
		
}
