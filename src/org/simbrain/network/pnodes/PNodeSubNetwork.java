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

package org.simbrain.network.pnodes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkPreferences;
import org.simbrain.network.ScreenElement;
import org.simnet.interfaces.ComplexNetwork;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.Synapse;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * <b>PNodeSubNetwork</b> represents a container for a subnetwork.
 */
public class PNodeSubNetwork extends PNode implements ScreenElement {
	
	int INDENT = 5;

    private static Color subnetColor = Color.GRAY;
    
    private Network subnet;
    private NetworkPanel parentPanel;
    
	PBounds cachedChildBounds = new PBounds();
	PBounds comparisonBounds = new PBounds();
	
	public PNodeSubNetwork(Network subnet, NetworkPanel parentpanel) {
		super();
		this.subnet = subnet;
		parentPanel = parentpanel;		
	}
	
	/**
	 * Change the default paint to fill an expanded bounding box based on its children's bounds
	 */
	public void paint(PPaintContext ppc) {

		Graphics2D g2 = ppc.getGraphics();
		g2.setPaint(subnetColor);
			
		PBounds bounds = getUnionOfChildrenBounds(null);
		bounds.setRect(bounds.getX()-INDENT,bounds.getY()-INDENT,bounds.getWidth()+2*INDENT,bounds.getHeight()+2*INDENT);			
		
		g2.draw(bounds);			
	}
				
	/**
	 * Change the full bounds computation to take into account that we are expanding the children's bounds
	 * Do this instead of overriding getBoundsReference() since the node is not volatile
	 */
	public PBounds computeFullBounds(PBounds dstBounds) {
		PBounds result = getUnionOfChildrenBounds(dstBounds);
		
		cachedChildBounds.setRect(result);		
		result.setRect(result.getX()-INDENT,result.getY()-INDENT,result.getWidth()+2*INDENT,result.getHeight()+2*INDENT);
		localToParent(result);
		return result;		
	}
						
	/**
	 * This is a crucial step.  We have to override this method to invalidate the paint each time the bounds are changed so
	 * we repaint the correct region
	 */
	public boolean validateFullBounds() {
		comparisonBounds = getUnionOfChildrenBounds(comparisonBounds);
	
		if (!cachedChildBounds.equals(comparisonBounds)) {
			setPaintInvalid(true);
		}
		return super.validateFullBounds();	
	}
	
	public void addToNetwork(NetworkPanel np) {
		return;
	}
	
	/**
	 * Initialize a new network
	 */
	public void initSubnet(String layout) {

		int numRows = (int)Math.sqrt(subnet.getNeuronCount());
		int increment = 45;
		
		if(layout.equalsIgnoreCase("Line")) {
						
			for (int i = 0; i < subnet.getNeuronCount(); i++) {
				double x = parentPanel.getLastClicked().getX();
				double y = parentPanel.getLastClicked().getY();
				PNodeNeuron theNode = new PNodeNeuron(x + i * increment, y, subnet.getNeuron(i), parentPanel);
				parentPanel.addNode(theNode, false);
				addChild(theNode);
			}
			
		} else if (layout.equalsIgnoreCase("Grid")) {
			for (int i = 0; i < subnet.getNeuronCount(); i++) {
				double x = parentPanel.getLastClicked().getX() + (i % numRows) * increment;
				double y = parentPanel.getLastClicked().getY() + (i / numRows) * increment;
				PNodeNeuron theNode = new PNodeNeuron(x , y, subnet.getNeuron(i), parentPanel);
				parentPanel.addNode(theNode, false);
				addChild(theNode);
			}			
		} else if (layout.equalsIgnoreCase("Layers")) {
			if (! (subnet instanceof ComplexNetwork)) {
				return;
			}
			ComplexNetwork cn = (ComplexNetwork)subnet;
			double x = parentPanel.getLastClicked().getX();
			double y = parentPanel.getLastClicked().getY() + cn.getNetworkList().size() * increment;
			
			for (int i = 0; i < cn.getNetworkList().size(); i++) {
				for(int j = 0; j < cn.getNetwork(i).getNeuronCount(); j++) {
					int bpnetinc = (cn.getNetwork(0).getNeuronCount()-cn.getNetwork(i).getNeuronCount())*increment/2;
					PNodeNeuron theNode = new PNodeNeuron(x + bpnetinc + j * increment, y - i * increment, cn.getNetwork(i).getNeuron(j), parentPanel);
					parentPanel.addNode(theNode, false);
					addChild(theNode);			
				}
			}
		}
		
		for (int i = 0; i < subnet.getWeightCount(); i++) {
			Synapse s = subnet.getWeight(i);
			PNodeWeight theNode = new PNodeWeight(parentPanel.findPNodeNeuron(s.getSource()), parentPanel.findPNodeNeuron(s.getTarget()), s);
			
			parentPanel.addNode(theNode, false);
			addChild(theNode);	
		}
				
	}

	
	public void drawBoundary() {
		return;
	}
	
	public boolean isSelectable() {
		return true;
	}
	
	/**
	 * @param np Reference to parent NetworkPanel
	 */
	public void initCastor(NetworkPanel np)
	{
		
		return;
	}
	
	public void delete() {
		return;
	}
	
	public void randomize()
	{
		return;
	}
	
	public void increment()
	{
		return;
	}
	
	public void decrement()
	{
		return;
	}
	
	public void nudge(int offsetX, int offsetY, double nudgeAmount)
	{
		offset(offsetX * nudgeAmount, offsetY * nudgeAmount);
	}
	
	public void renderNode()
	{
		return;
	}
	
	public void resetLineColors()
	{
		return;
	}	
}


