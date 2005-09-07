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
import java.awt.Shape;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkPreferences;
import org.simbrain.network.ScreenElement;

import edu.umd.cs.piccolo.nodes.PPath;

/**
* <b>PNodeLine</b> is a Piccolo PNode representing the line connecting two
*  PNodeNeurons.  
* 
* TODO: make this part of PNodeNeuron
* 
* @author Mai Ngoc Thang
*/
public class PNodeLine extends PPath implements ScreenElement {
    
    public PNodeLine() {
    }

	public PNodeLine(Shape shape){
		super(shape);

	}
	
	public void delete() {
		return;
	}
	
	public void addToNetwork(NetworkPanel np)
	{
		return;
	}
	
	public void drawBoundary()
	{
		return;
	}
	
	public boolean isSelectable()
	{
		return true;
	}
	
	public void initCastor(NetworkPanel np)
	{
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
